package lib.persistence;

import lib.Users;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Gerencia arquivo de dados (snapshot do banco).
 * 
 * Usa serialização Java para salvar/carregar estado completo.
 * Arquivo binário compacto e eficiente.
 * 
 * @author SQL Parser Team
 * @version 2.0
 */
public class DataFile {
    
    private final Path dataPath;
    private final Path tempPath;
    
    /**
     * Construtor.
     * 
     * @param dataPath Caminho do arquivo de dados
     */
    public DataFile(Path dataPath) {
        this.dataPath = dataPath;
        this.tempPath = Paths.get(dataPath.toString() + ".tmp");
    }
    
    /**
     * Salva lista de usuários no arquivo.
     * Usa arquivo temporário para operação atômica.
     * 
     * @param users Lista de usuários
     * @throws IOException Se erro de I/O
     */
    public void save(List<Users> users) throws IOException {
        // Criar diretório se não existir
        Files.createDirectories(dataPath.getParent());
        
        // Salvar em arquivo temporário primeiro
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new BufferedOutputStream(
                    Files.newOutputStream(tempPath)))) {
            
            // Escrever versão do formato
            oos.writeInt(1);  // Versão 1
            
            // Escrever número de usuários
            oos.writeInt(users.size());
            
            // Escrever cada usuário
            for (Users user : users) {
                oos.writeInt(user.getId());
                oos.writeUTF(user.getName());
                oos.writeInt(user.getAge());
                oos.writeUTF(user.getCity());
            }
            
            oos.flush();
        }
        
        // Mover arquivo temporário para definitivo (operação atômica)
        Files.move(tempPath, dataPath, 
            StandardCopyOption.REPLACE_EXISTING,
            StandardCopyOption.ATOMIC_MOVE);
    }
    
    /**
     * Carrega lista de usuários do arquivo.
     * 
     * @return Lista de usuários
     * @throws IOException Se erro de I/O
     */
    public List<Users> load() throws IOException {
        if (!Files.exists(dataPath)) {
            return new ArrayList<>();
        }
        
        List<Users> users = new ArrayList<>();
        
        try (ObjectInputStream ois = new ObjectInputStream(
                new BufferedInputStream(
                    Files.newInputStream(dataPath)))) {
            
            // Ler versão do formato
            int version = ois.readInt();
            
            if (version != 1) {
                throw new IOException("Versão de arquivo não suportada: " + version);
            }
            
            // Ler número de usuários
            int count = ois.readInt();
            
            // Ler cada usuário
            for (int i = 0; i < count; i++) {
                int id = ois.readInt();
                String name = ois.readUTF();
                int age = ois.readInt();
                String city = ois.readUTF();
                
                users.add(new Users(id, name, age, city));
            }
        }
        
        return users;
    }
    
    /**
     * Verifica se arquivo de dados existe.
     * 
     * @return true se existe
     */
    public boolean exists() {
        return Files.exists(dataPath);
    }
    
    /**
     * Deleta arquivo de dados.
     * 
     * @throws IOException Se erro ao deletar
     */
    public void delete() throws IOException {
        Files.deleteIfExists(dataPath);
        Files.deleteIfExists(tempPath);
    }
    
    /**
     * Retorna tamanho do arquivo em bytes.
     * 
     * @return Tamanho em bytes ou 0 se não existe
     */
    public long getSize() {
        try {
            return Files.size(dataPath);
        } catch (IOException e) {
            return 0;
        }
    }
    
    /**
     * Retorna timestamp da última modificação.
     * 
     * @return Timestamp ou null se não existe
     */
    public java.time.Instant getLastModified() {
        try {
            return Files.getLastModifiedTime(dataPath).toInstant();
        } catch (IOException e) {
            return null;
        }
    }
    
    /**
     * Valida integridade do arquivo.
     * Tenta carregar para verificar se está corrompido.
     * 
     * @return true se válido
     */
    public boolean validate() {
        if (!exists()) {
            return true;  // Arquivo inexistente é considerado válido
        }
        
        try {
            load();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Cria backup do arquivo de dados.
     * 
     * @param backupPath Caminho do backup
     * @throws IOException Se erro ao copiar
     */
    public void backup(Path backupPath) throws IOException {
        if (exists()) {
            Files.copy(dataPath, backupPath, 
                StandardCopyOption.REPLACE_EXISTING);
        }
    }
    
    /**
     * Restaura arquivo de backup.
     * 
     * @param backupPath Caminho do backup
     * @throws IOException Se erro ao restaurar
     */
    public void restore(Path backupPath) throws IOException {
        if (Files.exists(backupPath)) {
            Files.copy(backupPath, dataPath, 
                StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
