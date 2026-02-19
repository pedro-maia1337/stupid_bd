package lib;

import lib.parser.SQLiteSimpleLexer;
import lib.parser.SQLiteSimpleParser;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;

/**
 * Parser de queries SQL usando ANTLR4.
 * Substitui o parser manual anterior mantendo a mesma interface.
 *
 * @author SQL Parser Team
 * @version 3.0
 */
public class UserQueryParser {

    private UserQuery engine;

    public UserQueryParser() {
        engine = new UserQuery();
    }

    /**
     * Executa uma query SQL usando o parser ANTLR4.
     *
     * @param sql Comando SQL a executar
     * @return Resultado da execução ou mensagem de erro prefixada com "Invalid"
     */
    public String execute(String sql) {
        try {
            // Validar entrada vazia
            if (sql == null || sql.trim().isEmpty()) {
                return "Invalid: Empty query";
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
            final String[] errorMessage = {null};

            // Adicionar custom error listener
            parser.addErrorListener(new BaseErrorListener() {
                @Override
                public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                                        int line, int charPositionInLine,
                                        String msg, RecognitionException e) {
                    hasError[0] = true;
                    errorMessage[0] = msg;
                }
            });

            // Parse da query (ponto de entrada da gramática)
            ParseTree tree = parser.parse();

            // Se houve erro de sintaxe, retornar
            if (hasError[0]) {
                return "Invalid: SQL syntax error" +
                        (errorMessage[0] != null ? " - " + errorMessage[0] : "");
            }

            // Criar visitor e executar
            SQLVisitor visitor = new SQLVisitor(engine);
            Object result = visitor.visit(tree);

            // Processar resultado do visitor
            if (result != null) {
                String resultStr = result.toString();

                // Normalizar mensagens de erro
                // Converter qualquer variação para "Invalid:"
                if (resultStr.startsWith("ERRO:")) {
                    return "Invalid:" + resultStr.substring(5);
                }

                if (resultStr.startsWith("Comando inválido:") ||
                        resultStr.startsWith("Comando invalido:")) {
                    return "Invalid:" + resultStr.substring(resultStr.indexOf(":"));
                }

                // Se já começa com "Invalid", garantir formato consistente
                if (resultStr.startsWith("Invalid")) {
                    // Garantir que tem : depois de Invalid
                    if (resultStr.length() > 7 && resultStr.charAt(7) != ':') {
                        return "Invalid: " + resultStr.substring(7).trim();
                    }
                    return resultStr;
                }

                // Resultado normal (sucesso)
                return resultStr;
            }

            // Retornar OK se não há resultado
            return "OK";

        } catch (RuntimeException e) {
            // Erro de sintaxe ou validação
            String msg = e.getMessage();

            if (msg == null) {
                return "Invalid: Runtime error";
            }

            // Normalizar mensagens de erro para "Invalid:"
            if (msg.startsWith("ERRO:")) {
                return "Invalid:" + msg.substring(5);
            }

            if (msg.startsWith("Comando inválido:") ||
                    msg.startsWith("Comando invalido:")) {
                return "Invalid:" + msg.substring(msg.indexOf(":"));
            }

            if (msg.startsWith("Invalid")) {
                // Já começa com Invalid, garantir formato
                if (msg.length() > 7 && msg.charAt(7) != ':') {
                    return "Invalid: " + msg.substring(7).trim();
                }
                return msg;
            }

            // Adicionar prefixo Invalid a outras mensagens
            return "Invalid: " + msg;

        } catch (Exception e) {
            // Outros erros inesperados
            String msg = e.getMessage();
            return "Invalid: " + (msg != null ? msg : "Unexpected error");
        }
    }

    /**
     * Retorna a engine UserQuery subjacente.
     * Útil para testes e diagnóstico.
     *
     * @return Instância do UserQuery
     */
    public UserQuery getEngine() {
        return engine;
    }
}