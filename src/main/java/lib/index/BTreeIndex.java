package lib.index;

import java.util.*;
import java.util.function.Function;

/**
 * Índice B-Tree para buscas por intervalo e ordenação.
 * 
 * Usa TreeMap internamente para acesso O(log n) e ordenação.
 * Ideal para queries:
 *   - WHERE campo BETWEEN x AND y
 *   - WHERE campo > x
 *   - ORDER BY campo
 * 
 * Exemplo:
 *   BTreeIndex<Integer, Users> ageIndex = new BTreeIndex<>(Users::getAge);
 *   ageIndex.searchRange(25, 35) → retorna Users com age entre 25-35
 * 
 * @param <K> Tipo da chave (deve ser Comparable)
 * @param <V> Tipo do valor (registro)
 * 
 * @author SQL Parser Team
 * @version 2.0
 */
public class BTreeIndex<K extends Comparable<K>, V> implements Index<K, V> {
    
    // TreeMap para armazenar chave → lista de registros (ordenado)
    private final TreeMap<K, List<V>> index;
    
    // Função para extrair a chave de um registro
    private final Function<V, K> keyExtractor;
    
    // Nome do índice (para debug)
    private final String indexName;
    
    /**
     * Construtor com extrator de chave.
     * 
     * @param keyExtractor Função que extrai a chave do registro
     */
    public BTreeIndex(Function<V, K> keyExtractor) {
        this(keyExtractor, "BTreeIndex");
    }
    
    /**
     * Construtor com extrator de chave e nome.
     * 
     * @param keyExtractor Função que extrai a chave do registro
     * @param indexName Nome do índice (para debug)
     */
    public BTreeIndex(Function<V, K> keyExtractor, String indexName) {
        this.index = new TreeMap<>();
        this.keyExtractor = keyExtractor;
        this.indexName = indexName;
    }
    
    @Override
    public void insert(K key, V value) {
        index.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
    }
    
    @Override
    public boolean remove(K key, V value) {
        List<V> values = index.get(key);
        
        if (values == null) {
            return false;
        }
        
        boolean removed = values.remove(value);
        
        if (values.isEmpty()) {
            index.remove(key);
        }
        
        return removed;
    }
    
    @Override
    public List<V> search(K key) {
        List<V> result = index.get(key);
        
        if (result == null) {
            return new ArrayList<>();
        }
        
        return new ArrayList<>(result);
    }
    
    /**
     * Busca por intervalo [minKey, maxKey] (inclusivo).
     * 
     * @param minKey Chave mínima
     * @param maxKey Chave máxima
     * @return Lista de registros no intervalo
     */
    public List<V> searchRange(K minKey, K maxKey) {
        List<V> result = new ArrayList<>();
        
        // subMap retorna view do TreeMap no intervalo
        for (List<V> values : index.subMap(minKey, true, maxKey, true).values()) {
            result.addAll(values);
        }
        
        return result;
    }
    
    /**
     * Busca registros com chave maior que o valor especificado.
     * 
     * @param key Valor mínimo (exclusivo)
     * @return Lista de registros
     */
    public List<V> searchGreaterThan(K key) {
        List<V> result = new ArrayList<>();
        
        for (List<V> values : index.tailMap(key, false).values()) {
            result.addAll(values);
        }
        
        return result;
    }
    
    /**
     * Busca registros com chave menor que o valor especificado.
     * 
     * @param key Valor máximo (exclusivo)
     * @return Lista de registros
     */
    public List<V> searchLessThan(K key) {
        List<V> result = new ArrayList<>();
        
        for (List<V> values : index.headMap(key, false).values()) {
            result.addAll(values);
        }
        
        return result;
    }
    
    /**
     * Retorna todos os registros em ordem crescente.
     * Útil para ORDER BY.
     * 
     * @return Lista ordenada de registros
     */
    public List<V> getAllOrdered() {
        List<V> result = new ArrayList<>();
        
        for (List<V> values : index.values()) {
            result.addAll(values);
        }
        
        return result;
    }
    
    /**
     * Retorna todos os registros em ordem decrescente.
     * Útil para ORDER BY DESC.
     * 
     * @return Lista ordenada de registros (decrescente)
     */
    public List<V> getAllOrderedDesc() {
        List<V> result = new ArrayList<>();
        
        for (List<V> values : index.descendingMap().values()) {
            result.addAll(values);
        }
        
        return result;
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
     * Retorna a menor chave no índice.
     * 
     * @return Menor chave ou null se vazio
     */
    public K getMinKey() {
        return index.isEmpty() ? null : index.firstKey();
    }
    
    /**
     * Retorna a maior chave no índice.
     * 
     * @return Maior chave ou null se vazio
     */
    public K getMaxKey() {
        return index.isEmpty() ? null : index.lastKey();
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
        
        K min = getMinKey();
        K max = getMaxKey();
        
        return String.format("%s: %d chaves, %d registros, range[%s, %s]", 
                indexName, size(), totalRecords, min, max);
    }
    
    @Override
    public String toString() {
        return getStats();
    }
}
