package lib.network;

import java.util.*;
import java.util.regex.*;

/**
 * Formata resultados SQL em tabelas ASCII art.
 *
 * @author SQL Parser Team
 * @version 1.0
 */
public class ResultFormatter {

    /**
     * Formata resultado em tabela ASCII.
     *
     * @param result Resultado bruto do parser
     * @return String formatada como tabela
     */
    public static String formatAsTable(String result) {
        if (result == null || result.trim().isEmpty()) {
            return "Empty result";
        }

        // Se for mensagem de erro ou status, retornar direto
        if (result.startsWith("Invalid") ||
                result.startsWith("ERROR") ||
                result.contains("updated") ||
                result.contains("removed") ||
                result.contains("Inserted")) {
            return result;
        }

        // Detectar tipo de resultado

        // Formato JSON: [{id=1, name=Ana...}]
        if (result.trim().startsWith("[{") && result.contains("id=")) {
            return formatJsonUserRows(result);
        }

        // Formato texto: id=1, name=Ana, age=22, city=SP
        if (result.contains("id=") && result.contains("name=")) {
            return formatUserRows(result);
        }

        if (result.contains("count=")) {
            return formatCountResult(result);
        }

        if (result.contains("=")) {
            return formatGenericKeyValue(result);
        }

        return result;
    }

    /**
     * Formata resultado JSON: [{id=1, name=Ana, age=22, city=SP}, {...}]
     */
    private static String formatJsonUserRows(String result) {
        List<Map<String, String>> rows = parseJsonUserRows(result);

        if (rows.isEmpty()) {
            return "No results";
        }

        return buildUserTable(rows);
    }

    /**
     * Parse resultado JSON em lista de maps.
     */
    private static List<Map<String, String>> parseJsonUserRows(String result) {
        List<Map<String, String>> rows = new ArrayList<>();

        // Remove [ e ]
        result = result.trim();
        if (result.startsWith("[")) result = result.substring(1);
        if (result.endsWith("]")) result = result.substring(0, result.length() - 1);

        // Split por }, { para pegar cada objeto
        String[] objects = result.split("\\},\\s*\\{");

        for (String obj : objects) {
            // Limpar { e }
            obj = obj.replace("{", "").replace("}", "").trim();
            if (obj.isEmpty()) continue;

            Map<String, String> row = new HashMap<>();

            // Parse: id=1, name=Ana, age=22, city=SP
            String[] parts = obj.split(",\\s*");

            for (String part : parts) {
                String[] kv = part.split("=", 2);
                if (kv.length == 2) {
                    row.put(kv[0].trim(), kv[1].trim());
                }
            }

            if (!row.isEmpty()) {
                rows.add(row);
            }
        }

        return rows;
    }

    /**
     * Formata linhas de usuários (id=1, name=Ana, age=22, city=SP).
     */
    private static String formatUserRows(String result) {
        List<Map<String, String>> rows = parseUserRows(result);

        if (rows.isEmpty()) {
            return "No results";
        }

        return buildUserTable(rows);
    }

    /**
     * Constrói tabela ASCII a partir de lista de rows.
     */
    private static String buildUserTable(List<Map<String, String>> rows) {
        if (rows.isEmpty()) {
            return "No results";
        }

        // Determinar larguras das colunas
        int idWidth = 4;
        int nameWidth = 4;
        int ageWidth = 3;
        int cityWidth = 4;

        for (Map<String, String> row : rows) {
            idWidth = Math.max(idWidth, row.getOrDefault("id", "").length());
            nameWidth = Math.max(nameWidth, row.getOrDefault("name", "").length());
            ageWidth = Math.max(ageWidth, row.getOrDefault("age", "").length());
            cityWidth = Math.max(cityWidth, row.getOrDefault("city", "").length());
        }

        // Construir tabela
        StringBuilder sb = new StringBuilder();

        // Linha superior
        sb.append("┌").append("─".repeat(idWidth + 2))
                .append("┬").append("─".repeat(nameWidth + 2))
                .append("┬").append("─".repeat(ageWidth + 2))
                .append("┬").append("─".repeat(cityWidth + 2))
                .append("┐\n");

        // Cabeçalho
        sb.append("│ ").append(pad("id", idWidth))
                .append(" │ ").append(pad("name", nameWidth))
                .append(" │ ").append(pad("age", ageWidth))
                .append(" │ ").append(pad("city", cityWidth))
                .append(" │\n");

        // Linha separadora
        sb.append("├").append("─".repeat(idWidth + 2))
                .append("┼").append("─".repeat(nameWidth + 2))
                .append("┼").append("─".repeat(ageWidth + 2))
                .append("┼").append("─".repeat(cityWidth + 2))
                .append("┤\n");

        // Dados
        for (Map<String, String> row : rows) {
            sb.append("│ ").append(pad(row.getOrDefault("id", ""), idWidth))
                    .append(" │ ").append(pad(row.getOrDefault("name", ""), nameWidth))
                    .append(" │ ").append(pad(row.getOrDefault("age", ""), ageWidth))
                    .append(" │ ").append(pad(row.getOrDefault("city", ""), cityWidth))
                    .append(" │\n");
        }

        // Linha inferior
        sb.append("└").append("─".repeat(idWidth + 2))
                .append("┴").append("─".repeat(nameWidth + 2))
                .append("┴").append("─".repeat(ageWidth + 2))
                .append("┴").append("─".repeat(cityWidth + 2))
                .append("┘\n");

        // Rodapé
        sb.append("(").append(rows.size()).append(rows.size() == 1 ? " row)" : " rows)");

        return sb.toString();
    }

    /**
     * Formata resultado de COUNT.
     */
    private static String formatCountResult(String result) {
        Pattern p = Pattern.compile("count=(\\d+)");
        Matcher m = p.matcher(result);

        if (m.find()) {
            String count = m.group(1);
            StringBuilder sb = new StringBuilder();

            sb.append("┌───────┐\n");
            sb.append("│ count │\n");
            sb.append("├───────┤\n");
            sb.append("│ ").append(pad(count, 5)).append(" │\n");
            sb.append("└───────┘");

            return sb.toString();
        }

        return result;
    }

    /**
     * Formata resultado genérico key=value.
     */
    private static String formatGenericKeyValue(String result) {
        String[] lines = result.split("\n");
        StringBuilder sb = new StringBuilder();

        for (String line : lines) {
            if (line.contains("=")) {
                sb.append(line).append("\n");
            }
        }

        return sb.toString().trim();
    }

    /**
     * Parse linhas de usuários.
     */
    private static List<Map<String, String>> parseUserRows(String result) {
        List<Map<String, String>> rows = new ArrayList<>();

        // Split por linhas
        String[] lines = result.split("\n");

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            Map<String, String> row = new HashMap<>();

            // Parse: id=1, name=Ana, age=22, city=SP
            String[] parts = line.split(",\\s*");

            for (String part : parts) {
                String[] kv = part.split("=", 2);
                if (kv.length == 2) {
                    row.put(kv[0].trim(), kv[1].trim());
                }
            }

            if (!row.isEmpty()) {
                rows.add(row);
            }
        }

        return rows;
    }

    /**
     * Pad string para largura especificada.
     */
    private static String pad(String str, int width) {
        if (str.length() >= width) {
            return str.substring(0, width);
        }
        return str + " ".repeat(width - str.length());
    }
}