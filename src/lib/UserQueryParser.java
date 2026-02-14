package lib;

import lib.parser.SQLiteSimpleLexer;
import lib.parser.SQLiteSimpleParser;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;

/**
 * Parser de queries SQL usando ANTLR4.
 * Substitui o parser manual anterior mantendo a mesma interface.
 */
public class UserQueryParser {

    private UserQuery engine;

    public UserQueryParser() {
        engine = new UserQuery();
    }

    /**
     * Executa uma query SQL usando o parser ANTLR4.
     */
    public String execute(String sql) {
        try {
            // Validar entrada vazia
            if (sql == null || sql.trim().isEmpty()) {
                return "Comando inválido: Empty query";
            }
            
            // Criar lexer a partir da string SQL
            SQLiteSimpleLexer lexer = new SQLiteSimpleLexer(CharStreams.fromString(sql));
            
            // Criar stream de tokens
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            
            // Criar parser
            SQLiteSimpleParser parser = new SQLiteSimpleParser(tokens);
            
            // Remover error listeners padrão
            parser.removeErrorListeners();
            
            // Flag para capturar erro de sintaxe
            final boolean[] hasError = {false};
            
            // Adicionar custom error listener
            parser.addErrorListener(new BaseErrorListener() {
                @Override
                public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                                      int line, int charPositionInLine, 
                                      String msg, RecognitionException e) {
                    hasError[0] = true;
                    throw new RuntimeException("Comando inválido: SQL syntax error");
                }
            });
            
            // Parse da query (ponto de entrada da gramática)
            ParseTree tree = parser.parse();
            
            // Se houve erro, retornar
            if (hasError[0]) {
                return "Comando inválido: Syntax error";
            }
            
            // Criar visitor e executar
            SQLVisitor visitor = new SQLVisitor(engine);
            Object result = visitor.visit(tree);
            
            // Se o visitor retornou uma string com erro, processar
            if (result != null) {
                String resultStr = result.toString();
                
                // Se começa com "ERRO:", transformar em "Invalid"
                if (resultStr.startsWith("ERRO:")) {
                    return "Invalid: " + resultStr.substring(6);
                }
                
                // Se já é "Invalid", manter
                if (resultStr.startsWith("Invalid")) {
                    return resultStr;
                }
                
                // Resultado normal
                return resultStr;
            }
            
            // Retornar OK se não há resultado
            return "OK";
            
        } catch (RuntimeException e) {
            // Erro de sintaxe ou validação
            String msg = e.getMessage();
            
            // Se a mensagem já contém "Invalid" ou "Comando inválido", retornar direto
            if (msg != null && (msg.contains("Invalid") || msg.contains("Comando inválido"))) {
                return msg;
            }
            
            // Caso contrário, adicionar prefixo apropriado
            return "Invalid: " + (msg != null ? msg : "Syntax error");
            
        } catch (Exception e) {
            // Outros erros
            return "Comando inválido: " + e.getMessage();
        }
    }
}
