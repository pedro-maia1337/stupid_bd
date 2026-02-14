package lib;

import lib.parser.SQLiteSimpleBaseVisitor;
import lib.parser.SQLiteSimpleParser;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Visitor que converte a árvore sintática do ANTLR4 em chamadas para UserQuery.
 * Mantém a compatibilidade com a implementação existente.
 */
public class SQLVisitor extends SQLiteSimpleBaseVisitor<Object> {

    private UserQuery engine;

    public SQLVisitor(UserQuery engine) {
        this.engine = engine;
    }

    // ============================================================
    // ENTRY POINT - Delegar para sql_stmt
    // ============================================================

    @Override
    public Object visitParse(SQLiteSimpleParser.ParseContext ctx) {
        // Delegar para sql_stmt
        return visit(ctx.sql_stmt());
    }

    @Override
    public Object visitSql_stmt(SQLiteSimpleParser.Sql_stmtContext ctx) {
        // Delegar para o statement específico
        if (ctx.select_stmt() != null) {
            return visit(ctx.select_stmt());
        }
        if (ctx.insert_stmt() != null) {
            return visit(ctx.insert_stmt());
        }
        if (ctx.update_stmt() != null) {
            return visit(ctx.update_stmt());
        }
        if (ctx.delete_stmt() != null) {
            return visit(ctx.delete_stmt());
        }
        return "ERRO: Statement não reconhecido";
    }

    // ============================================================
    // SELECT STATEMENT
    // ============================================================

    @Override
    public Object visitSelect_stmt(SQLiteSimpleParser.Select_stmtContext ctx) {
        // Obter nome da tabela
        String tableName = ctx.table_name().getText();
        
        // Obter colunas a selecionar
        String columns = extractColumns(ctx.result_column());
        
        // Executar FROM
        List<Users> result = engine.from();
        
        // Aplicar WHERE se existir
        if (ctx.where_clause() != null) {
            Predicate<Users> predicate = buildPredicate(ctx.where_clause().expr());
            result = engine.from(predicate);
        }
        
        // Aplicar GROUP BY se existir
        if (ctx.group_by_clause() != null) {
            String groupColumn = ctx.group_by_clause().column_name().getText();
            Map<String, List<Users>> grouped = engine.groupBy(groupColumn, result);
            return grouped.toString();
        }
        
        // Aplicar ORDER BY se existir
        if (ctx.order_by_clause() != null) {
            String orderColumn = ctx.order_by_clause().column_name().getText();
            String direction = ctx.order_by_clause().K_DESC() != null ? "desc" : "asc";
            result = engine.orderBy(orderColumn, direction, result);
        }
        
        // Executar SELECT
        return engine.select(columns, result).toString();
    }

    /**
     * Extrai as colunas a serem selecionadas.
     */
    private String extractColumns(List<SQLiteSimpleParser.Result_columnContext> resultColumns) {
        for (SQLiteSimpleParser.Result_columnContext col : resultColumns) {
            if (col instanceof SQLiteSimpleParser.SelectAllContext) {
                return "*";
            }
            if (col instanceof SQLiteSimpleParser.SelectCountContext) {
                return "count";
            }
            if (col instanceof SQLiteSimpleParser.SelectColumnsContext) {
                SQLiteSimpleParser.SelectColumnsContext selectCols = 
                    (SQLiteSimpleParser.SelectColumnsContext) col;
                return selectCols.column_name().stream()
                    .map(ParseTree::getText)
                    .collect(Collectors.joining(","));
            }
        }
        return "*";
    }

    // ============================================================
    // INSERT STATEMENT
    // ============================================================

    @Override
    public Object visitInsert_stmt(SQLiteSimpleParser.Insert_stmtContext ctx) {
        // Extrair valores
        List<SQLiteSimpleParser.Literal_valueContext> values = ctx.literal_value();
        
        if (values.size() != 3) {
            return "ERRO: INSERT requer 3 valores (name, age, city)";
        }
        
        String name = extractStringValue(values.get(0));
        int age = extractIntValue(values.get(1));
        String city = extractStringValue(values.get(2));
        
        int id = engine.insert(name, age, city);
        
        return "Inserted id=" + id;
    }

    // ============================================================
    // UPDATE STATEMENT
    // ============================================================

    @Override
    public Object visitUpdate_stmt(SQLiteSimpleParser.Update_stmtContext ctx) {
        // Extrair assignments (SET)
        Map<String, Object> values = new HashMap<>();
        
        for (SQLiteSimpleParser.AssignmentContext assignment : ctx.assignment()) {
            String column = assignment.column_name().getText();
            
            // Validar campo
            if (!isValidField(column)) {
                throw new RuntimeException("Invalid: Field '" + column + "' is not valid");
            }
            
            Object value = extractValue(assignment.literal_value());
            values.put(column, value);
        }
        
        // Construir predicado WHERE
        Predicate<Users> predicate = buildPredicate(ctx.where_clause().expr());
        
        // Executar UPDATE
        int updated = engine.update(values, predicate);
        
        return updated + " updated";
    }

    // ============================================================
    // DELETE STATEMENT
    // ============================================================

    @Override
    public Object visitDelete_stmt(SQLiteSimpleParser.Delete_stmtContext ctx) {
        // Validar que DELETE só pode usar 'id' no WHERE
        validateDeleteCondition(ctx.where_clause().expr());
        
        // Construir predicado WHERE
        Predicate<Users> predicate = buildPredicate(ctx.where_clause().expr());
        
        // Executar DELETE
        int deleted = engine.delete(predicate);
        
        return deleted + " removed";
    }
    
    /**
     * Valida que a condição DELETE usa apenas o campo 'id'.
     * DELETE só pode ser executado por ID.
     */
    private void validateDeleteCondition(SQLiteSimpleParser.ExprContext ctx) {
        if (ctx == null) {
            throw new RuntimeException("Invalid: DELETE requires WHERE clause with id");
        }
        
        // EQUALS: field = value
        if (ctx instanceof SQLiteSimpleParser.EqualsExprContext) {
            SQLiteSimpleParser.EqualsExprContext eqCtx = 
                (SQLiteSimpleParser.EqualsExprContext) ctx;
            
            String field = eqCtx.column_name().getText();
            
            // DELETE só aceita 'id'
            if (!field.equalsIgnoreCase("id")) {
                throw new RuntimeException("Invalid: DELETE can only use 'id' field in WHERE clause");
            }
            return;
        }
        
        // AND/OR: validar recursivamente ambos os lados
        if (ctx instanceof SQLiteSimpleParser.AndExprContext) {
            SQLiteSimpleParser.AndExprContext andCtx = 
                (SQLiteSimpleParser.AndExprContext) ctx;
            validateDeleteCondition(andCtx.expr(0));
            validateDeleteCondition(andCtx.expr(1));
            return;
        }
        
        if (ctx instanceof SQLiteSimpleParser.OrExprContext) {
            SQLiteSimpleParser.OrExprContext orCtx = 
                (SQLiteSimpleParser.OrExprContext) ctx;
            validateDeleteCondition(orCtx.expr(0));
            validateDeleteCondition(orCtx.expr(1));
            return;
        }
        
        // PAREN: validar expressão interna
        if (ctx instanceof SQLiteSimpleParser.ParenExprContext) {
            SQLiteSimpleParser.ParenExprContext parenCtx = 
                (SQLiteSimpleParser.ParenExprContext) ctx;
            validateDeleteCondition(parenCtx.expr());
            return;
        }
        
        // LIKE, BETWEEN ou outros não são permitidos em DELETE
        throw new RuntimeException("Invalid: DELETE only supports simple id comparison");
    }

    // ============================================================
    // PREDICATES (WHERE CONDITIONS)
    // ============================================================

    /**
     * Valida se um campo é permitido na tabela users.
     * Campos válidos: id, name, age, city
     */
    private boolean isValidField(String field) {
        if (field == null) {
            return false;
        }
        String normalized = field.toLowerCase();
        return normalized.equals("id") || 
               normalized.equals("name") || 
               normalized.equals("age") || 
               normalized.equals("city");
    }

    /**
     * Constrói um Predicate a partir de uma expressão WHERE.
     */
    private Predicate<Users> buildPredicate(SQLiteSimpleParser.ExprContext ctx) {
        if (ctx == null) {
            throw new RuntimeException("Invalid: WHERE clause requires a condition");
        }
        
        // EQUALS: field = value
        if (ctx instanceof SQLiteSimpleParser.EqualsExprContext) {
            SQLiteSimpleParser.EqualsExprContext eqCtx = 
                (SQLiteSimpleParser.EqualsExprContext) ctx;
            
            String field = eqCtx.column_name().getText();
            
            // Validar campo
            if (!isValidField(field)) {
                throw new RuntimeException("Invalid: Field '" + field + "' is not valid");
            }
            
            Object value = extractValue(eqCtx.literal_value());
            
            // Validar tipo do valor para o campo 'id'
            if (field.equalsIgnoreCase("id")) {
                if (!(value instanceof Integer)) {
                    throw new RuntimeException("Invalid: Field 'id' requires an integer value");
                }
                return engine.equals(field, (Integer) value);
            }
            
            // Se for inteiro, usar sobrecarga específica
            if (value instanceof Integer) {
                return engine.equals(field, (Integer) value);
            }
            
            return engine.equals(field, value.toString());
        }
        
        // LIKE: field LIKE 'pattern'
        if (ctx instanceof SQLiteSimpleParser.LikeExprContext) {
            SQLiteSimpleParser.LikeExprContext likeCtx = 
                (SQLiteSimpleParser.LikeExprContext) ctx;
            
            String field = likeCtx.column_name().getText();
            
            // Validar campo
            if (!isValidField(field)) {
                throw new RuntimeException("Invalid: Field '" + field + "' is not valid");
            }
            
            String pattern = extractStringValue(likeCtx.literal_value());
            
            return engine.like(field, pattern);
        }
        
        // BETWEEN: field BETWEEN min AND max
        if (ctx instanceof SQLiteSimpleParser.BetweenExprContext) {
            SQLiteSimpleParser.BetweenExprContext betweenCtx = 
                (SQLiteSimpleParser.BetweenExprContext) ctx;
            
            String field = betweenCtx.column_name().getText();
            
            // Validar campo
            if (!isValidField(field)) {
                throw new RuntimeException("Invalid: Field '" + field + "' is not valid");
            }
            
            int min = extractIntValue(betweenCtx.literal_value(0));
            int max = extractIntValue(betweenCtx.literal_value(1));
            
            return engine.between(field, min, max);
        }
        
        // AND: expr AND expr
        if (ctx instanceof SQLiteSimpleParser.AndExprContext) {
            SQLiteSimpleParser.AndExprContext andCtx = 
                (SQLiteSimpleParser.AndExprContext) ctx;
            
            Predicate<Users> left = buildPredicate(andCtx.expr(0));
            Predicate<Users> right = buildPredicate(andCtx.expr(1));
            
            return left.and(right);
        }
        
        // OR: expr OR expr
        if (ctx instanceof SQLiteSimpleParser.OrExprContext) {
            SQLiteSimpleParser.OrExprContext orCtx = 
                (SQLiteSimpleParser.OrExprContext) ctx;
            
            Predicate<Users> left = buildPredicate(orCtx.expr(0));
            Predicate<Users> right = buildPredicate(orCtx.expr(1));
            
            return left.or(right);
        }
        
        // PAREN: (expr)
        if (ctx instanceof SQLiteSimpleParser.ParenExprContext) {
            SQLiteSimpleParser.ParenExprContext parenCtx = 
                (SQLiteSimpleParser.ParenExprContext) ctx;
            
            return buildPredicate(parenCtx.expr());
        }
        
        // Default: sempre true (não deveria chegar aqui)
        return user -> true;
    }

    // ============================================================
    // HELPER METHODS
    // ============================================================

    /**
     * Extrai valor de um literal (String ou Integer).
     */
    private Object extractValue(SQLiteSimpleParser.Literal_valueContext ctx) {
        String text = ctx.getText();
        
        // String literal (entre aspas simples)
        if (text.startsWith("'")) {
            return text.substring(1, text.length() - 1).replace("''", "'");
        }
        
        // Numeric literal
        try {
            if (text.contains(".")) {
                return Double.parseDouble(text);
            }
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            // Se não é número e não tem aspas, é um identificador inválido
            throw new RuntimeException("Invalid: Value '" + text + "' is not a valid literal");
        }
    }

    /**
     * Extrai valor String de um literal.
     */
    private String extractStringValue(SQLiteSimpleParser.Literal_valueContext ctx) {
        Object value = extractValue(ctx);
        
        // Se for string entre aspas, já está tratado
        if (value instanceof String) {
            return (String) value;
        }
        
        return value.toString();
    }

    /**
     * Extrai valor Integer de um literal.
     */
    private int extractIntValue(SQLiteSimpleParser.Literal_valueContext ctx) {
        Object value = extractValue(ctx);
        
        if (value instanceof Integer) {
            return (Integer) value;
        }
        
        if (value instanceof Double) {
            return ((Double) value).intValue();
        }
        
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            throw new RuntimeException("Valor não é um número inteiro: " + value);
        }
    }
}
