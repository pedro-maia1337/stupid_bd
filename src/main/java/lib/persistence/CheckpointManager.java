package lib.persistence;

import lib.Users;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Gerencia checkpoints do banco de dados.
 * 
 * Implementa múltiplas estratégias de checkpoint:
 * - Por contagem de operações
 * - Por tamanho do WAL
 * - Por tempo decorrido
 * - Híbrida (combinação das anteriores)
 * 
 * Checkpoint salva snapshot completo do banco e limpa o WAL.
 * 
 * @author SQL Parser Team
 * @version 2.0
 */
public class CheckpointManager {
    
    /**
     * Estratégia de checkpoint.
     */
    public enum Strategy {
        /** Checkpoint a cada N operações */
        OPERATION_COUNT,
        
        /** Checkpoint quando WAL atinge tamanho X */
        WAL_SIZE,
        
        /** Checkpoint a cada X tempo */
        TIME_BASED,
        
        /** Checkpoint quando qualquer condição é satisfeita */
        HYBRID
    }
    
    // Configuração
    private final Strategy strategy;
    private final int operationThreshold;      // Operações para checkpoint
    private final long walSizeThreshold;       // Bytes para checkpoint
    private final Duration timeThreshold;      // Tempo para checkpoint
    
    // Estado
    private int operationCount;
    private Instant lastCheckpoint;
    private boolean checkpointInProgress;
    
    // Estatísticas
    private int totalCheckpoints;
    private long totalCheckpointTime;
    private Instant lastCheckpointTime;
    
    /**
     * Construtor com estratégia padrão (HYBRID).
     */
    public CheckpointManager() {
        this(Strategy.HYBRID, 100, 10 * 1024 * 1024, Duration.ofMinutes(5));
    }
    
    /**
     * Construtor completo.
     * 
     * @param strategy Estratégia de checkpoint
     * @param operationThreshold Threshold de operações
     * @param walSizeThreshold Threshold de tamanho WAL (bytes)
     * @param timeThreshold Threshold de tempo
     */
    public CheckpointManager(Strategy strategy, int operationThreshold,
                           long walSizeThreshold, Duration timeThreshold) {
        this.strategy = strategy;
        this.operationThreshold = operationThreshold;
        this.walSizeThreshold = walSizeThreshold;
        this.timeThreshold = timeThreshold;
        
        this.operationCount = 0;
        this.lastCheckpoint = Instant.now();
        this.checkpointInProgress = false;
        
        this.totalCheckpoints = 0;
        this.totalCheckpointTime = 0;
    }
    
    /**
     * Registra uma operação.
     * Incrementa contador interno.
     */
    public void recordOperation() {
        operationCount++;
    }
    
    /**
     * Verifica se checkpoint é necessário.
     * 
     * @param walSize Tamanho atual do WAL em bytes
     * @return true se checkpoint recomendado
     */
    public boolean shouldCheckpoint(long walSize) {
        if (checkpointInProgress) {
            return false;
        }
        
        switch (strategy) {
            case OPERATION_COUNT:
                return operationCount >= operationThreshold;
                
            case WAL_SIZE:
                return walSize >= walSizeThreshold;
                
            case TIME_BASED:
                Duration elapsed = Duration.between(lastCheckpoint, Instant.now());
                return elapsed.compareTo(timeThreshold) >= 0;
                
            case HYBRID:
                // Checkpoint se QUALQUER condição for satisfeita
                boolean byOps = operationCount >= operationThreshold;
                boolean bySize = walSize >= walSizeThreshold;
                
                Duration elapsedTime = Duration.between(lastCheckpoint, Instant.now());
                boolean byTime = elapsedTime.compareTo(timeThreshold) >= 0;
                
                return byOps || bySize || byTime;
                
            default:
                return false;
        }
    }
    
    /**
     * Executa checkpoint.
     * 
     * @param database Estado atual do banco
     * @param persistenceManager Gerenciador de persistência
     * @throws IOException Se erro ao executar checkpoint
     */
    public void executeCheckpoint(List<Users> database, 
                                 PersistenceManager persistenceManager) 
                                 throws IOException {
        if (checkpointInProgress) {
            throw new IllegalStateException("Checkpoint já em progresso");
        }
        
        checkpointInProgress = true;
        long startTime = System.currentTimeMillis();
        
        try {
            // Delegar para PersistenceManager
            persistenceManager.checkpoint(database);
            
            // Atualizar estado
            operationCount = 0;
            lastCheckpoint = Instant.now();
            totalCheckpoints++;
            
            long elapsedTime = System.currentTimeMillis() - startTime;
            totalCheckpointTime += elapsedTime;
            lastCheckpointTime = Instant.now();
            
        } finally {
            checkpointInProgress = false;
        }
    }
    
    /**
     * Força checkpoint imediato.
     * 
     * @param database Estado atual do banco
     * @param persistenceManager Gerenciador de persistência
     * @throws IOException Se erro ao executar checkpoint
     */
    public void forceCheckpoint(List<Users> database,
                               PersistenceManager persistenceManager)
                               throws IOException {
        executeCheckpoint(database, persistenceManager);
    }
    
    /**
     * Retorna estatísticas de checkpoint.
     * 
     * @return String formatada com estatísticas
     */
    public String getStats() {
        StringBuilder sb = new StringBuilder();
        sb.append("╔════════════════════════════════════════════════╗\n");
        sb.append("║       ESTATÍSTICAS DE CHECKPOINT               ║\n");
        sb.append("╚════════════════════════════════════════════════╝\n\n");
        
        sb.append("Estratégia: ").append(strategy).append("\n\n");
        
        sb.append("Thresholds:\n");
        sb.append("  - Operações: ").append(operationThreshold).append("\n");
        sb.append("  - Tamanho WAL: ").append(formatBytes(walSizeThreshold)).append("\n");
        sb.append("  - Tempo: ").append(timeThreshold.toMinutes()).append(" minutos\n\n");
        
        sb.append("Estado Atual:\n");
        sb.append("  - Operações acumuladas: ").append(operationCount).append("\n");
        sb.append("  - Progresso: ").append(getProgressPercentage()).append("%\n");
        Duration timeSinceCheckpoint = Duration.between(lastCheckpoint, Instant.now());
        sb.append("  - Tempo desde último: ").append(formatDuration(timeSinceCheckpoint)).append("\n");
        sb.append("  - Checkpoint em progresso: ").append(checkpointInProgress ? "Sim" : "Não").append("\n\n");
        
        sb.append("Histórico:\n");
        sb.append("  - Total de checkpoints: ").append(totalCheckpoints).append("\n");
        if (totalCheckpoints > 0) {
            long avgTime = totalCheckpointTime / totalCheckpoints;
            sb.append("  - Tempo médio: ").append(avgTime).append(" ms\n");
            sb.append("  - Tempo total: ").append(totalCheckpointTime).append(" ms\n");
            sb.append("  - Último checkpoint: ").append(lastCheckpointTime).append("\n");
        }
        
        return sb.toString();
    }
    
    /**
     * Retorna progresso até próximo checkpoint (0-100%).
     * 
     * @return Porcentagem de progresso
     */
    public int getProgressPercentage() {
        if (operationThreshold == 0) {
            return 0;
        }
        
        int progress = (operationCount * 100) / operationThreshold;
        return Math.min(progress, 100);
    }
    
    /**
     * Verifica se checkpoint está em progresso.
     * 
     * @return true se em progresso
     */
    public boolean isCheckpointInProgress() {
        return checkpointInProgress;
    }
    
    /**
     * Retorna número de operações desde último checkpoint.
     * 
     * @return Contagem
     */
    public int getOperationCount() {
        return operationCount;
    }
    
    /**
     * Retorna número total de checkpoints executados.
     * 
     * @return Total
     */
    public int getTotalCheckpoints() {
        return totalCheckpoints;
    }
    
    /**
     * Retorna tempo médio de checkpoint.
     * 
     * @return Tempo médio em ms
     */
    public long getAverageCheckpointTime() {
        if (totalCheckpoints == 0) {
            return 0;
        }
        return totalCheckpointTime / totalCheckpoints;
    }
    
    /**
     * Retorna informações de diagnóstico.
     * 
     * @param walSize Tamanho atual do WAL
     * @return String com diagnóstico
     */
    public String getDiagnostics(long walSize) {
        StringBuilder sb = new StringBuilder();
        sb.append("Diagnóstico de Checkpoint:\n");
        
        // Verificar cada condição
        boolean needsByOps = operationCount >= operationThreshold;
        boolean needsBySize = walSize >= walSizeThreshold;
        
        Duration elapsed = Duration.between(lastCheckpoint, Instant.now());
        boolean needsByTime = elapsed.compareTo(timeThreshold) >= 0;
        
        sb.append("  - Por operações: ").append(operationCount).append("/")
          .append(operationThreshold).append(" ")
          .append(needsByOps ? "✓ TRIGGER" : "✗").append("\n");
        
        sb.append("  - Por tamanho: ").append(formatBytes(walSize)).append("/")
          .append(formatBytes(walSizeThreshold)).append(" ")
          .append(needsBySize ? "✓ TRIGGER" : "✗").append("\n");
        
        sb.append("  - Por tempo: ").append(formatDuration(elapsed)).append("/")
          .append(formatDuration(timeThreshold)).append(" ")
          .append(needsByTime ? "✓ TRIGGER" : "✗").append("\n");
        
        boolean shouldCheckpoint = shouldCheckpoint(walSize);
        sb.append("\nRecomendação: ").append(shouldCheckpoint ? "CHECKPOINT AGORA" : "Aguardar");
        
        return sb.toString();
    }
    
    /**
     * Reseta contador de operações.
     * Usado após checkpoint.
     */
    public void reset() {
        operationCount = 0;
        lastCheckpoint = Instant.now();
    }
    
    // Métodos auxiliares de formatação
    
    private String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else {
            return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        }
    }
    
    private String formatDuration(Duration duration) {
        long seconds = duration.getSeconds();
        
        if (seconds < 60) {
            return seconds + "s";
        } else if (seconds < 3600) {
            long minutes = seconds / 60;
            long secs = seconds % 60;
            return minutes + "m " + secs + "s";
        } else {
            long hours = seconds / 3600;
            long minutes = (seconds % 3600) / 60;
            return hours + "h " + minutes + "m";
        }
    }
}
