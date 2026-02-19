

import lib.UserQuery;
import lib.Users;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.*;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

//Precisa Desabilitar a persistência do banco para os testes
//UserQuery.java - > public UserQuery() {
//                      this(true, "data/");
//                   }

@DisplayName("UserQuery - Testes de Manipulação de Dados")
class UserQueryTest {

    private UserQuery userQuery;

    @BeforeEach
    void setUp() {
        userQuery = new UserQuery();
    }

    // ==================== TESTES FROM ====================

    @Test
    @DisplayName("FROM - Deve retornar todos os usuários quando não há condição")
    void testFromSemCondicao() {
        List<Users> result = userQuery.from();
        
        assertNotNull(result);
        assertEquals(30, result.size());
    }

    @Test
    @DisplayName("FROM - Deve retornar todos os usuários quando condição é null")
    void testFromComCondicaoNull() {
        List<Users> result = userQuery.from(null);
        
        assertNotNull(result);
        assertEquals(30, result.size());
    }

    @Test
    @DisplayName("FROM - Deve filtrar usuários com condição específica")
    void testFromComCondicao() {
        Predicate<Users> condition = u -> u.getAge() > 40;
        List<Users> result = userQuery.from(condition);
        
        assertNotNull(result);
        assertTrue(result.size() > 0);
        assertTrue(result.stream().allMatch(u -> u.getAge() > 40));
    }

    @Test
    @DisplayName("FROM - Deve retornar lista vazia quando nenhum usuário atende a condição")
    void testFromSemResultados() {
        Predicate<Users> condition = u -> u.getAge() > 100;
        List<Users> result = userQuery.from(condition);
        
        assertNotNull(result);
        assertEquals(0, result.size());
    }

    // ==================== TESTES SELECT ====================

    @Test
    @DisplayName("SELECT - Deve retornar todas as colunas com *")
    void testSelectTodasColunas() {
        List<Users> users = userQuery.from();
        List<Map<String, Object>> result = userQuery.select("*", users);
        
        assertNotNull(result);
        assertEquals(30, result.size());
        
        Map<String, Object> firstRow = result.get(0);
        assertTrue(firstRow.containsKey("id"));
        assertTrue(firstRow.containsKey("name"));
        assertTrue(firstRow.containsKey("age"));
        assertTrue(firstRow.containsKey("city"));
    }

    @Test
    @DisplayName("SELECT - Deve retornar apenas colunas específicas")
    void testSelectColunasEspecificas() {
        List<Users> users = userQuery.from();
        List<Map<String, Object>> result = userQuery.select("name,age", users);
        
        assertNotNull(result);
        assertEquals(30, result.size());
        
        Map<String, Object> firstRow = result.get(0);
        assertTrue(firstRow.containsKey("name"));
        assertTrue(firstRow.containsKey("age"));
        assertFalse(firstRow.containsKey("id"));
        assertFalse(firstRow.containsKey("city"));
    }

    @Test
    @DisplayName("SELECT - Deve retornar count corretamente")
    void testSelectCount() {
        List<Users> users = userQuery.from();
        List<Map<String, Object>> result = userQuery.select("count", users);
        
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(30, result.get(0).get("count"));
    }

    @Test
    @DisplayName("SELECT - Deve retornar count com filtro")
    void testSelectCountComFiltro() {
        List<Users> users = userQuery.from(u -> u.getAge() > 40);
        List<Map<String, Object>> result = userQuery.select("count", users);
        
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue((Integer) result.get(0).get("count") > 0);
    }

    @Test
    @DisplayName("SELECT - Deve lidar com espaços nos nomes das colunas")
    void testSelectComEspacos() {
        List<Users> users = userQuery.from();
        List<Map<String, Object>> result = userQuery.select("name , age , city", users);
        
        assertNotNull(result);
        Map<String, Object> firstRow = result.get(0);
        assertTrue(firstRow.containsKey("name"));
        assertTrue(firstRow.containsKey("age"));
        assertTrue(firstRow.containsKey("city"));
    }

    @Test
    @DisplayName("SELECT - Deve retornar lista vazia para resultado vazio")
    void testSelectResultadoVazio() {
        List<Users> emptyList = new ArrayList<>();
        List<Map<String, Object>> result = userQuery.select("*", emptyList);
        
        assertNotNull(result);
        assertEquals(0, result.size());
    }

    // ==================== TESTES ORDER BY ====================

    @Test
    @DisplayName("ORDER BY - Deve ordenar por ID ascendente")
    void testOrderByIdAsc() {
        List<Users> users = userQuery.from();
        List<Users> result = userQuery.orderBy("id", "asc", users);
        
        assertNotNull(result);
        assertEquals(30, result.size());
        assertEquals(1, result.get(0).getId());
        assertEquals(30, result.get(29).getId());
    }

    @Test
    @DisplayName("ORDER BY - Deve ordenar por ID descendente")
    void testOrderByIdDesc() {
        List<Users> users = userQuery.from();
        List<Users> result = userQuery.orderBy("id", "desc", users);
        
        assertNotNull(result);
        assertEquals(30, result.size());
        assertEquals(30, result.get(0).getId());
        assertEquals(1, result.get(29).getId());
    }

    @Test
    @DisplayName("ORDER BY - Deve ordenar por nome ascendente")
    void testOrderByNameAsc() {
        List<Users> users = userQuery.from();
        List<Users> result = userQuery.orderBy("name", "asc", users);
        
        assertNotNull(result);
        assertTrue(result.get(0).getName().compareToIgnoreCase(result.get(1).getName()) <= 0);
    }

    @Test
    @DisplayName("ORDER BY - Deve ordenar por idade ascendente")
    void testOrderByAgeAsc() {
        List<Users> users = userQuery.from();
        List<Users> result = userQuery.orderBy("age", "asc", users);
        
        assertNotNull(result);
        assertTrue(result.get(0).getAge() <= result.get(1).getAge());
    }

    @Test
    @DisplayName("ORDER BY - Deve ordenar por cidade descendente")
    void testOrderByCityDesc() {
        List<Users> users = userQuery.from();
        List<Users> result = userQuery.orderBy("city", "desc", users);
        
        assertNotNull(result);
        assertTrue(result.get(0).getCity().compareToIgnoreCase(result.get(1).getCity()) >= 0);
    }

    @Test
    @DisplayName("ORDER BY - Deve retornar lista original para coluna inválida")
    void testOrderByColunaInvalida() {
        List<Users> users = userQuery.from();
        List<Users> result = userQuery.orderBy("invalid", "asc", users);
        
        assertNotNull(result);
        assertEquals(users.size(), result.size());
    }

    // ==================== TESTES GROUP BY ====================

    @Test
    @DisplayName("GROUP BY - Deve agrupar por cidade")
    void testGroupByCity() {
        List<Users> users = userQuery.from();
        Map<String, List<Users>> result = userQuery.groupBy("city", users);
        
        assertNotNull(result);
        assertTrue(result.size() > 0);
        assertTrue(result.containsKey("São Paulo"));
    }

    @Test
    @DisplayName("GROUP BY - Deve agrupar por idade")
    void testGroupByAge() {
        List<Users> users = userQuery.from();
        Map<String, List<Users>> result = userQuery.groupBy("age", users);
        
        assertNotNull(result);
        assertTrue(result.size() > 0);
    }

    @Test
    @DisplayName("GROUP BY - Deve agrupar por nome")
    void testGroupByName() {
        List<Users> users = userQuery.from();
        Map<String, List<Users>> result = userQuery.groupBy("name", users);
        
        assertNotNull(result);
        assertEquals(30, result.size()); // Todos os nomes são únicos
    }

    @Test
    @DisplayName("GROUP BY - Deve retornar 'undefined' para coluna inválida")
    void testGroupByColunaInvalida() {
        List<Users> users = userQuery.from();
        Map<String, List<Users>> result = userQuery.groupBy("invalid", users);
        
        assertNotNull(result);
        assertTrue(result.containsKey("undefined"));
    }

    // ==================== TESTES INSERT ====================

    @Test
    @DisplayName("INSERT - Deve inserir novo usuário corretamente")
    void testInsert() {
        int initialSize = userQuery.from().size();
        int newId = userQuery.insert("Teste", 25, "Cidade Teste");
        
        assertEquals(initialSize + 1, newId);
        
        List<Users> users = userQuery.from();
        assertEquals(initialSize + 1, users.size());
        
        Users inserted = users.stream()
                .filter(u -> u.getId() == newId)
                .findFirst()
                .orElse(null);
        
        assertNotNull(inserted);
        assertEquals("Teste", inserted.getName());
        assertEquals(25, inserted.getAge());
        assertEquals("Cidade Teste", inserted.getCity());
    }

    @Test
    @DisplayName("INSERT - Deve inserir múltiplos usuários sequencialmente")
    void testMultipleInserts() {
        int id1 = userQuery.insert("Usuario1", 30, "Cidade1");
        int id2 = userQuery.insert("Usuario2", 35, "Cidade2");
        
        assertEquals(id1 + 1, id2);
        assertEquals(32, userQuery.from().size());
    }

    // ==================== TESTES DELETE ====================

    @Test
    @DisplayName("DELETE - Deve deletar usuário que atende a condição")
    void testDelete() {
        int initialSize = userQuery.from().size();
        int deleted = userQuery.delete(u -> u.getId() == 1);
        
        assertEquals(1, deleted);
        assertEquals(initialSize - 1, userQuery.from().size());
        
        List<Users> users = userQuery.from();
        assertTrue(users.stream().noneMatch(u -> u.getId() == 1));
    }

    @Test
    @DisplayName("DELETE - Deve deletar múltiplos usuários")
    void testDeleteMultiple() {
        int deleted = userQuery.delete(u -> u.getAge() > 40);
        
        assertTrue(deleted > 0);
        assertTrue(userQuery.from().stream().allMatch(u -> u.getAge() <= 40));
    }

    @Test
    @DisplayName("DELETE - Deve retornar 0 quando nenhum usuário atende a condição")
    void testDeleteNenhumResultado() {
        int deleted = userQuery.delete(u -> u.getId() == 999);
        
        assertEquals(0, deleted);
    }

    // ==================== TESTES UPDATE ====================

    @Test
    @DisplayName("UPDATE - Deve atualizar nome do usuário")
    void testUpdateName() {
        Map<String, Object> values = new HashMap<>();
        values.put("name", "Nome Atualizado");
        
        int updated = userQuery.update(values, u -> u.getId() == 1);
        
        assertEquals(1, updated);
        
        Users user = userQuery.from().stream()
                .filter(u -> u.getId() == 1)
                .findFirst()
                .orElse(null);
        
        assertNotNull(user);
        assertEquals("Nome Atualizado", user.getName());
    }

    @Test
    @DisplayName("UPDATE - Deve atualizar idade do usuário")
    void testUpdateAge() {
        Map<String, Object> values = new HashMap<>();
        values.put("age", 99);
        
        int updated = userQuery.update(values, u -> u.getId() == 1);
        
        assertEquals(1, updated);
        
        Users user = userQuery.from().stream()
                .filter(u -> u.getId() == 1)
                .findFirst()
                .orElse(null);
        
        assertNotNull(user);
        assertEquals(99, user.getAge());
    }

    @Test
    @DisplayName("UPDATE - Deve atualizar cidade do usuário")
    void testUpdateCity() {
        Map<String, Object> values = new HashMap<>();
        values.put("city", "Nova Cidade");
        
        int updated = userQuery.update(values, u -> u.getId() == 1);
        
        assertEquals(1, updated);
        
        Users user = userQuery.from().stream()
                .filter(u -> u.getId() == 1)
                .findFirst()
                .orElse(null);
        
        assertNotNull(user);
        assertEquals("Nova Cidade", user.getCity());
    }

    @Test
    @DisplayName("UPDATE - Deve atualizar múltiplos campos simultaneamente")
    void testUpdateMultiplosCampos() {
        Map<String, Object> values = new HashMap<>();
        values.put("name", "Novo Nome");
        values.put("age", 50);
        values.put("city", "Cidade Nova");
        
        int updated = userQuery.update(values, u -> u.getId() == 1);
        
        assertEquals(1, updated);
        
        Users user = userQuery.from().stream()
                .filter(u -> u.getId() == 1)
                .findFirst()
                .orElse(null);
        
        assertNotNull(user);
        assertEquals("Novo Nome", user.getName());
        assertEquals(50, user.getAge());
        assertEquals("Cidade Nova", user.getCity());
    }

    @Test
    @DisplayName("UPDATE - Deve atualizar múltiplos usuários")
    void testUpdateMultiplosUsuarios() {
        Map<String, Object> values = new HashMap<>();
        values.put("city", "Cidade Unificada");
        
        int updated = userQuery.update(values, u -> u.getAge() > 40);
        
        assertTrue(updated > 0);
        
        long count = userQuery.from().stream()
                .filter(u -> u.getAge() > 40)
                .filter(u -> u.getCity().equals("Cidade Unificada"))
                .count();
        
        assertEquals(updated, count);
    }

    @Test
    @DisplayName("UPDATE - Deve retornar 0 quando nenhum usuário atende a condição")
    void testUpdateNenhumResultado() {
        Map<String, Object> values = new HashMap<>();
        values.put("name", "Teste");
        
        int updated = userQuery.update(values, u -> u.getId() == 999);
        
        assertEquals(0, updated);
    }

    // ==================== TESTES LIKE ====================

    @Test
    @DisplayName("LIKE - Deve encontrar usuários por nome")
    void testLikeName() {
        Predicate<Users> predicate = userQuery.like("name", "ana");
        List<Users> result = userQuery.from(predicate);
        
        assertNotNull(result);
        assertTrue(result.size() > 0);
        assertTrue(result.stream().allMatch(u -> 
            u.getName().toLowerCase().contains("ana")));
    }

    @Test
    @DisplayName("LIKE - Deve ser case insensitive")
    void testLikeCaseInsensitive() {
        Predicate<Users> predicate1 = userQuery.like("name", "ANA");
        Predicate<Users> predicate2 = userQuery.like("name", "ana");
        
        List<Users> result1 = userQuery.from(predicate1);
        List<Users> result2 = userQuery.from(predicate2);
        
        assertEquals(result1.size(), result2.size());
    }

    @Test
    @DisplayName("LIKE - Deve encontrar usuários por cidade")
    void testLikeCity() {
        Predicate<Users> predicate = userQuery.like("city", "são");
        List<Users> result = userQuery.from(predicate);
        
        assertNotNull(result);
        assertTrue(result.size() > 0);
        assertTrue(result.stream().allMatch(u -> 
            u.getCity().toLowerCase().contains("são")));
    }

    @Test
    @DisplayName("LIKE - Deve retornar false para campo inválido")
    void testLikeCampoInvalido() {
        Predicate<Users> predicate = userQuery.like("invalid", "test");
        List<Users> result = userQuery.from(predicate);
        
        assertEquals(0, result.size());
    }

    // ==================== TESTES BETWEEN ====================

    @Test
    @DisplayName("BETWEEN - Deve encontrar usuários dentro do intervalo de idade")
    void testBetween() {
        Predicate<Users> predicate = userQuery.between("age", 25, 30);
        List<Users> result = userQuery.from(predicate);
        
        assertNotNull(result);
        assertTrue(result.size() > 0);
        assertTrue(result.stream().allMatch(u -> 
            u.getAge() >= 25 && u.getAge() <= 30));
    }

    @Test
    @DisplayName("BETWEEN - Deve incluir valores nos limites")
    void testBetweenInclusivo() {
        Predicate<Users> predicate = userQuery.between("age", 22, 22);
        List<Users> result = userQuery.from(predicate);
        
        assertTrue(result.size() > 0);
        assertTrue(result.stream().allMatch(u -> u.getAge() == 22));
    }

    @Test
    @DisplayName("BETWEEN - Deve retornar false para campo diferente de age")
    void testBetweenCampoInvalido() {
        Predicate<Users> predicate = userQuery.between("name", 25, 30);
        List<Users> result = userQuery.from(predicate);
        
        assertEquals(0, result.size());
    }

    // ==================== TESTES EQUALS ====================

    @Test
    @DisplayName("EQUALS - Deve encontrar usuário por ID (String)")
    void testEqualsIdString() {
        Predicate<Users> predicate = userQuery.equals("id", "1");
        List<Users> result = userQuery.from(predicate);
        
        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getId());
    }

    @Test
    @DisplayName("EQUALS - Deve encontrar usuário por ID (int)")
    void testEqualsIdInt() {
        Predicate<Users> predicate = userQuery.equals("id", 1);
        List<Users> result = userQuery.from(predicate);
        
        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getId());
    }

    @Test
    @DisplayName("EQUALS - Deve encontrar usuário por nome")
    void testEqualsName() {
        Predicate<Users> predicate = userQuery.equals("name", "Ana");
        List<Users> result = userQuery.from(predicate);
        
        assertTrue(result.size() > 0);
        assertTrue(result.stream().allMatch(u -> 
            u.getName().equalsIgnoreCase("Ana")));
    }

    @Test
    @DisplayName("EQUALS - Deve ser case insensitive para nome")
    void testEqualsNameCaseInsensitive() {
        Predicate<Users> predicate = userQuery.equals("name", "ana");
        List<Users> result = userQuery.from(predicate);
        
        assertTrue(result.size() > 0);
    }

    @Test
    @DisplayName("EQUALS - Deve encontrar usuários por cidade")
    void testEqualsCity() {
        Predicate<Users> predicate = userQuery.equals("city", "São Paulo");
        List<Users> result = userQuery.from(predicate);
        
        assertTrue(result.size() > 0);
        assertTrue(result.stream().allMatch(u -> 
            u.getCity().equalsIgnoreCase("São Paulo")));
    }

    @Test
    @DisplayName("EQUALS - Deve encontrar usuário por idade")
    void testEqualsAge() {
        Predicate<Users> predicate = userQuery.equals("age", "22");
        List<Users> result = userQuery.from(predicate);
        
        assertTrue(result.size() > 0);
        assertTrue(result.stream().allMatch(u -> u.getAge() == 22));
    }

    @Test
    @DisplayName("EQUALS - Deve retornar false para campo inválido")
    void testEqualsCampoInvalido() {
        Predicate<Users> predicate = userQuery.equals("invalid", "test");
        List<Users> result = userQuery.from(predicate);
        
        assertEquals(0, result.size());
    }

    @Test
    @DisplayName("EQUALS - Deve retornar false para ID inválido (int)")
    void testEqualsIdInvalidoInt() {
        Predicate<Users> predicate = userQuery.equals("name", 1);
        List<Users> result = userQuery.from(predicate);
        
        assertEquals(0, result.size());
    }
}
