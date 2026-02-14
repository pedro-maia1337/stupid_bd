package lib;

import java.util.*;
import java.util.function.Predicate;

public class UserQueryParser {

    private UserQuery engine;

    public UserQueryParser() {
        engine = new UserQuery();
    }

    public String execute(String sql) {

        sql = sql.trim();

        if (sql.toUpperCase().startsWith("SELECT"))
            return parseSelect(sql);

        if (sql.toUpperCase().startsWith("INSERT"))
            return parseInsert(sql);

        if (sql.toUpperCase().startsWith("DELETE"))
            return parseDelete(sql);

        if (sql.toUpperCase().startsWith("UPDATE"))
            return parseUpdate(sql);

        return "Comando inválido.";
    }

    // Parse SELECT
    private String parseSelect(String sql) {

        String[] parts = sql.split("(?i)FROM");
        String columns = parts[0].replaceFirst("(?i)SELECT","").trim();

        List<Users> result = engine.from();

        if (sql.toUpperCase().contains("WHERE")) {

            String where = sql.split("(?i)WHERE")[1];

            if (where.toUpperCase().contains("LIKE")) {
                String[] w = where.split("(?i)LIKE");
                result = engine.from(engine.like(
                        w[0].trim(),
                        w[1].replace("'","").trim()));
            }

            if (where.toUpperCase().contains("BETWEEN")) {
                String[] w = where.split("(?i)BETWEEN");
                String field = w[0].trim();
                String[] range = w[1].split("(?i)AND");

                result = engine.from(
                        engine.between(field,
                                Integer.parseInt(range[0].trim()),
                                Integer.parseInt(range[1].trim())));
            }
        }

        if (sql.toUpperCase().contains("GROUP BY")) {
            String column = sql.split("(?i)GROUP BY")[1].trim();
            var grouped = engine.groupBy(column, result);
            return grouped.toString();
        }

        return engine.select(columns, result).toString();
    }

    // Parse INSERT
    private String parseInsert(String sql) {

        String values = sql.split("(?i)VALUES")[1]
                .replace("(","")
                .replace(")","");

        String[] v = values.split(",");

        int id = engine.insert(
                v[0].replace("'","").trim(),
                Integer.parseInt(v[1].trim()),
                v[2].replace("'","").trim());

        return "Inserted id=" + id;
    }

    // Parse DELETE
    private String parseDelete(String sql) {

        if (!sql.toUpperCase().contains("WHERE")) {
            return "Invalid DELETE syntax";
        }

        String[] parts = sql.split("(?i)WHERE");
        if (parts.length != 2) {
            return "Invalid DELETE syntax";
        }

        String where = parts[1].trim();

        if (!where.contains("=")) {
            return "Invalid DELETE syntax";
        }

        String[] w = where.split("=");
        if (w.length != 2) {
            return "Invalid DELETE syntax";
        }

        String field = w[0].trim();
        String value = w[1].trim();

        if (!field.equalsIgnoreCase("id")) {
            return "Invalid field in DELETE";
        }

        int id;
        try {
            id = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return "Invalid id value";
        }

        int removed = engine.delete(
                engine.equals("id", id)
        );

        return removed + " removed";
    }

    // Parse UPDATE
    private String parseUpdate(String sql) {

        String setPart = sql.split("(?i)SET")[1]
                .split("(?i)WHERE")[0];

        String wherePart = sql.split("(?i)WHERE")[1];

        Map<String, Object> values = new HashMap<>();

        for (String assign : setPart.split(",")) {

            String[] p = assign.split("=");

            String field = p[0].trim();
            String value = p[1].replace("'", "").trim();

            if (field.equalsIgnoreCase("id") || field.equalsIgnoreCase("age")) {
                try {
                    values.put(field, Integer.parseInt(value));
                } catch (NumberFormatException e) {
                    values.put(field, value);
                }
            } else {
                values.put(field, value);
            }
        }

        String[] w = wherePart.split("=");

        String whereField = w[0].trim();
        String whereValue = w[1].replace("'", "").trim();

        int updated;

        if (whereField.equalsIgnoreCase("id")) {

            int id = Integer.parseInt(whereValue);
            updated = engine.update(
                    values,
                    engine.equals("id", id)
            );

        } else {

            updated = engine.update(
                    values,
                    engine.equals(whereField, whereValue)
            );
        }

        return updated + " updated";
    }
}
