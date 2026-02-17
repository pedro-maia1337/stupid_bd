package lib.persistence;

import lib.Users;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Lê e executa operações do Write-Ahead Log.
 * 
 * Usado para:
 * - Recovery após crash
 * - Replay de operações pendentes
 * 
 * @author SQL Parser Team
 * @version 2.0
 */
public class WALReader {
    
    private final Path walPath;
    
    /**
     * Construtor.
     * 
     * @param walPath Caminho do arquivo WAL
     */
    public WALReader(Path walPath) {
        this.walPath = walPath;
    }
    
    /**
     * Lê todas as entradas do WAL.
     * 
     * @return Lista de entradas (na ordem)
     * @throws IOException Se erro de leitura
     */
    public List<LogEntry> readAll() throws IOException {
        List<LogEntry> entries = new ArrayList<>();
        
        if (!Files.exists(walPath)) {
            return entries;
        }
        
        try (BufferedReader reader = Files.newBufferedReader(walPath)) {
            String line;
            int lineNumber = 0;
            
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                
                LogEntry entry = LogEntry.deserialize(line);
                
                if (entry == null) {
                    System.err.println("WAL: Linha " + lineNumber + 
                        " inválida, ignorando: " + line);
                    continue;
                }
                
                entries.add(entry);
            }
        }
        
        return entries;
    }
    
    /**
     * Executa replay do WAL em uma lista de usuários.
     * Reconstrói o estado do banco a partir do log.
     * 
     * @param database Lista para aplicar as operações
     * @return Número de operações aplicadas
     * @throws IOException Se erro de leitura
     */
    public int replay(List<Users> database) throws IOException {
        List<LogEntry> entries = readAll();
        int appliedCount = 0;
        
        for (LogEntry entry : entries) {
            try {
                applyEntry(entry, database);
                appliedCount++;
            } catch (Exception e) {
                System.err.println("Erro ao aplicar entrada: " + entry);
                System.err.println("Erro: " + e.getMessage());
                // Continuar com próxima entrada
            }
        }
        
        return appliedCount;
    }
    
    /**
     * Aplica uma entrada do log no database.
     * 
     * @param entry Entrada a aplicar
     * @param database Lista de usuários
     */
    private void applyEntry(LogEntry entry, List<Users> database) {
        Map<String, Object> data = entry.getData();
        
        switch (entry.getOperation()) {
            case INSERT -> {
                int id = (Integer) data.get("id");
                String name = (String) data.get("name");
                int age = (Integer) data.get("age");
                String city = (String) data.get("city");
                
                Users user = new Users(id, name, age, city);
                database.add(user);
            }
            
            case UPDATE -> {
                int id = (Integer) data.get("id");
                Map<String, Object> changes = 
                    (Map<String, Object>) data.get("changes");
                
                // Encontrar usuário
                Users user = database.stream()
                    .filter(u -> u.getId() == id)
                    .findFirst()
                    .orElse(null);
                
                if (user != null) {
                    // Aplicar mudanças
                    for (var change : changes.entrySet()) {
                        switch (change.getKey().toLowerCase()) {
                            case "name" -> user.setName((String) change.getValue());
                            case "age" -> user.setAge((Integer) change.getValue());
                            case "city" -> user.setCity((String) change.getValue());
                        }
                    }
                }
            }
            
            case DELETE -> {
                int id = (Integer) data.get("id");
                database.removeIf(u -> u.getId() == id);
            }
            
            case CHECKPOINT -> {
                // Checkpoint não faz nada no replay
                // Serve apenas como marcador
            }
        }
    }
    
    /**
     * Valida integridade do WAL.
     * Verifica se todas as linhas são válidas.
     * 
     * @return true se WAL é válido
     */
    public boolean validate() {
        if (!Files.exists(walPath)) {
            return true;  // WAL vazio é válido
        }
        
        try (BufferedReader reader = Files.newBufferedReader(walPath)) {
            String line;
            
            while ((line = reader.readLine()) != null) {
                LogEntry entry = LogEntry.deserialize(line);
                
                if (entry == null) {
                    return false;  // Linha inválida
                }
            }
            
            return true;
            
        } catch (IOException e) {
            return false;
        }
    }
    
    /**
     * Retorna estatísticas do WAL.
     * 
     * @return Mapa com estatísticas
     */
    public Map<String, Integer> getStats() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("total", 0);
        stats.put("insert", 0);
        stats.put("update", 0);
        stats.put("delete", 0);
        stats.put("checkpoint", 0);
        stats.put("invalid", 0);
        
        if (!Files.exists(walPath)) {
            return stats;
        }
        
        try {
            List<LogEntry> entries = readAll();
            stats.put("total", entries.size());
            
            for (LogEntry entry : entries) {
                String op = entry.getOperation().name().toLowerCase();
                stats.put(op, stats.get(op) + 1);
            }
            
        } catch (IOException e) {
            // Ignorar erro
        }
        
        return stats;
    }
    
    /**
     * Retorna a última entrada do tipo CHECKPOINT.
     * 
     * @return Última entrada de checkpoint ou null
     */
    public LogEntry getLastCheckpoint() {
        try {
            List<LogEntry> entries = readAll();
            
            // Procurar de trás para frente
            for (int i = entries.size() - 1; i >= 0; i--) {
                LogEntry entry = entries.get(i);
                if (entry.getOperation() == LogEntry.Operation.CHECKPOINT) {
                    return entry;
                }
            }
            
        } catch (IOException e) {
            // Ignorar erro
        }
        
        return null;
    }
}
