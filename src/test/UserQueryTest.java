package test;

import lib.UserQuery;
import lib.Users;
import lib.UsersDatabase;

import org.junit.jupiter.api.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class UserQueryTest {

    private UserQuery query;

    @BeforeEach
    void setup() {
        UsersDatabase.clear();
        query = new UserQuery();

        query.insert("Pedro", 22, "SP");
        query.insert("Maria", 30, "RJ");
        query.insert("Joao", 18, "SP");
    }

    @Test
    void testInsert() {
        int id = query.insert("Ana", 25, "BH");
        assertEquals(4, id);
    }

    @Test
    void testSelectAll() {
        List<Map<String, Object>> result = query.select("*", query.from());
        assertEquals(3, result.size());
    }

    @Test
    void testWhereEquals() {
        List<Users> result = query.from(
                query.equals("city", "SP"));

        assertEquals(2, result.size());
    }

    @Test
    void testLike() {
        List<Users> result = query.from(
                query.like("name", "Pe"));

        assertEquals(1, result.size());
        assertEquals("Pedro", result.get(0).getName());
    }

    @Test
    void testBetween() {
        List<Users> result = query.from(
                query.between("age", 20, 30));

        assertEquals(2, result.size());
    }

    @Test
    void testDelete() {

        int removed = query.delete(
                query.equals("name", "Pedro"));

        assertEquals(1, removed);

        List<Users> result = query.from();
        assertEquals(2, result.size());
    }

    @Test
    void testUpdate() {

        Map<String, Object> values = new HashMap<>();
        values.put("city", "BH");

        int updated = query.update(values,
                query.equals("name", "Pedro"));

        assertEquals(1, updated);

        List<Users> result = query.from(
                query.equals("city", "BH"));

        assertEquals(1, result.size());
        assertEquals("Pedro", result.get(0).getName());
    }

    @Test
    void testGroupBy() {
        Map<String, List<Users>> grouped =
                query.groupBy("city", query.from());

        assertEquals(2, grouped.size());
        assertTrue(grouped.containsKey("SP"));
        assertTrue(grouped.containsKey("RJ"));
    }
}
