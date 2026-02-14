package test;

import lib.UserQueryParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UserQueryParser - SQL Parsing Tests")
class UserQueryParserTest {

    private UserQueryParser parser;

    @BeforeEach
    void setUp() {
        parser = new UserQueryParser();
    }

    // ==================== TESTES SELECT ====================

    @Test
    @DisplayName("SELECT - must execute SELECT * FROM users.")
    void testSelectAll() {
        String result = parser.execute("SELECT * FROM users");
        
        assertNotNull(result);
        assertTrue(result.contains("id"));
        assertTrue(result.contains("name"));
        assertTrue(result.contains("age"));
        assertTrue(result.contains("city"));
    }

    @Test
    @DisplayName("SELECT - must execute SELECT * FROM users with specific columns.")
    void testSelectEspecifico() {
        String result = parser.execute("SELECT name, age FROM users");
        
        assertNotNull(result);
        assertTrue(result.contains("name"));
        assertTrue(result.contains("age"));
    }

    @Test
    @DisplayName("SELECT - must execute SELECT COUNT")
    void testSelectCount() {
        String result = parser.execute("SELECT count FROM users");
        
        assertNotNull(result);
        assertTrue(result.contains("count"));
        assertTrue(result.contains("30"));
    }

    @Test
    @DisplayName("SELECT - it must be case insensitive")
    void testSelectCaseInsensitive() {
        String result1 = parser.execute("SELECT * FROM users");
        String result2 = parser.execute("select * from users");
        String result3 = parser.execute("SeLeCt * FrOm users");
        
        assertNotNull(result1);
        assertNotNull(result2);
        assertNotNull(result3);
    }

    @Test
    @DisplayName("SELECT - must execute SELECT com WHERE LIKE")
    void testSelectWhereLike() {
        String result = parser.execute("SELECT * FROM users WHERE name LIKE 'Ana'");
        
        assertNotNull(result);
        assertTrue(result.contains("Ana"));
    }

    @Test
    @DisplayName("SELECT - must execute SELECT WHERE LIKE case insensitive")
    void testSelectWhereLikeCaseInsensitive() {
        String result1 = parser.execute("SELECT * FROM users WHERE name LIKE 'ana'");
        String result2 = parser.execute("SELECT * FROM users WHERE name like 'ANA'");
        
        assertNotNull(result1);
        assertNotNull(result2);
    }

    @Test
    @DisplayName("SELECT - must execute SELECT com WHERE LIKE em cidade")
    void testSelectWhereLikeCity() {
        String result = parser.execute("SELECT * FROM users WHERE city LIKE 'São'");
        
        assertNotNull(result);
        assertTrue(result.contains("São Paulo"));
    }

    @Test
    @DisplayName("SELECT - must execute SELECT com WHERE BETWEEN")
    void testSelectWhereBetween() {
        String result = parser.execute("SELECT * FROM users WHERE age BETWEEN 25 AND 30");
        
        assertNotNull(result);
        assertNotEquals("[]", result);
    }

    @Test
    @DisplayName("SELECT - must execute SELECT WHERE BETWEEN case insensitive")
    void testSelectWhereBetweenCaseInsensitive() {
        String result1 = parser.execute("SELECT * FROM users WHERE age BETWEEN 25 AND 30");
        String result2 = parser.execute("SELECT * FROM users WHERE age between 25 and 30");
        
        assertNotNull(result1);
        assertNotNull(result2);
    }

    @Test
    @DisplayName("SELECT - must execute SELECT com GROUP BY")
    void testSelectGroupBy() {
        String result = parser.execute("SELECT * FROM users GROUP BY city");
        
        assertNotNull(result);
        assertTrue(result.contains("São Paulo"));
        assertTrue(result.contains("Rio de Janeiro"));
    }

    @Test
    @DisplayName("SELECT - must execute SELECT GROUP BY case insensitive")
    void testSelectGroupByCaseInsensitive() {
        String result1 = parser.execute("SELECT * FROM users GROUP BY city");
        String result2 = parser.execute("SELECT * FROM users group by city");
        
        assertNotNull(result1);
        assertNotNull(result2);
    }

    @Test
    @DisplayName("SELECT - must execute SELECT GROUP BY with different columns.")
    void testSelectGroupByDiferentesColunas() {
        String resultCity = parser.execute("SELECT * FROM users GROUP BY city");
        String resultAge = parser.execute("SELECT * FROM users GROUP BY age");
        String resultName = parser.execute("SELECT * FROM users GROUP BY name");
        
        assertNotNull(resultCity);
        assertNotNull(resultAge);
        assertNotNull(resultName);
    }

    @Test
    @DisplayName("SELECT - must execute SELECT with extra spaces")
    void testSelectComEspacos() {
        String result = parser.execute("  SELECT   *   FROM   users  ");
        
        assertNotNull(result);
        assertTrue(result.contains("id"));
    }

    @Test
    @DisplayName("SELECT - must execute SELECT COUNT with WHERE")
    void testSelectCountComWhere() {
        String result = parser.execute("SELECT count FROM users WHERE age BETWEEN 25 AND 30");
        
        assertNotNull(result);
        assertTrue(result.contains("count"));
    }

    @Test
    @DisplayName("SELECT - must execute complex query WHERE e LIKE")
    void testSelectComplexoWhereLike() {
        String result = parser.execute("SELECT name, city FROM users WHERE city LIKE 'Rio'");
        
        assertNotNull(result);
        assertTrue(result.contains("Rio"));
    }

    // TESTS INSERT

    @Test
    @DisplayName("INSERT - Deve inserir novo usuário")
    void testInsert() {
        String result = parser.execute("INSERT INTO users VALUES ('Teste', 25, 'Cidade')");
        
        assertNotNull(result);
        assertTrue(result.contains("Inserted"));
        assertTrue(result.contains("id="));
    }

    @Test
    @DisplayName("INSERT - Deve ser case insensitive")
    void testInsertCaseInsensitive() {
        String result1 = parser.execute("INSERT INTO users VALUES ('Teste1', 25, 'Cidade1')");
        String result2 = parser.execute("insert into users values ('Teste2', 26, 'Cidade2')");
        
        assertNotNull(result1);
        assertNotNull(result2);
        assertTrue(result1.contains("Inserted"));
        assertTrue(result2.contains("Inserted"));
    }

    @Test
    @DisplayName("INSERT - Deve inserir com diferentes tipos de dados")
    void testInsertDiferentesTipos() {
        String result = parser.execute("INSERT INTO users VALUES ('Nome Com Espaços', 99, 'Cidade Nova')");
        
        assertNotNull(result);
        assertTrue(result.contains("Inserted"));
    }

    @Test
    @DisplayName("INSERT - Deve incrementar ID automaticamente")
    void testInsertIncrementoId() {
        String result1 = parser.execute("INSERT INTO users VALUES ('User1', 25, 'City1')");
        String result2 = parser.execute("INSERT INTO users VALUES ('User2', 26, 'City2')");
        
        assertTrue(result1.contains("id=31"));
        assertTrue(result2.contains("id=32"));
    }

    @Test
    @DisplayName("INSERT - Deve lidar com aspas simples nos valores")
    void testInsertComAspas() {
        String result = parser.execute("INSERT INTO users VALUES ('Nome', 30, 'Cidade')");
        
        assertNotNull(result);
        assertTrue(result.contains("Inserted"));
    }

    @Test
    @DisplayName("INSERT - Deve lidar com espaços extras")
    void testInsertComEspacos() {
        String result = parser.execute("  INSERT   INTO   users   VALUES  ( 'Nome' , 30 , 'Cidade' )  ");
        
        assertNotNull(result);
        assertTrue(result.contains("Inserted"));
    }

    //TESTS DELETE
    @Test
    @DisplayName("DELETE - Deve deletar usuário por ID")
    void testDelete() {
        String result = parser.execute("DELETE FROM users WHERE id=1");
        
        assertNotNull(result);
        assertTrue(result.contains("removed"));
        assertTrue(result.contains("1"));
    }

    @Test
    @DisplayName("DELETE - Deve ser case insensitive")
    void testDeleteCaseInsensitive() {
        parser.execute("INSERT INTO users VALUES ('Test1', 25, 'City1')");
        parser.execute("INSERT INTO users VALUES ('Test2', 26, 'City2')");
        
        String result1 = parser.execute("DELETE FROM users WHERE id=31");
        String result2 = parser.execute("delete from users where id=32");
        
        assertNotNull(result1);
        assertNotNull(result2);
        assertTrue(result1.contains("removed"));
        assertTrue(result2.contains("removed"));
    }

    @Test
    @DisplayName("DELETE - Deve retornar 0 removed para ID inexistente")
    void testDeleteIdInexistente() {
        String result = parser.execute("DELETE FROM users WHERE id=999");
        
        assertNotNull(result);
        assertTrue(result.contains("0 removed"));
    }

    @Test
    @DisplayName("DELETE - Deve retornar erro sem WHERE")
    void testDeleteSemWhere() {
        String result = parser.execute("DELETE FROM users");
        
        assertNotNull(result);
        assertTrue(result.contains("Invalid"));
    }

    @Test
    @DisplayName("DELETE - Deve retornar erro sem condição")
    void testDeleteSemCondicao() {
        String result = parser.execute("DELETE FROM users WHERE");
        
        assertNotNull(result);
        assertTrue(result.contains("Invalid"));
    }

    @Test
    @DisplayName("DELETE - Deve retornar erro com campo inválido")
    void testDeleteCampoInvalido() {
        String result = parser.execute("DELETE FROM users WHERE name='Test'");
        
        assertNotNull(result);
        assertTrue(result.contains("Invalid"));
    }

    @Test
    @DisplayName("DELETE - Deve retornar erro sem sinal de igualdade")
    void testDeleteSemIgualdade() {
        String result = parser.execute("DELETE FROM users WHERE id 1");
        
        assertNotNull(result);
        assertTrue(result.contains("Invalid"));
    }

    @Test
    @DisplayName("DELETE - Deve retornar erro com valor de ID inválido")
    void testDeleteIdInvalido() {
        String result = parser.execute("DELETE FROM users WHERE id=abc");
        
        assertNotNull(result);
        assertTrue(result.contains("Invalid"));
    }

    @Test
    @DisplayName("DELETE - Deve lidar com espaços extras")
    void testDeleteComEspacos() {
        parser.execute("INSERT INTO users VALUES ('Test', 25, 'City')");
        String result = parser.execute("  DELETE   FROM   users   WHERE   id = 31  ");
        
        assertNotNull(result);
        assertTrue(result.contains("removed"));
    }

    // ==================== TESTES UPDATE ====================

    @Test
    @DisplayName("UPDATE - Deve atualizar nome por ID")
    void testUpdateName() {
        String result = parser.execute("UPDATE users SET name='Novo Nome' WHERE id=1");
        
        assertNotNull(result);
        assertTrue(result.contains("updated"));
        assertTrue(result.contains("1"));
    }

    @Test
    @DisplayName("UPDATE - Deve atualizar idade por ID")
    void testUpdateAge() {
        String result = parser.execute("UPDATE users SET age=99 WHERE id=2");
        
        assertNotNull(result);
        assertTrue(result.contains("1 updated"));
    }

    @Test
    @DisplayName("UPDATE - Deve atualizar cidade por ID")
    void testUpdateCity() {
        String result = parser.execute("UPDATE users SET city='Nova Cidade' WHERE id=3");
        
        assertNotNull(result);
        assertTrue(result.contains("1 updated"));
    }

    @Test
    @DisplayName("UPDATE - Deve atualizar múltiplos campos")
    void testUpdateMultiplosCampos() {
        String result = parser.execute("UPDATE users SET name='Nome Novo', age=50, city='Cidade Nova' WHERE id=4");
        
        assertNotNull(result);
        assertTrue(result.contains("1 updated"));
    }

    @Test
    @DisplayName("UPDATE - Deve ser case insensitive")
    void testUpdateCaseInsensitive() {
        String result1 = parser.execute("UPDATE users SET name='Test1' WHERE id=5");
        String result2 = parser.execute("update users set name='Test2' where id=6");
        
        assertNotNull(result1);
        assertNotNull(result2);
        assertTrue(result1.contains("updated"));
        assertTrue(result2.contains("updated"));
    }

    @Test
    @DisplayName("UPDATE - Deve atualizar por campo diferente de ID")
    void testUpdatePorNome() {
        String result = parser.execute("UPDATE users SET age=100 WHERE name='Ana'");
        
        assertNotNull(result);
        assertTrue(result.contains("updated"));
    }

    @Test
    @DisplayName("UPDATE - Deve atualizar múltiplos registros")
    void testUpdateMultiplosRegistros() {
        String result = parser.execute("UPDATE users SET city='Cidade Única' WHERE city='São Paulo'");
        
        assertNotNull(result);
        assertTrue(result.contains("updated"));
        assertFalse(result.equals("0 updated"));
    }

    @Test
    @DisplayName("UPDATE - Deve retornar 0 updated para ID inexistente")
    void testUpdateIdInexistente() {
        String result = parser.execute("UPDATE users SET name='Test' WHERE id=999");
        
        assertNotNull(result);
        assertTrue(result.contains("0 updated"));
    }

    @Test
    @DisplayName("UPDATE - Deve lidar com espaços extras")
    void testUpdateComEspacos() {
        String result = parser.execute("  UPDATE   users   SET   name = 'Test'   WHERE   id = 7  ");
        
        assertNotNull(result);
        assertTrue(result.contains("updated"));
    }

    @Test
    @DisplayName("UPDATE - Deve atualizar ID (embora não seja recomendado)")
    void testUpdateId() {
        String result = parser.execute("UPDATE users SET id=999 WHERE id=8");
        
        assertNotNull(result);
        assertTrue(result.contains("updated"));
    }

    @Test
    @DisplayName("UPDATE - Deve lidar com valores numéricos sem aspas")
    void testUpdateValoresNumericos() {
        String result = parser.execute("UPDATE users SET age=50 WHERE id=9");
        
        assertNotNull(result);
        assertTrue(result.contains("updated"));
    }

    @Test
    @DisplayName("UPDATE - Deve lidar com valores string com aspas")
    void testUpdateValoresString() {
        String result = parser.execute("UPDATE users SET name='Novo', city='Nova' WHERE id=10");
        
        assertNotNull(result);
        assertTrue(result.contains("updated"));
    }

    // ==================== TESTES DE COMANDOS INVÁLIDOS ====================

    @Test
    @DisplayName("Comando Inválido - Deve retornar mensagem de erro")
    void testComandoInvalido() {
        String result = parser.execute("INVALID COMMAND");
        
        assertNotNull(result);
        assertTrue(result.contains("inválido") || result.contains("Comando"));
    }

    @Test
    @DisplayName("Comando Inválido - Deve rejeitar comando vazio")
    void testComandoVazio() {
        String result = parser.execute("");
        
        assertNotNull(result);
    }

    @Test
    @DisplayName("Comando Inválido - Deve rejeitar apenas espaços")
    void testApenasEspacos() {
        String result = parser.execute("   ");
        
        assertNotNull(result);
    }

    @Test
    @DisplayName("Comando Inválido - Deve rejeitar DROP TABLE")
    void testDropTable() {
        String result = parser.execute("DROP TABLE users");
        
        assertNotNull(result);
        assertTrue(result.contains("inválido") || result.contains("Comando"));
    }

    @Test
    @DisplayName("Comando Inválido - Deve rejeitar CREATE TABLE")
    void testCreateTable() {
        String result = parser.execute("CREATE TABLE test");
        
        assertNotNull(result);
        assertTrue(result.contains("inválido") || result.contains("Comando"));
    }

    // ==================== TESTES DE INTEGRAÇÃO ====================

    @Test
    @DisplayName("Integração - Deve inserir e depois consultar")
    void testInsertESelect() {
        String insertResult = parser.execute("INSERT INTO users VALUES ('Integration Test', 88, 'Test City')");
        assertTrue(insertResult.contains("Inserted"));
        
        String selectResult = parser.execute("SELECT * FROM users WHERE name LIKE 'Integration'");
        assertTrue(selectResult.contains("Integration Test"));
    }

    @Test
    @DisplayName("Integração - Deve inserir, atualizar e consultar")
    void testInsertUpdateSelect() {
        String insertResult = parser.execute("INSERT INTO users VALUES ('Test User', 25, 'Old City')");
        assertTrue(insertResult.contains("id=31"));
        
        String updateResult = parser.execute("UPDATE users SET city='New City' WHERE id=31");
        assertTrue(updateResult.contains("1 updated"));
        
        String selectResult = parser.execute("SELECT city FROM users WHERE city LIKE 'New City'");
        assertTrue(selectResult.contains("New City"));
    }

    @Test
    @DisplayName("Integração - Deve inserir e deletar")
    void testInsertEDelete() {
        String insertResult = parser.execute("INSERT INTO users VALUES ('Delete Me', 30, 'Any City')");
        assertTrue(insertResult.contains("id=31"));
        
        String deleteResult = parser.execute("DELETE FROM users WHERE id=31");
        assertTrue(deleteResult.contains("1 removed"));
        
        String selectResult = parser.execute("SELECT count FROM users WHERE name LIKE 'Delete Me'");
        assertTrue(selectResult.contains("count=0"));
    }

    @Test
    @DisplayName("Integração - Múltiplas operações sequenciais")
    void testMultiplasOperacoes() {
        // Insert
        parser.execute("INSERT INTO users VALUES ('User A', 20, 'City A')");
        parser.execute("INSERT INTO users VALUES ('User B', 21, 'City B')");
        parser.execute("INSERT INTO users VALUES ('User C', 22, 'City C')");
        
        // Update
        String updateResult = parser.execute("UPDATE users SET age=25 WHERE name='User A'");
        assertTrue(updateResult.contains("updated"));
        
        // Select
        String selectResult = parser.execute("SELECT count FROM users WHERE age BETWEEN 20 AND 25");
        assertNotNull(selectResult);
        
        // Delete
        String deleteResult = parser.execute("DELETE FROM users WHERE id=31");
        assertTrue(deleteResult.contains("removed"));
    }

    @Test
    @DisplayName("Integração - Validação de estado após operações")
    void testValidacaoEstado() {
        // Estado inicial
        String initialCount = parser.execute("SELECT count FROM users");
        assertTrue(initialCount.contains("30"));
        
        // Inserir 3 usuários
        parser.execute("INSERT INTO users VALUES ('New1', 25, 'City1')");
        parser.execute("INSERT INTO users VALUES ('New2', 26, 'City2')");
        parser.execute("INSERT INTO users VALUES ('New3', 27, 'City3')");
        
        // Verificar novo count
        String afterInsertCount = parser.execute("SELECT count FROM users");
        assertTrue(afterInsertCount.contains("33"));
        
        // Deletar 2 usuários
        parser.execute("DELETE FROM users WHERE id=31");
        parser.execute("DELETE FROM users WHERE id=32");
        
        // Verificar count final
        String finalCount = parser.execute("SELECT count FROM users");
        assertTrue(finalCount.contains("31"));
    }
}
