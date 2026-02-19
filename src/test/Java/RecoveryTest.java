

import lib.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/**
 * Testes de recovery e persistência.
 *
 * Simula diversos cenários de crash e verifica recuperação.
 *
 * @author SQL Parser Team
 * @version 3.0
 */
public class RecoveryTest {

    private static final String DATA_DIR = "data_test/";

    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════════════╗");
        System.out.println("║         TESTES DE RECOVERY E PERSISTÊNCIA      ║");
        System.out.println("╚════════════════════════════════════════════════╝");
        System.out.println();

        try {
            // Executar testes
            testBasicPersistence();
            testRecoveryAfterInserts();
            testRecoveryAfterMixedOperations();
            testCheckpointBehavior();
            testCrashDuringOperations();
            testMultipleRecoveries();

            System.out.println("\n╔════════════════════════════════════════════════╗");
            System.out.println("║         TODOS OS TESTES PASSARAM! ✓            ║");
            System.out.println("╚════════════════════════════════════════════════╝");

        } catch (Exception e) {
            System.err.println("\n✗ TESTE FALHOU: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } finally {
            // Limpar após todos os testes
            cleanupTestData();
        }
    }

    /**
     * Teste 1: Persistência básica
     * Insere dados, fecha, reabre e verifica.
     */
    private static void testBasicPersistence() throws Exception {
        System.out.println("╔════════════════════════════════════════════════╗");
        System.out.println("║   TESTE 1: Persistência Básica                 ║");
        System.out.println("╚════════════════════════════════════════════════╝");

        // LIMPAR dados antes do teste
        cleanupTestData();

        // Fase 1: Criar e inserir dados
        {
            UserQuery query = new UserQuery(true, DATA_DIR);

            int initialSize = query.size();
            System.out.println("Tamanho inicial: " + initialSize);

            // Inserir 5 usuários
            for (int i = 1; i <= 5; i++) {
                int id = query.insert("TestUser" + i, 20 + i, "TestCity" + i);
                System.out.println("Inserido: id=" + id);
            }

            int finalSize = query.size();
            System.out.println("Tamanho após inserções: " + finalSize);

            // Shutdown gracioso
            query.shutdown();
            System.out.println("✓ Shutdown executado\n");
        }

        // Fase 2: Reabrir e verificar
        {
            System.out.println("Reabrindo banco de dados...");
            UserQuery query = new UserQuery(true, DATA_DIR);

            int recoveredSize = query.size();
            System.out.println("Tamanho recuperado: " + recoveredSize);

            // Verificar se usuários foram persistidos
            List<Users> allUsers = query.getAllUsers();
            long testUsers = allUsers.stream()
                    .filter(u -> u.getName().startsWith("TestUser"))
                    .count();

            System.out.println("Usuários de teste encontrados: " + testUsers);

            if (testUsers != 5) {
                throw new AssertionError("Esperado 5 usuários, encontrado: " + testUsers);
            }

            query.shutdown();
            System.out.println("✓ TESTE 1 PASSOU!\n");
        }
    }

    /**
     * Teste 2: Recovery após INSERTs sem checkpoint
     * Simula crash sem checkpoint e verifica recovery via WAL.
     */
    private static void testRecoveryAfterInserts() throws Exception {
        System.out.println("╔════════════════════════════════════════════════╗");
        System.out.println("║   TESTE 2: Recovery Após INSERTs (WAL)         ║");
        System.out.println("╚════════════════════════════════════════════════╝");

        cleanupTestData();

        // Fase 1: Inserir sem checkpoint
        {
            UserQuery query = new UserQuery(true, DATA_DIR);

            // Inserir alguns registros
            query.insert("RecoveryTest1", 25, "SP");
            query.insert("RecoveryTest2", 30, "RJ");
            query.insert("RecoveryTest3", 35, "MG");

            System.out.println("Inseridos 3 registros SEM checkpoint");
            System.out.println("Tamanho: " + query.size());

            // SIMULAÇÃO DE CRASH - não chama shutdown()
            // Os dados estão apenas no WAL, não no snapshot
            System.out.println("⚠ SIMULANDO CRASH (sem shutdown)\n");
        }

        // Fase 2: Recovery
        {
            System.out.println("Iniciando recovery...");
            UserQuery query = new UserQuery(true, DATA_DIR);

            // Verificar se dados foram recuperados do WAL
            List<Users> allUsers = query.getAllUsers();
            long recoveredCount = allUsers.stream()
                    .filter(u -> u.getName().startsWith("RecoveryTest"))
                    .count();

            System.out.println("Registros recuperados: " + recoveredCount);

            if (recoveredCount != 3) {
                throw new AssertionError("Esperado 3 registros, recuperado: " + recoveredCount);
            }

            query.shutdown();
            System.out.println("✓ TESTE 2 PASSOU!\n");
        }
    }

    /**
     * Teste 3: Recovery após operações mistas
     * INSERT, UPDATE, DELETE e verifica consistência.
     */
    private static void testRecoveryAfterMixedOperations() throws Exception {
        System.out.println("╔════════════════════════════════════════════════╗");
        System.out.println("║   TESTE 3: Recovery com Operações Mistas       ║");
        System.out.println("╚════════════════════════════════════════════════╝");

        cleanupTestData();

        int insertedId;

        // Fase 1: Operações mistas
        {
            UserQuery query = new UserQuery(true, DATA_DIR);

            // INSERT
            insertedId = query.insert("MixedTest", 40, "BA");
            System.out.println("INSERT: id=" + insertedId);

            // UPDATE
            Map<String, Object> updates = new HashMap<>();
            updates.put("age", 41);
            updates.put("city", "CE");
            int updated = query.update(updates, query.equals("id", insertedId));
            System.out.println("UPDATE: " + updated + " registro(s)");

            // INSERT mais um
            int id2 = query.insert("ToDelete", 50, "PE");
            System.out.println("INSERT: id=" + id2);

            // DELETE
            int deleted = query.delete(query.equals("id", id2));
            System.out.println("DELETE: " + deleted + " registro(s)");

            // SIMULAÇÃO DE CRASH
            System.out.println("⚠ SIMULANDO CRASH\n");
        }

        // Fase 2: Verificar recovery
        {
            System.out.println("Verificando recovery...");
            UserQuery query = new UserQuery(true, DATA_DIR);

            // Verificar se MixedTest existe e foi atualizado
            List<Users> result = query.from(query.equals("id", insertedId));

            if (result.isEmpty()) {
                throw new AssertionError("Registro MixedTest não encontrado!");
            }

            Users user = result.get(0);
            System.out.println("Encontrado: " + user.getName() +
                    ", age=" + user.getAge() + ", city=" + user.getCity());

            if (user.getAge() != 41 || !user.getCity().equals("CE")) {
                throw new AssertionError("UPDATE não foi aplicado corretamente");
            }

            // Verificar se ToDelete foi deletado
            List<Users> deleted = query.from(u -> u.getName().equals("ToDelete"));
            if (!deleted.isEmpty()) {
                throw new AssertionError("DELETE não foi aplicado!");
            }

            query.shutdown();
            System.out.println("✓ TESTE 3 PASSOU!\n");
        }
    }

    /**
     * Teste 4: Comportamento de checkpoint
     * Verifica que checkpoint salva dados corretamente.
     */
    private static void testCheckpointBehavior() throws Exception {
        System.out.println("╔════════════════════════════════════════════════╗");
        System.out.println("║   TESTE 4: Comportamento de Checkpoint         ║");
        System.out.println("╚════════════════════════════════════════════════╝");

        cleanupTestData();

        // Fase 1: Inserir e fazer checkpoint
        {
            UserQuery query = new UserQuery(true, DATA_DIR);

            // Inserir registros
            for (int i = 1; i <= 3; i++) {
                query.insert("CheckpointTest" + i, 30, "DF");
            }

            System.out.println("Registros inseridos: 3");

            // Forçar checkpoint
            boolean checkpointed = query.forceCheckpoint();
            System.out.println("Checkpoint forçado: " + checkpointed);

            // Inserir mais registros APÓS checkpoint
            query.insert("AfterCheckpoint", 35, "GO");

            query.shutdown();
            System.out.println("✓ Shutdown executado\n");
        }

        // Fase 2: Verificar que tudo foi salvo
        {
            System.out.println("Verificando dados após checkpoint...");
            UserQuery query = new UserQuery(true, DATA_DIR);

            long beforeCheckpoint = query.getAllUsers().stream()
                    .filter(u -> u.getName().startsWith("CheckpointTest"))
                    .count();

            long afterCheckpoint = query.getAllUsers().stream()
                    .filter(u -> u.getName().equals("AfterCheckpoint"))
                    .count();

            System.out.println("Antes do checkpoint: " + beforeCheckpoint);
            System.out.println("Depois do checkpoint: " + afterCheckpoint);

            if (beforeCheckpoint != 3 || afterCheckpoint != 1) {
                throw new AssertionError("Checkpoint não funcionou corretamente");
            }

            query.shutdown();
            System.out.println("✓ TESTE 4 PASSOU!\n");
        }
    }

    /**
     * Teste 5: Crash durante operações
     * Simula crash no meio de várias operações.
     */
    private static void testCrashDuringOperations() throws Exception {
        System.out.println("╔════════════════════════════════════════════════╗");
        System.out.println("║   TESTE 5: Crash Durante Operações             ║");
        System.out.println("╚════════════════════════════════════════════════╝");

        cleanupTestData();

        // Fase 1: Executar operações e crashar
        {
            UserQuery query = new UserQuery(true, DATA_DIR);

            // Inserir 10 registros
            for (int i = 1; i <= 10; i++) {
                query.insert("Batch" + i, 20 + i, "City" + i);
                System.out.println("Inserido: Batch" + i);

                // Simular crash no meio (após 7 inserções)
                if (i == 7) {
                    System.out.println("⚠ SIMULANDO CRASH após 7 inserções\n");
                    break;
                }
            }
        }

        // Fase 2: Recovery
        {
            System.out.println("Recovery após crash...");
            UserQuery query = new UserQuery(true, DATA_DIR);

            long recovered = query.getAllUsers().stream()
                    .filter(u -> u.getName().startsWith("Batch"))
                    .count();

            System.out.println("Registros recuperados: " + recovered);

            if (recovered != 7) {
                throw new AssertionError("Esperado 7 registros, encontrado: " + recovered);
            }

            query.shutdown();
            System.out.println("✓ TESTE 5 PASSOU!\n");
        }
    }

    /**
     * Teste 6: Múltiplos recoveries
     * Várias sessões com crashes.
     */
    private static void testMultipleRecoveries() throws Exception {
        System.out.println("╔════════════════════════════════════════════════╗");
        System.out.println("║   TESTE 6: Múltiplos Recoveries                ║");
        System.out.println("╚════════════════════════════════════════════════╝");

        cleanupTestData();

        // Sessão 1
        {
            UserQuery query = new UserQuery(true, DATA_DIR);
            query.insert("Session1", 25, "RS");
            System.out.println("Sessão 1: 1 inserção");
            // Crash
        }

        // Sessão 2
        {
            UserQuery query = new UserQuery(true, DATA_DIR);
            query.insert("Session2", 30, "SC");
            System.out.println("Sessão 2: recovery + 1 inserção");
            // Crash
        }

        // Sessão 3
        {
            UserQuery query = new UserQuery(true, DATA_DIR);
            query.insert("Session3", 35, "PR");
            System.out.println("Sessão 3: recovery + 1 inserção");
            query.shutdown();  // Shutdown gracioso
        }

        // Verificação final
        {
            UserQuery query = new UserQuery(true, DATA_DIR);

            long total = query.getAllUsers().stream()
                    .filter(u -> u.getName().startsWith("Session"))
                    .count();

            System.out.println("Total de registros recuperados: " + total);

            if (total != 3) {
                throw new AssertionError("Esperado 3 sessões, encontrado: " + total);
            }

            query.shutdown();
            System.out.println("✓ TESTE 6 PASSOU!\n");
        }
    }

    /**
     * Limpa dados de teste.
     */
    private static void cleanupTestData() {
        try {
            Path dataDir = Paths.get(DATA_DIR);
            if (Files.exists(dataDir)) {
                Files.walk(dataDir)
                        .sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                // Ignorar
                            }
                        });
            }
            System.out.println("✓ Dados de teste limpos\n");
        } catch (IOException e) {
            System.err.println("Aviso: Erro ao limpar dados de teste: " + e.getMessage());
        }
    }
}