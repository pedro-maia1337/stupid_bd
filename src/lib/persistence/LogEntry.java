package lib.persistence;

import lib.Users;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Representa uma operação registrada no Write-Ahead Log.
 * 
 * Cada LogEntry corresponde a uma linha no arquivo WAL.
 * Formato: OPERACAO|parametro1|parametro2|...
 * 
 * @author SQL Parser Team
 * @version 2.0
 */
public class LogEntry {
    
    public enum Operation {
        INSERT,
        UPDATE,
        DELETE,
        CHECKPOINT
    }
    
    private final Operation operation;
    private LocalDateTime timestamp;  // Não final para permitir desserialização
    private final Map<String, Object> data;
    
    /**
     * Construtor para criar uma entrada de log.
     * 
     * @param operation Tipo de operação
     * @param data Dados da operação
     */
    public LogEntry(Operation operation, Map<String, Object> data) {
        this.operation = operation;
        this.timestamp = LocalDateTime.now();
        this.data = data;
    }
    
    /**
     * Serializa a entrada para o formato do WAL.
     * 
     * @return String no formato: OPERACAO|param1|param2|...
     */
    public String serialize() {
        StringBuilder sb = new StringBuilder();
        sb.append(operation.name()).append("|");
        sb.append(timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("|");
        
        switch (operation) {
            case INSERT -> {
                sb.append(data.get("id")).append("|");
                sb.append(escape((String) data.get("name"))).append("|");
                sb.append(data.get("age")).append("|");
                sb.append(escape((String) data.get("city")));
            }
            case UPDATE -> {
                sb.append(data.get("id")).append("|");
                Map<String, Object> changes = (Map<String, Object>) data.get("changes");
                for (var entry : changes.entrySet()) {
                    sb.append(entry.getKey()).append("=");
                    if (entry.getValue() instanceof String) {
                        sb.append(escape((String) entry.getValue()));
                    } else {
                        sb.append(entry.getValue());
                    }
                    sb.append("|");
                }
                // Remover último pipe
                sb.setLength(sb.length() - 1);
            }
            case DELETE -> {
                sb.append(data.get("id"));
            }
            case CHECKPOINT -> {
                sb.append(data.get("count"));
            }
        }
        
        return sb.toString();
    }
    
    /**
     * Desserializa uma linha do WAL em LogEntry.
     * 
     * @param line Linha do arquivo WAL
     * @return LogEntry ou null se linha inválida
     */
    public static LogEntry deserialize(String line) {
        if (line == null || line.trim().isEmpty()) {
            return null;
        }
        
        String[] parts = line.split("\\|", -1);  // -1 para manter campos vazios
        
        if (parts.length < 2) {
            return null;
        }
        
        try {
            Operation op = Operation.valueOf(parts[0]);
            LocalDateTime timestamp = LocalDateTime.parse(parts[1], 
                DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            
            Map<String, Object> data = new java.util.HashMap<>();
            
            switch (op) {
                case INSERT -> {
                    if (parts.length < 6) return null;
                    data.put("id", Integer.parseInt(parts[2]));
                    data.put("name", unescape(parts[3]));
                    data.put("age", Integer.parseInt(parts[4]));
                    data.put("city", unescape(parts[5]));
                }
                case UPDATE -> {
                    if (parts.length < 4) return null;
                    data.put("id", Integer.parseInt(parts[2]));
                    
                    Map<String, Object> changes = new java.util.HashMap<>();
                    for (int i = 3; i < parts.length; i++) {
                        String[] kv = parts[i].split("=", 2);
                        if (kv.length == 2) {
                            String key = kv[0];
                            String value = kv[1];
                            
                            // Tentar parsear como número, senão é string
                            try {
                                changes.put(key, Integer.parseInt(value));
                            } catch (NumberFormatException e) {
                                changes.put(key, unescape(value));
                            }
                        }
                    }
                    data.put("changes", changes);
                }
                case DELETE -> {
                    if (parts.length < 3) return null;
                    data.put("id", Integer.parseInt(parts[2]));
                }
                case CHECKPOINT -> {
                    if (parts.length < 3) return null;
                    data.put("count", Integer.parseInt(parts[2]));
                }
            }
            
            LogEntry entry = new LogEntry(op, data);
            entry.timestamp = timestamp;
            return entry;
            
        } catch (Exception e) {
            // Linha mal formada, ignorar
            return null;
        }
    }
    
    /**
     * Escapa caracteres especiais em strings.
     * Substitui | por \| e \ por \\
     */
    private static String escape(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\").replace("|", "\\|");
    }
    
    /**
     * Remove escape de caracteres especiais.
     */
    private static String unescape(String str) {
        if (str == null) return "";
        return str.replace("\\|", "|").replace("\\\\", "\\");
    }
    
    /**
     * Cria LogEntry para INSERT.
     */
    public static LogEntry createInsert(Users user) {
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("id", user.getId());
        data.put("name", user.getName());
        data.put("age", user.getAge());
        data.put("city", user.getCity());
        return new LogEntry(Operation.INSERT, data);
    }
    
    /**
     * Cria LogEntry para UPDATE.
     */
    public static LogEntry createUpdate(int id, Map<String, Object> changes) {
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("id", id);
        data.put("changes", changes);
        return new LogEntry(Operation.UPDATE, data);
    }
    
    /**
     * Cria LogEntry para DELETE.
     */
    public static LogEntry createDelete(int id) {
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("id", id);
        return new LogEntry(Operation.DELETE, data);
    }
    
    /**
     * Cria LogEntry para CHECKPOINT.
     */
    public static LogEntry createCheckpoint(int count) {
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("count", count);
        return new LogEntry(Operation.CHECKPOINT, data);
    }
    
    // Getters
    
    public Operation getOperation() {
        return operation;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public Map<String, Object> getData() {
        return data;
    }
    
    @Override
    public String toString() {
        return serialize();
    }
}
