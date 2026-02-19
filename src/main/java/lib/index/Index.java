package lib.index;

import java.util.List;

/**
 * Interface base para todos os tipos de índices.
 * 
 * Um índice mapeia valores de campos (chaves) para registros (valores),
 * permitindo busca rápida sem percorrer toda a tabela.
 * 
 * @param <K> Tipo da chave (ex: Integer para id, String para name)
 * @param <V> Tipo do valor (ex: Users)
 * 
 * @author SQL Parser Team
 * @version 2.0
 */
public interface Index<K, V> {
    
    /**
     * Insere um registro no índice.
     * 
     * @param key Valor da chave (ex: id = 5)
     * @param value Registro completo (ex: objeto Users)
     */
    void insert(K key, V value);
    
    /**
     * Remove um registro do índice.
     * 
     * @param key Valor da chave
     * @param value Registro a remover
     * @return true se removeu, false se não encontrou
     */
    boolean remove(K key, V value);
    
    /**
     * Busca registros pela chave exata.
     * 
     * @param key Valor da chave
     * @return Lista de registros que correspondem à chave
     */
    List<V> search(K key);
    
    /**
     * Reconstrói o índice a partir de uma lista de dados.
     * Útil após múltiplas operações ou para inicialização.
     * 
     * @param data Lista completa de registros
     */
    void rebuild(List<V> data);
    
    /**
     * Limpa completamente o índice.
     */
    void clear();
    
    /**
     * Retorna o número de entradas no índice.
     * 
     * @return Número total de chaves indexadas
     */
    int size();
    
    /**
     * Verifica se o índice está vazio.
     * 
     * @return true se vazio, false caso contrário
     */
    boolean isEmpty();
}
