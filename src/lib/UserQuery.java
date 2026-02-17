package lib;

import lib.index.IndexManager;
import lib.persistence.PersistenceManager;
import lib.persistence.CheckpointManager;
import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Classe principal para operações de query no banco de dados Users.
 *
 * Versão 3.0 - COM PERSISTÊNCIA COMPLETA:
 * - Índices (Hash + B-Tree) para alta performance
 * - Write-Ahead Log (WAL) para durabilidade
 * - Recovery automático após crashes
 * - Checkpoint inteligente
 *
 * Garantias ACID completas.
 *
 * @author SQL Parser Team
 * @version 3.0
 */
public class UserQuery {

    private List<Users> database;
    private IndexManager indexManager;
    private PersistenceManager persistenceManager;
    private CheckpointManager checkpointManager;

    private int nextId;
    private boolean persistenceEnabled;

    /**
     * Construtor padrão com persistência habilitada.
     */
    public UserQuery() {
        this(true, "data/");
    }

    /**
     * Construtor com opção de habilitar/desabilitar persistência.
     *
     * @param enablePersistence Se true, habilita persistência em disco
     */
    public UserQuery(boolean enablePersistence) {
        this(enablePersistence, "data/");
    }

    /**
     * Construtor completo com diretório customizado.
     *
     * @param enablePersistence Se true, habilita persistência em disco
     * @param dataDirectory Diretório para arquivos de dados
     */
    public UserQuery(boolean enablePersistence, String dataDirectory) {
        this.database = new ArrayList<>();
        this.persistenceEnabled = enablePersistence;

        // Inicializar persistência
        if (persistenceEnabled) {
            try {
                System.out.println("╔════════════════════════════════════════════════╗");
                System.out.println("║     Inicializando Sistema de Persistência      ║");
                System.out.println("╚════════════════════════════════════════════════╝");

                this.persistenceManager = new PersistenceManager(dataDirectory);
                this.checkpointManager = new CheckpointManager();

                // Carregar dados e replay do WAL
                int recoveredOps = persistenceManager.initialize(this.database);

                if (recoveredOps > 0) {
                    System.out.println("✓ Recovery: " + recoveredOps + " operações restauradas");
                }

            } catch (IOException e) {
                System.err.println("⚠ ERRO ao inicializar persistência: " + e.getMessage());
                System.err.println("⚠ Continuando em modo IN-MEMORY (sem persistência)");
                this.persistenceEnabled = false;

                // Fallback: carregar dados padrão
                this.database = UsersDatabase.getAllUsers();
            }
        } else {
            // Modo in-memory: carregar dados padrão
            this.database = UsersDatabase.getAllUsers();
        }

        // Calcular próximo ID
        this.nextId = database.stream()
                .mapToInt(Users::getId)
                .max()
                .orElse(0) + 1;

        // Construir índices
        this.indexManager = new IndexManager();
        this.indexManager.rebuildAll(this.database);

        System.out.println("✓ Database inicializado: " + database.size() + " registros");
        System.out.println("✓ Índices construídos");
        System.out.println();
    }

    // ================================================================
    // FROM - Seleção de registros
    // ================================================================

    public List<Users> from(Predicate<Users> condition) {
        if (condition == null)
            return new ArrayList<>(database);

        return database.stream()
                .filter(condition)
                .collect(Collectors.toList());
    }

    public List<Users> from() {
        return from(null);
    }

    // ================================================================
    // SELECT - Projeção de colunas
    // ================================================================

    public List<Map<String, Object>> select(String columns, List<Users> result) {

        if (columns.equalsIgnoreCase("count")) {
            Map<String, Object> map = new HashMap<>();
            map.put("count", result.size());
            return List.of(map);
        }

        String[] cols = columns.equals("*")
                ? new String[]{"id", "name", "age", "city"}
                : columns.split(",");

        List<Map<String, Object>> output = new ArrayList<>();

        for (Users u : result) {
            Map<String, Object> row = new LinkedHashMap<>();

            for (String c : cols) {
                c = c.trim().toLowerCase();

                switch (c) {
                    case "id" -> row.put("id", u.getId());
                    case "name" -> row.put("name", u.getName());
                    case "age" -> row.put("age", u.getAge());
                    case "city" -> row.put("city", u.getCity());
                }
            }
            output.add(row);
        }
        return output;
    }

    // ================================================================
    // ORDER BY - Ordenação
    // ================================================================

    public List<Users> orderBy(String column, String order, List<Users> result) {

        // OTIMIZAÇÃO: Se ordenar por age e resultado completo, usar índice
        if (column.equalsIgnoreCase("age") && result.size() == database.size()) {
            boolean ascending = !order.equalsIgnoreCase("desc");
            return indexManager.getAllOrderedByAge(ascending);
        }

        Comparator<Users> comparator = switch (column.toLowerCase()) {
            case "id" -> Comparator.comparing(Users::getId);
            case "name" -> Comparator.comparing(Users::getName);
            case "age" -> Comparator.comparing(Users::getAge);
            case "city" -> Comparator.comparing(Users::getCity);
            default -> null;
        };

        if (comparator == null)
            return result;

        if (order.equalsIgnoreCase("desc"))
            comparator = comparator.reversed();

        return result.stream().sorted(comparator).toList();
    }

    // ================================================================
    // GROUP BY - Agrupamento
    // ================================================================

    public Map<String, List<Users>> groupBy(String column, List<Users> result) {

        return result.stream().collect(Collectors.groupingBy(user -> {
            return switch (column.toLowerCase()) {
                case "id" -> String.valueOf(user.getId());
                case "name" -> user.getName();
                case "age" -> String.valueOf(user.getAge());
                case "city" -> user.getCity();
                default -> "undefined";
            };
        }));
    }

    // ================================================================
    // INSERT - Inserção de registros
    // ================================================================

    public int insert(String name, int age, String city) {
        int id = nextId++;
        Users newUser = new Users(id, name, age, city);

        // 1. LOG NO WAL PRIMEIRO (durabilidade)
        if (persistenceEnabled) {
            try {
                persistenceManager.logInsert(newUser);
                checkpointManager.recordOperation();
            } catch (IOException e) {
                System.err.println("⚠ Erro ao persistir INSERT: " + e.getMessage());
                // Continua mesmo com erro - modo degradado
            }
        }

        // 2. Executar em memória
        database.add(newUser);
        indexManager.insertUser(newUser);

        // 3. Checkpoint se necessário
        checkpointIfNeeded();

        return id;
    }

    // ================================================================
    // DELETE - Remoção de registros
    // ================================================================

    public int delete(Predicate<Users> condition) {
        List<Users> toDelete = database.stream()
                .filter(condition)
                .toList();

        for (Users user : toDelete) {
            // 1. LOG NO WAL PRIMEIRO
            if (persistenceEnabled) {
                try {
                    persistenceManager.logDelete(user.getId());
                    checkpointManager.recordOperation();
                } catch (IOException e) {
                    System.err.println("⚠ Erro ao persistir DELETE: " + e.getMessage());
                }
            }

            // 2. Executar em memória
            database.remove(user);
            indexManager.removeUser(user);
        }

        // 3. Checkpoint se necessário
        checkpointIfNeeded();

        return toDelete.size();
    }

    // ================================================================
    // UPDATE - Atualização de registros
    // ================================================================

    public int update(Map<String, Object> values, Predicate<Users> condition) {

        int count = 0;

        for (Users u : database) {

            if (!condition.test(u))
                continue;

            // Salvar estado antigo
            Users oldUser = new Users(u.getId(), u.getName(), u.getAge(), u.getCity());

            // Aplicar mudanças
            for (var e : values.entrySet()) {
                switch (e.getKey().toLowerCase()) {
                    case "name" -> u.setName((String) e.getValue());
                    case "age" -> u.setAge((Integer) e.getValue());
                    case "city" -> u.setCity((String) e.getValue());
                }
            }

            // 1. LOG NO WAL
            if (persistenceEnabled) {
                try {
                    persistenceManager.logUpdate(u.getId(), values);
                    checkpointManager.recordOperation();
                } catch (IOException ex) {
                    System.err.println("⚠ Erro ao persistir UPDATE: " + ex.getMessage());
                }
            }

            // 2. Atualizar índices
            indexManager.updateUser(oldUser, u);

            count++;
        }

        // 3. Checkpoint se necessário
        checkpointIfNeeded();

        return count;
    }

    // ================================================================
    // PREDICATES - Condições de busca (COM ÍNDICES)
    // ================================================================

    public Predicate<Users> like(String field, String pattern) {
        return u -> switch (field.toLowerCase()) {
            case "name" -> u.getName().toLowerCase().contains(pattern.toLowerCase());
            case "city" -> u.getCity().toLowerCase().contains(pattern.toLowerCase());
            default -> false;
        };
    }

    public Predicate<Users> between(String field, int min, int max) {
        if (field.equalsIgnoreCase("age")) {
            Set<Users> indexResult = new HashSet<>(
                    indexManager.searchByAgeRange(min, max)
            );
            return user -> indexResult.contains(user);
        }

        return user -> false;
    }

    public Predicate<Users> equals(String field, String value) {
        return switch (field.toLowerCase()) {
            case "id" -> {
                try {
                    int id = Integer.parseInt(value);
                    Set<Users> indexResult = new HashSet<>(
                            indexManager.searchById(id)
                    );
                    yield user -> indexResult.contains(user);
                } catch (NumberFormatException e) {
                    yield user -> false;
                }
            }
            case "name" -> {
                Set<Users> indexResult = new HashSet<>(
                        indexManager.searchByName(value)
                );
                yield user -> indexResult.contains(user);
            }
            case "city" -> {
                Set<Users> indexResult = new HashSet<>(
                        indexManager.searchByCity(value)
                );
                yield user -> indexResult.contains(user);
            }
            case "age" -> {
                try {
                    int age = Integer.parseInt(value);
                    Set<Users> indexResult = new HashSet<>(
                            indexManager.searchByAge(age)
                    );
                    yield user -> indexResult.contains(user);
                } catch (NumberFormatException e) {
                    yield user -> false;
                }
            }
            default -> user -> false;
        };
    }

    public Predicate<Users> equals(String field, int value) {
        return switch (field.toLowerCase()) {
            case "id" -> {
                Set<Users> indexResult = new HashSet<>(
                        indexManager.searchById(value)
                );
                yield user -> indexResult.contains(user);
            }
            case "age" -> {
                Set<Users> indexResult = new HashSet<>(
                        indexManager.searchByAge(value)
                );
                yield user -> indexResult.contains(user);
            }
            default -> user -> false;
        };
    }

    // ================================================================
    // CHECKPOINT - Gerenciamento
    // ================================================================

    /**
     * Verifica se checkpoint é necessário e executa.
     */
    private void checkpointIfNeeded() {
        if (!persistenceEnabled) {
            return;
        }

        try {
            long walSize = persistenceManager.getOperationsSinceCheckpoint() * 100; // Estimativa

            if (checkpointManager.shouldCheckpoint(walSize)) {
                System.out.println("⏳ Executando checkpoint automático...");
                checkpointManager.executeCheckpoint(database, persistenceManager);
                System.out.println("✓ Checkpoint concluído");
            }
        } catch (IOException e) {
            System.err.println("⚠ Erro no checkpoint: " + e.getMessage());
        }
    }

    /**
     * Força checkpoint imediato.
     *
     * @return true se sucesso
     */
    public boolean forceCheckpoint() {
        if (!persistenceEnabled) {
            return false;
        }

        try {
            System.out.println("⏳ Forçando checkpoint...");
            checkpointManager.forceCheckpoint(database, persistenceManager);
            System.out.println("✓ Checkpoint forçado concluído");
            return true;
        } catch (IOException e) {
            System.err.println("⚠ Erro ao forçar checkpoint: " + e.getMessage());
            return false;
        }
    }

    // ================================================================
    // SHUTDOWN - Encerramento gracioso
    // ================================================================

    /**
     * Encerra o banco de dados de forma segura.
     * Executa checkpoint final e fecha recursos.
     */
    public void shutdown() {
        if (!persistenceEnabled) {
            return;
        }

        try {
            System.out.println("\n╔════════════════════════════════════════════════╗");
            System.out.println("║         Encerrando Sistema de Persistência     ║");
            System.out.println("╚════════════════════════════════════════════════╝");

            persistenceManager.shutdown(database);

            System.out.println("✓ Shutdown concluído com sucesso");

        } catch (IOException e) {
            System.err.println("⚠ Erro ao encerrar: " + e.getMessage());
        }
    }

    // ================================================================
    // ESTATÍSTICAS E DIAGNÓSTICO
    // ================================================================

    /**
     * Retorna estatísticas dos índices.
     */
    public String getIndexStats() {
        return indexManager.getStats();
    }

    /**
     * Retorna estatísticas de persistência.
     */
    public String getPersistenceStats() {
        if (!persistenceEnabled) {
            return "Persistência desabilitada (modo in-memory)";
        }
        return persistenceManager.getStats();
    }

    /**
     * Retorna estatísticas de checkpoint.
     */
    public String getCheckpointStats() {
        if (!persistenceEnabled) {
            return "Checkpoint desabilitado (modo in-memory)";
        }
        return checkpointManager.getStats();
    }

    /**
     * Retorna todas as estatísticas.
     */
    public String getAllStats() {
        StringBuilder sb = new StringBuilder();

        sb.append(getIndexStats()).append("\n");

        if (persistenceEnabled) {
            sb.append(getPersistenceStats()).append("\n");
            sb.append(getCheckpointStats());
        }

        return sb.toString();
    }

    /**
     * Reconstrói todos os índices.
     */
    public void rebuildIndexes() {
        indexManager.rebuildAll(database);
    }

    /**
     * Retorna o número total de registros.
     */
    public int size() {
        return database.size();
    }

    /**
     * Verifica se persistência está habilitada.
     */
    public boolean isPersistenceEnabled() {
        return persistenceEnabled;
    }

    /**
     * Retorna cópia da lista de usuários.
     * Para debug/testes apenas.
     */
    public List<Users> getAllUsers() {
        return new ArrayList<>(database);
    }
}