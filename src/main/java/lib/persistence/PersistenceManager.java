package lib.persistence;

import lib.Users;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/**
 * Gerenciador principal de persistência.
 *
 * Coordena:
 * - DataFile (snapshot do banco)
 * - WAL (write-ahead log)
 * - Recovery automático
 * - Checkpoints periódicos
 *
 * Garante durabilidade e consistência ACID.
 *
 * @author SQL Parser Team
 * @version 2.0
 */
public class PersistenceManager implements AutoCloseable {

    // Configuração
    private final Path dataDir;
    private final String dbName;
    private final int checkpointThreshold;
    private final boolean syncOnWrite;

    // Componentes
    private final DataFile dataFile;
    private final WALWriter walWriter;
    private final WALReader walReader;

    // Caminhos
    private final Path dataPath;
    private final Path walPath;
    private final Path walBackupPath;

    // Estado
    private boolean initialized = false;
    private int operationsSinceCheckpoint = 0;

    /**
     * Construtor com configuração padrão.
     *
     * @param dataDir Diretório de dados
     * @throws IOException Se erro ao inicializar
     */
    public PersistenceManager(String dataDir) throws IOException {
        this(dataDir, "users", 100, true);
    }

    /**
     * Construtor completo.
     *
     * @param dataDir Diretório de dados
     * @param dbName Nome do banco
     * @param checkpointThreshold Checkpoint a cada N operações
     * @param syncOnWrite Se true, fsync após cada operação
     * @throws IOException Se erro ao inicializar
     */
    public PersistenceManager(String dataDir, String dbName,
                              int checkpointThreshold, boolean syncOnWrite)
            throws IOException {
        this.dataDir = Paths.get(dataDir);
        this.dbName = dbName;
        this.checkpointThreshold = checkpointThreshold;
        this.syncOnWrite = syncOnWrite;

        // Criar diretório se não existir
        Files.createDirectories(this.dataDir);

        // Inicializar caminhos
        this.dataPath = this.dataDir.resolve(dbName + ".db");
        this.walPath = this.dataDir.resolve(dbName + ".wal");
        this.walBackupPath = this.dataDir.resolve(dbName + ".wal.old");

        // Inicializar componentes
        this.dataFile = new DataFile(dataPath);
        this.walWriter = new WALWriter(walPath, syncOnWrite);
        this.walReader = new WALReader(walPath);
    }

    /**
     * Inicializa o sistema de persistência.
     * Carrega dados e faz replay do WAL.
     *
     * @param database Lista para carregar dados
     * @return Número de operações recuperadas do WAL
     * @throws IOException Se erro de I/O
     */
    public int initialize(List<Users> database) throws IOException {
        if (initialized) {
            throw new IllegalStateException("Já inicializado");
        }

        System.out.println("Inicializando sistema de persistência...");

        // 1. Carregar snapshot do banco
        if (dataFile.exists()) {
            List<Users> loadedUsers = dataFile.load();
            database.addAll(loadedUsers);
            System.out.println("Carregados " + loadedUsers.size() +
                    " usuários de " + dataPath);
        } else {
            System.out.println("Arquivo de dados não existe, " +
                    "iniciando banco vazio");
        }

        // 2. Replay do WAL
        int replayedOps = 0;
        if (Files.exists(walPath)) {
            replayedOps = walReader.replay(database);
            System.out.println("Replay de " + replayedOps +
                    " operações do WAL");
        }

        // 3. Marcar como inicializado
        initialized = true;
        operationsSinceCheckpoint = walWriter.getOperationCount();

        System.out.println("Persistência inicializada. " +
                "Total: " + database.size() + " registros");

        return replayedOps;
    }

    /**
     * Registra operação INSERT no WAL.
     *
     * @param user Usuário inserido
     * @throws IOException Se erro ao escrever
     */
    public void logInsert(Users user) throws IOException {
        checkInitialized();

        LogEntry entry = LogEntry.createInsert(user);
        walWriter.write(entry);
        operationsSinceCheckpoint++;

        checkpointIfNeeded();
    }

    /**
     * Registra operação UPDATE no WAL.
     *
     * @param id ID do usuário
     * @param changes Mapa de mudanças
     * @throws IOException Se erro ao escrever
     */
    public void logUpdate(int id, Map<String, Object> changes) throws IOException {
        checkInitialized();

        LogEntry entry = LogEntry.createUpdate(id, changes);
        walWriter.write(entry);
        operationsSinceCheckpoint++;

        checkpointIfNeeded();
    }

    /**
     * Registra operação DELETE no WAL.
     *
     * @param id ID do usuário deletado
     * @throws IOException Se erro ao escrever
     */
    public void logDelete(int id) throws IOException {
        checkInitialized();

        LogEntry entry = LogEntry.createDelete(id);
        walWriter.write(entry);
        operationsSinceCheckpoint++;

        checkpointIfNeeded();
    }

    /**
     * Executa checkpoint.
     * Salva snapshot do banco e rotaciona o WAL.
     *
     * @param database Estado atual do banco
     * @throws IOException Se erro de I/O
     */
    public void checkpoint(List<Users> database) throws IOException {
        checkInitialized();

        System.out.println("Executando checkpoint...");
        long start = System.currentTimeMillis();

        try {
            // 1. Flush do WAL antes de rotacionar
            walWriter.flush();

            // 2. Salvar snapshot do banco
            dataFile.save(database);

            // 3. Rotacionar WAL (copia e trunca)
            walWriter.rotate(walBackupPath);

            // 4. Escrever marcador de checkpoint no novo WAL
            LogEntry checkpointEntry = LogEntry.createCheckpoint(database.size());
            walWriter.write(checkpointEntry);

            // 5. Resetar contador
            operationsSinceCheckpoint = 0;

            long elapsed = System.currentTimeMillis() - start;
            System.out.println("Checkpoint concluído em " + elapsed + "ms. " +
                    "Salvos " + database.size() + " registros");

        } catch (IOException e) {
            System.err.println("ERRO durante checkpoint: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Verifica se precisa fazer checkpoint e executa se necessário.
     *
     * @throws IOException Se erro de I/O
     */
    private void checkpointIfNeeded() throws IOException {
        if (operationsSinceCheckpoint >= checkpointThreshold) {
            // Não podemos fazer checkpoint aqui pois não temos referência ao database
            // O UserQuery precisa chamar checkpoint() manualmente
            System.out.println("Checkpoint recomendado: " +
                    operationsSinceCheckpoint + " operações pendentes");
        }
    }

    /**
     * Força flush do WAL para disco.
     *
     * @throws IOException Se erro ao flush
     */
    public void flush() throws IOException {
        checkInitialized();
        walWriter.flush();
    }

    /**
     * Retorna estatísticas do sistema de persistência.
     *
     * @return String formatada com estatísticas
     */
    public String getStats() {
        StringBuilder sb = new StringBuilder();
        sb.append("╔════════════════════════════════════════════════╗\n");
        sb.append("║         ESTATÍSTICAS DE PERSISTÊNCIA           ║\n");
        sb.append("╚════════════════════════════════════════════════╝\n\n");

        // Data File
        sb.append("Arquivo de Dados:\n");
        sb.append("  - Caminho: ").append(dataPath).append("\n");
        sb.append("  - Existe: ").append(dataFile.exists() ? "Sim" : "Não").append("\n");
        if (dataFile.exists()) {
            sb.append("  - Tamanho: ").append(dataFile.getSize()).append(" bytes\n");
            sb.append("  - Última modificação: ").append(dataFile.getLastModified()).append("\n");
        }
        sb.append("\n");

        // WAL
        sb.append("Write-Ahead Log:\n");
        sb.append("  - Caminho: ").append(walPath).append("\n");
        try {
            sb.append("  - Tamanho: ").append(walWriter.getSize()).append(" bytes\n");
        } catch (IOException e) {
            sb.append("  - Tamanho: N/A\n");
        }
        sb.append("  - Operações desde checkpoint: ").append(operationsSinceCheckpoint).append("\n");
        sb.append("  - Threshold: ").append(checkpointThreshold).append("\n");

        Map<String, Integer> walStats = walReader.getStats();
        sb.append("  - Total de entradas: ").append(walStats.get("total")).append("\n");
        sb.append("    - INSERT: ").append(walStats.get("insert")).append("\n");
        sb.append("    - UPDATE: ").append(walStats.get("update")).append("\n");
        sb.append("    - DELETE: ").append(walStats.get("delete")).append("\n");
        sb.append("    - CHECKPOINT: ").append(walStats.get("checkpoint")).append("\n");
        sb.append("\n");

        // Configuração
        sb.append("Configuração:\n");
        sb.append("  - Diretório: ").append(dataDir).append("\n");
        sb.append("  - Banco: ").append(dbName).append("\n");
        sb.append("  - Sync on write: ").append(syncOnWrite ? "Sim" : "Não").append("\n");

        return sb.toString();
    }

    /**
     * Verifica integridade dos arquivos.
     *
     * @return true se tudo OK
     */
    public boolean validate() {
        boolean dataValid = dataFile.validate();
        boolean walValid = walReader.validate();

        if (!dataValid) {
            System.err.println("ERRO: Arquivo de dados corrompido!");
        }
        if (!walValid) {
            System.err.println("ERRO: WAL corrompido!");
        }

        return dataValid && walValid;
    }

    /**
     * Retorna se precisa checkpoint.
     *
     * @return true se recomendado
     */
    public boolean needsCheckpoint() {
        return operationsSinceCheckpoint >= checkpointThreshold;
    }

    /**
     * Retorna número de operações desde último checkpoint.
     *
     * @return Contagem
     */
    public int getOperationsSinceCheckpoint() {
        return operationsSinceCheckpoint;
    }

    /**
     * Fecha o gerenciador e executa checkpoint final.
     *
     * @param database Estado atual para checkpoint
     * @throws IOException Se erro de I/O
     */
    public void shutdown(List<Users> database) throws IOException {
        if (!initialized) {
            return;
        }

        System.out.println("Encerrando sistema de persistência...");

        // Checkpoint final
        if (operationsSinceCheckpoint > 0) {
            checkpoint(database);
        }

        // Fechar WAL
        walWriter.close();

        initialized = false;
        System.out.println("Persistência encerrada");
    }

    @Override
    public void close() throws IOException {
        if (initialized) {
            walWriter.close();
        }
    }

    private void checkInitialized() {
        if (!initialized) {
            throw new IllegalStateException(
                    "PersistenceManager não inicializado. Chame initialize() primeiro.");
        }
    }
}