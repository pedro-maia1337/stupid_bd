package lib;

import java.util.*;

public class ResultFormatter {

    public static String toJson(List<Map<String, Object>> data) {

        if (data == null || data.isEmpty())
            return "[]";

        StringBuilder json = new StringBuilder();
        json.append("[\n");

        for (int i = 0; i < data.size(); i++) {

            Map<String, Object> row = data.get(i);
            json.append("  {");

            int j = 0;
            for (var entry : row.entrySet()) {

                json.append("\"")
                        .append(entry.getKey())
                        .append("\":");

                Object value = entry.getValue();

                if (value instanceof String)
                    json.append("\"").append(value).append("\"");
                else
                    json.append(value);

                if (++j < row.size())
                    json.append(",");
            }

            json.append("}");

            if (i < data.size() - 1)
                json.append(",");

            json.append("\n");
        }

        json.append("]");

        return json.toString();
    }
}
