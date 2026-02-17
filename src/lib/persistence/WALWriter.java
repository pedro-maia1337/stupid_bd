package lib.persistence;

import java.io.*;
import java.nio.file.*;

/**
 * Escreve operações no Write-Ahead Log.
 *
 * Garante durabilidade através de:
 * - Escrita sequencial (append-only)
 * - fsync após cada operação (opcional)
 * - Modo sincronizado para thread-safety
 *
 * @author SQL Parser Team
 * @version 2.0
 */
public class WALWriter implements AutoCloseable {

    private final Path walPath;
    private BufferedWriter writer;
    private FileOutputStream fileOutputStream;  // Usar FileOutputStream para fsync
    private final boolean syncOnWrite;
    private int operationCount;

    /**
     * Construtor.
     *
     * @param walPath Caminho do arquivo WAL
     * @param syncOnWrite Se true, faz fsync após cada write
     * @throws IOException Se erro ao abrir arquivo
     */
    public WALWriter(Path walPath, boolean syncOnWrite) throws IOException {
        this.walPath = walPath;
        this.syncOnWrite = syncOnWrite;
        this.operationCount = 0;

        // Criar diretório se não existir
        Files.createDirectories(walPath.getParent());

        // Abrir arquivo em modo append usando FileOutputStream
        this.fileOutputStream = new FileOutputStream(walPath.toFile(), true);
        this.writer = new BufferedWriter(new OutputStreamWriter(fileOutputStream));
    }

    /**
     * Escreve uma entrada no log.
     *
     * @param entry Entrada a escrever
     * @throws IOException Se erro de I/O
     */
    public synchronized void write(LogEntry entry) throws IOException {
        if (writer == null) {
            throw new IOException("WAL writer is closed");
        }

        String line = entry.serialize();
        writer.write(line);
        writer.newLine();

        if (syncOnWrite) {
            writer.flush();
            // Force sync para disco usando FileDescriptor
            fileOutputStream.getFD().sync();
        }

        operationCount++;
    }

    /**
     * Força gravação no disco.
     *
     * @throws IOException Se erro de I/O
     */
    public synchronized void flush() throws IOException {
        if (writer != null) {
            writer.flush();
            if (fileOutputStream != null && syncOnWrite) {
                fileOutputStream.getFD().sync();
            }
        }
    }

    /**
     * Rotaciona o WAL usando cópia em vez de move.
     * Mais robusto no Windows onde arquivos podem ficar locked.
     *
     * @param backupPath Caminho para backup do WAL antigo
     * @throws IOException Se erro de I/O
     */
    public synchronized void rotate(Path backupPath) throws IOException {
        // 1. Flush tudo antes
        if (writer != null) {
            writer.flush();
        }
        if (fileOutputStream != null) {
            fileOutputStream.flush();
        }

        // 2. Copiar arquivo atual para backup (não fecha, apenas copia)
        if (Files.exists(walPath) && Files.size(walPath) > 0) {
            try {
                Files.copy(walPath, backupPath, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                System.err.println("Aviso: Não foi possível criar backup do WAL: " + e.getMessage());
                // Continuar mesmo assim
            }
        }

        // 3. Fechar arquivo atual
        if (writer != null) {
            writer.close();
            writer = null;
        }
        if (fileOutputStream != null) {
            fileOutputStream.close();
            fileOutputStream = null;
        }

        // 4. Esperar um pouco para o SO liberar
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 5. Reabrir arquivo TRUNCANDO (limpa o conteúdo)
        // Isso é equivalente a criar novo arquivo, mas não precisa mover/deletar
        this.fileOutputStream = new FileOutputStream(walPath.toFile(), false);  // false = truncate
        this.writer = new BufferedWriter(new OutputStreamWriter(fileOutputStream));

        this.operationCount = 0;
    }

    /**
     * Retorna número de operações escritas desde último checkpoint.
     *
     * @return Contagem de operações
     */
    public int getOperationCount() {
        return operationCount;
    }

    /**
     * Reseta contador de operações.
     */
    public void resetCount() {
        this.operationCount = 0;
    }

    /**
     * Retorna tamanho do arquivo WAL em bytes.
     *
     * @return Tamanho em bytes
     * @throws IOException Se erro ao ler arquivo
     */
    public long getSize() throws IOException {
        flush();
        return Files.size(walPath);
    }

    @Override
    public synchronized void close() throws IOException {
        if (writer != null) {
            writer.flush();
            writer.close();
            writer = null;
        }
        if (fileOutputStream != null) {
            fileOutputStream.close();
            fileOutputStream = null;
        }
    }

    /**
     * Verifica se o writer está aberto.
     *
     * @return true se aberto
     */
    public boolean isOpen() {
        return writer != null && fileOutputStream != null;
    }
}