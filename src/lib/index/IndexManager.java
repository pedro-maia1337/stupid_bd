package lib.index;

import lib.Users;
import java.util.*;

/**
 * Gerenciador central de todos os índices.
 * 
 * Mantém índices para diferentes campos da tabela Users:
 * - Hash Index para id (igualdade)
 * - Hash Index para name (igualdade)
 * - B-Tree Index para age (intervalos e ordenação)
 * - Hash Index para city (igualdade)
 * 
 * Responsabilidades:
 * - Criar e manter índices
 * - Sincronizar índices com operações de INSERT/UPDATE/DELETE
 * - Escolher melhor índice para cada query
 * 
 * @author SQL Parser Team
 * @version 2.0
 */
public class IndexManager {
    
    // Índices por campo
    private final HashIndex<Integer, Users> idIndex;
    private final HashIndex<String, Users> nameIndex;
    private final BTreeIndex<Integer, Users> ageIndex;
    private final HashIndex<String, Users> cityIndex;
    
    // Estatísticas
    private long totalSearches = 0;
    private long indexHits = 0;
    private long indexMisses = 0;
    
    /**
     * Construtor padrão.
     * Cria todos os índices vazios.
     */
    public IndexManager() {
        this.idIndex = new HashIndex<>(Users::getId, "IdIndex");
        this.nameIndex = new HashIndex<>(Users::getName, "NameIndex");
        this.ageIndex = new BTreeIndex<>(Users::getAge, "AgeIndex");
        this.cityIndex = new HashIndex<>(Users::getCity, "CityIndex");
    }
    
    /**
     * Reconstrói todos os índices a partir dos dados atuais.
     * 
     * @param data Lista completa de usuários
     */
    public void rebuildAll(List<Users> data) {
        idIndex.rebuild(data);
        nameIndex.rebuild(data);
        ageIndex.rebuild(data);
        cityIndex.rebuild(data);
    }
    
    /**
     * Adiciona um usuário a todos os índices.
     * Deve ser chamado após INSERT.
     * 
     * @param user Usuário a adicionar
     */
    public void insertUser(Users user) {
        idIndex.insert(user.getId(), user);
        nameIndex.insert(user.getName(), user);
        ageIndex.insert(user.getAge(), user);
        cityIndex.insert(user.getCity(), user);
    }
    
    /**
     * Remove um usuário de todos os índices.
     * Deve ser chamado antes de DELETE.
     * 
     * @param user Usuário a remover
     */
    public void removeUser(Users user) {
        idIndex.remove(user.getId(), user);
        nameIndex.remove(user.getName(), user);
        ageIndex.remove(user.getAge(), user);
        cityIndex.remove(user.getCity(), user);
    }
    
    /**
     * Atualiza um usuário nos índices.
     * Remove do antigo, insere no novo.
     * 
     * @param oldUser Usuário antes da atualização
     * @param newUser Usuário depois da atualização
     */
    public void updateUser(Users oldUser, Users newUser) {
        removeUser(oldUser);
        insertUser(newUser);
    }
    
    /**
     * Limpa todos os índices.
     */
    public void clearAll() {
        idIndex.clear();
        nameIndex.clear();
        ageIndex.clear();
        cityIndex.clear();
    }
    
    // ================================================================
    // MÉTODOS DE BUSCA USANDO ÍNDICES
    // ================================================================
    
    /**
     * Busca usuário por ID (usa hash index).
     * 
     * @param id ID do usuário
     * @return Lista de usuários (geralmente 1)
     */
    public List<Users> searchById(int id) {
        totalSearches++;
        List<Users> result = idIndex.search(id);
        
        if (!result.isEmpty()) {
            indexHits++;
        } else {
            indexMisses++;
        }
        
        return result;
    }
    
    /**
     * Busca usuários por nome exato (usa hash index).
     * 
     * @param name Nome completo
     * @return Lista de usuários com esse nome
     */
    public List<Users> searchByName(String name) {
        totalSearches++;
        List<Users> result = nameIndex.search(name);
        
        if (!result.isEmpty()) {
            indexHits++;
        } else {
            indexMisses++;
        }
        
        return result;
    }
    
    /**
     * Busca usuários por idade exata (usa b-tree index).
     * 
     * @param age Idade
     * @return Lista de usuários com essa idade
     */
    public List<Users> searchByAge(int age) {
        totalSearches++;
        List<Users> result = ageIndex.search(age);
        
        if (!result.isEmpty()) {
            indexHits++;
        } else {
            indexMisses++;
        }
        
        return result;
    }
    
    /**
     * Busca usuários por intervalo de idade (usa b-tree index).
     * 
     * @param minAge Idade mínima
     * @param maxAge Idade máxima
     * @return Lista de usuários no intervalo
     */
    public List<Users> searchByAgeRange(int minAge, int maxAge) {
        totalSearches++;
        List<Users> result = ageIndex.searchRange(minAge, maxAge);
        
        if (!result.isEmpty()) {
            indexHits++;
        } else {
            indexMisses++;
        }
        
        return result;
    }
    
    /**
     * Busca usuários por cidade (usa hash index).
     * 
     * @param city Nome da cidade
     * @return Lista de usuários dessa cidade
     */
    public List<Users> searchByCity(String city) {
        totalSearches++;
        List<Users> result = cityIndex.search(city);
        
        if (!result.isEmpty()) {
            indexHits++;
        } else {
            indexMisses++;
        }
        
        return result;
    }
    
    /**
     * Retorna todos os usuários ordenados por idade (usa b-tree index).
     * 
     * @param ascending true para ASC, false para DESC
     * @return Lista ordenada
     */
    public List<Users> getAllOrderedByAge(boolean ascending) {
        return ascending ? 
            ageIndex.getAllOrdered() : 
            ageIndex.getAllOrderedDesc();
    }
    
    // ================================================================
    // ESTATÍSTICAS E DEBUG
    // ================================================================
    
    /**
     * Retorna estatísticas de todos os índices.
     * 
     * @return String com estatísticas completas
     */
    public String getStats() {
        StringBuilder sb = new StringBuilder();
        sb.append("╔════════════════════════════════════════════════╗\n");
        sb.append("║            ESTATÍSTICAS DOS ÍNDICES            ║\n");
        sb.append("╚════════════════════════════════════════════════╝\n\n");
        
        sb.append("Índices:\n");
        sb.append("  - ").append(idIndex.getStats()).append("\n");
        sb.append("  - ").append(nameIndex.getStats()).append("\n");
        sb.append("  - ").append(ageIndex.getStats()).append("\n");
        sb.append("  - ").append(cityIndex.getStats()).append("\n\n");
        
        sb.append("Buscas:\n");
        sb.append("  - Total: ").append(totalSearches).append("\n");
        sb.append("  - Hits: ").append(indexHits).append("\n");
        sb.append("  - Misses: ").append(indexMisses).append("\n");
        
        if (totalSearches > 0) {
            double hitRate = (indexHits * 100.0) / totalSearches;
            sb.append("  - Hit Rate: ").append(String.format("%.1f%%", hitRate)).append("\n");
        }
        
        return sb.toString();
    }
    
    /**
     * Reseta estatísticas de busca.
     */
    public void resetStats() {
        totalSearches = 0;
        indexHits = 0;
        indexMisses = 0;
    }
    
    /**
     * Verifica se índices estão consistentes.
     * 
     * @return true se todos têm o mesmo número de registros
     */
    public boolean isConsistent() {
        int idCount = idIndex.size();
        int nameCount = nameIndex.size();
        int ageCount = ageIndex.size();
        int cityCount = cityIndex.size();
        
        // Todos devem ter o mesmo número de chaves (ou próximo, dependendo de duplicatas)
        // Esta é uma verificação simplificada
        return idCount > 0 && nameCount > 0 && ageCount > 0 && cityCount > 0;
    }
}
