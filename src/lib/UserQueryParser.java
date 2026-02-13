package lib;

import java.util.*;

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

        String where = sql.split("(?i)WHERE")[1];
        String[] w = where.split("=");

        int removed = engine.delete(
                engine.equals(w[0].trim(), w[1].trim()));

        return removed + " removed";
    }

    // Parse UPDATE
    private String parseUpdate(String sql) {

        String setPart = sql.split("(?i)SET")[1]
                .split("(?i)WHERE")[0];

        String wherePart = sql.split("(?i)WHERE")[1];

        Map<String,Object> values = new HashMap<>();

        for (String assign : setPart.split(",")) {
            String[] p = assign.split("=");
            values.put(p[0].trim(),
                    p[1].replace("'","").trim());
        }

        String[] w = wherePart.split("=");

        int updated = engine.update(values,
                engine.equals(w[0].trim(), w[1].trim()));

        return updated + " updated";
    }
}
