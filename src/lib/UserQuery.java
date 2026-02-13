package lib;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class UserQuery {

    private List<Users> database;

    public UserQuery() {
        this.database = UsersDatabase.getAllUsers();
    }

    //FROM
    public List<Users> from(Predicate<Users> condition) {
        if (condition == null)
            return new ArrayList<>(database);

        return database.stream()
                .filter(condition)
                .collect(Collectors.toList());
    }

    public List<Users> from() {
        return from(null);
    }

    // SELECT
    public List<Map<String, Object>> select(String columns, List<Users> result) {

        if (columns.equalsIgnoreCase("count")) {
            Map<String, Object> map = new HashMap<>();
            map.put("count", result.size());
            return List.of(map);
        }

        String[] cols = columns.equals("*")
                ? new String[]{"id","name","age","city"}
                : columns.split(",");

        List<Map<String, Object>> output = new ArrayList<>();

        for (Users u : result) {

            Map<String, Object> row = new LinkedHashMap<>();

            for (String c : cols) {

                c = c.trim().toLowerCase();

                switch (c) {
                    case "id" -> row.put("id", u.getId());
                    case "name" -> row.put("name", u.getName());
                    case "age" -> row.put("age", u.getAge());
                    case "city" -> row.put("city", u.getCity());
                }
            }
            output.add(row);
        }
        return output;
    }

    // ORDER BY
    public List<Users> orderBy(String column, String order, List<Users> result) {

        Comparator<Users> comparator = switch (column.toLowerCase()) {
            case "id" -> Comparator.comparing(Users::getId);
            case "name" -> Comparator.comparing(Users::getName);
            case "age" -> Comparator.comparing(Users::getAge);
            case "city" -> Comparator.comparing(Users::getCity);
            default -> null;
        };

        if (comparator == null)
            return result;

        if (order.equalsIgnoreCase("desc"))
            comparator = comparator.reversed();

        return result.stream().sorted(comparator).toList();
    }


    // GROUP BY
    public Map<String, List<Users>> groupBy(String column, List<Users> result) {

        return result.stream().collect(Collectors.groupingBy(user -> {

            return switch (column.toLowerCase()) {
                case "id" -> String.valueOf(user.getId());
                case "name" -> user.getName();
                case "age" -> String.valueOf(user.getAge());
                case "city" -> user.getCity();
                default -> "undefined";
            };
        }));
    }

    // INSERT
    public int insert(String name, int age, String city) {
        int id = database.size() + 1;
        database.add(new Users(id, name, age, city));
        return id;
    }

    // DELETE
    public int delete(Predicate<Users> condition) {
        int before = database.size();
        database.removeIf(condition);
        return before - database.size();
    }

    // UPDATE
    public int update(Map<String,Object> values,
                      Predicate<Users> condition) {

        int count = 0;

        for (Users u : database) {

            if (!condition.test(u))
                continue;

            for (var e : values.entrySet()) {

                switch (e.getKey().toLowerCase()) {
                    case "name" -> u.setName((String)e.getValue());
                    case "age" -> u.setAge((Integer)e.getValue());
                    case "city" -> u.setCity((String)e.getValue());
                }
            }
            count++;
        }
        return count;
    }

    // PREDICATES
    public Predicate<Users> like(String field, String pattern) {
        return u -> switch (field.toLowerCase()) {
            case "name" -> u.getName().toLowerCase().contains(pattern.toLowerCase());
            case "city" -> u.getCity().toLowerCase().contains(pattern.toLowerCase());
            default -> false;
        };
    }

    public Predicate<Users> between(String field, int min, int max) {
        return u -> field.equalsIgnoreCase("age")
                && u.getAge() >= min
                && u.getAge() <= max;
    }

    public Predicate<Users> equals(String field, String value) {
        return u -> switch (field.toLowerCase()) {
            case "id" -> u.getId() == Integer.parseInt(value);
            case "name" -> u.getName().equalsIgnoreCase(value);
            case "city" -> u.getCity().equalsIgnoreCase(value);
            case "age" -> u.getAge() == Integer.parseInt(value);
            default -> false;
        };
    }
}
