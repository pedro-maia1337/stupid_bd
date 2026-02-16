package lib.index;

import java.util.*;
import java.util.function.Function;

/**
 * Índice Hash para buscas de igualdade exata.
 * 
 * Usa HashMap internamente para acesso O(1).
 * Ideal para queries: WHERE campo = valor
 * 
 * Exemplo:
 *   HashIndex<Integer, Users> idIndex = new HashIndex<>(Users::getId);
 *   idIndex.search(5) → retorna Users com id=5 em O(1)
 * 
 * @param <K> Tipo da chave
 * @param <V> Tipo do valor (registro)
 * 
 * @author SQL Parser Team
 * @version 2.0
 */
public class HashIndex<K, V> implements Index<K, V> {
    
    // HashMap para armazenar chave → lista de registros
    private final Map<K, List<V>> index;
    
    // Função para extrair a chave de um registro
    private final Function<V, K> keyExtractor;
    
    // Nome do índice (para debug)
    private final String indexName;
    
    /**
     * Construtor com extrator de chave.
     * 
     * @param keyExtractor Função que extrai a chave do registro
     *                     Ex: Users::getId, Users::getName
     */
    public HashIndex(Function<V, K> keyExtractor) {
        this(keyExtractor, "HashIndex");
    }
    
    /**
     * Construtor com extrator de chave e nome.
     * 
     * @param keyExtractor Função que extrai a chave do registro
     * @param indexName Nome do índice (para debug)
     */
    public HashIndex(Function<V, K> keyExtractor, String indexName) {
        this.index = new HashMap<>();
        this.keyExtractor = keyExtractor;
        this.indexName = indexName;
    }
    
    @Override
    public void insert(K key, V value) {
        // Obter ou criar lista para esta chave
        index.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
    }
    
    @Override
    public boolean remove(K key, V value) {
        List<V> values = index.get(key);
        
        if (values == null) {
            return false;
        }
        
        boolean removed = values.remove(value);
        
        // Se a lista ficou vazia, remover a chave
        if (values.isEmpty()) {
            index.remove(key);
        }
        
        return removed;
    }
    
    @Override
    public List<V> search(K key) {
        List<V> result = index.get(key);
        
        // Retornar cópia para evitar modificações externas
        if (result == null) {
            return new ArrayList<>();
        }
        
        return new ArrayList<>(result);
    }
    
    @Override
    public void rebuild(List<V> data) {
        clear();
        
        for (V record : data) {
            K key = keyExtractor.apply(record);
            insert(key, record);
        }
    }
    
    @Override
    public void clear() {
        index.clear();
    }
    
    @Override
    public int size() {
        return index.size();
    }
    
    @Override
    public boolean isEmpty() {
        return index.isEmpty();
    }
    
    /**
     * Retorna todas as chaves presentes no índice.
     * 
     * @return Set de chaves
     */
    public Set<K> keys() {
        return new HashSet<>(index.keySet());
    }
    
    /**
     * Retorna estatísticas do índice.
     * 
     * @return String com estatísticas
     */
    public String getStats() {
        int totalRecords = index.values().stream()
                .mapToInt(List::size)
                .sum();
        
        return String.format("%s: %d chaves, %d registros", 
                indexName, size(), totalRecords);
    }
    
    @Override
    public String toString() {
        return getStats();
    }
}
