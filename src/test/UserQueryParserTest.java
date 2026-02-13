package test;

import lib.UserQueryParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserQueryParserTest {

    private UserQueryParser parser;

    @BeforeEach
    void setup() {
        parser = new UserQueryParser();
    }

    @Test
    void testSelectQuery() {
        String result = parser.execute("SELECT * FROM users");

        assertNotNull(result);
        assertTrue(result.contains("name=Ana"));
        assertTrue(result.contains("city=São Paulo"));
    }

    @Test
    void testInsertQuery() {
        String result = parser.execute(
                "INSERT INTO users (name, age, city) VALUES ('Teste', 20, 'SP')"
        );

        assertNotNull(result);

        String select = parser.execute("SELECT * FROM users");
        assertTrue(select.contains("name=Teste"));
    }

    @Test
    void testUpdateQuery() {
        parser.execute(
                "UPDATE users SET city='Campinas' WHERE id=1"
        );

        String result = parser.execute(
                "SELECT * FROM users WHERE id=1"
        );

        assertTrue(result.contains("city=Campinas"));
    }

    @Test
    void testDeleteQuery() {
        parser.execute("DELETE FROM users WHERE id=1");

        String result = parser.execute(
                "SELECT * FROM users WHERE id=1"
        );

        assertFalse(result.contains("id=1"));
    }
}
