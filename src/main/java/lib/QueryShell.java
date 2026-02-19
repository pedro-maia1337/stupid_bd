package lib;

import lib.parser.*;
import java.util.Scanner;


public class QueryShell {
    
    private UserQueryParser parser;
    private Scanner scanner;
    private boolean running;

    public QueryShell() {
        this.parser = new UserQueryParser();
        this.scanner = new Scanner(System.in);
        this.running = true;
    }

    /**
     * Inicia o shell interativo.
     * Loop principal que lê comandos do usuário e executa.
     */
    public void start() {
        printWelcome();
        
        while (running) {
            System.out.print("sql> ");
            String input = scanner.nextLine().trim();
            
            // Ignorar linhas vazias
            if (input.isEmpty()) {
                continue;
            }
            
            // Comandos especiais do shell
            if (handleSpecialCommand(input)) {
                continue;
            }
            
            // Executar como SQL
            executeSQL(input);
        }
        
        scanner.close();
    }
    
    /**
     * Trata comandos especiais do shell (exit, help, clear).
     * 
     * @param input Comando digitado pelo usuário
     * @return true se foi um comando especial, false caso contrário
     */
    private boolean handleSpecialCommand(String input) {
        String cmd = input.toLowerCase();
        
        // Sair do shell
        if (cmd.equals("exit") || cmd.equals("quit") || cmd.equals("q")) {
            System.out.println("Encerrando main.java.lib.QueryShell. Até logo!");
            running = false;
            return true;
        }
        
        // Mostrar ajuda
        if (cmd.equals("help") || cmd.equals("?")) {
            printHelp();
            return true;
        }
        
        // Limpar tela
        if (cmd.equals("clear") || cmd.equals("cls")) {
            clearScreen();
            return true;
        }
        
        // Mostrar informações do banco
        if (cmd.equals("info") || cmd.equals("status")) {
            printDatabaseInfo();
            return true;
        }
        
        // Executar COUNT rápido
        if (cmd.equals("count")) {
            executeSQL("SELECT count FROM users");
            return true;
        }

        if (cmd.equals("stats") || cmd.equals("index")) {
            showIndexStats();
            return true;
        }

        return false;
    }

    private void showIndexStats() {
        try {
            // Usar reflection para acessar UserQuery do parser
            // Ou adicionar método getIndexStats() no UserQueryParser
            System.out.println("Estatísticas dos índices:");
            System.out.println("(implementar acesso ao IndexManager)");
        } catch (Exception e) {
            System.out.println("Erro ao obter estatísticas");
        }
    }
    
    /**
     * Executa um comando SQL.
     * 
     * @param sql Comando SQL a ser executado
     */
    private void executeSQL(String sql) {
        try {
            long startTime = System.currentTimeMillis();
            String result = parser.execute(sql);
            long endTime = System.currentTimeMillis();
            
            System.out.println(result);
            System.out.println("(" + (endTime - startTime) + " ms)");
            System.out.println();
            
        } catch (Exception e) {
            System.out.println("❌ Erro: " + e.getMessage());
            System.out.println();
        }
    }
    
    /**
     * Imprime mensagem de boas-vindas.
     */
    private void printWelcome() {
        System.out.println("╔════════════════════════════════════════════════╗");
        System.out.println("║         main.java.lib.QueryShell - SQL Interactive           ║");
        System.out.println("║              Versão 1.0                        ║");
        System.out.println("╚════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("Digite comandos SQL diretamente (sem aspas)");
        System.out.println("Digite 'help' para ver exemplos");
        System.out.println("Digite 'exit' para sair");
        System.out.println();
        printDatabaseInfo();
    }
    
    /**
     * Imprime informações do banco de dados.
     */
    private void printDatabaseInfo() {
        System.out.println("Banco de dados: In-Memory");
        System.out.println("Tabela: users");
        System.out.println("Colunas: id (int), name (string), age (int), city (string)");
        System.out.println("Registros: 30 usuários pré-carregados (IDs 1-30)");
        System.out.println();
    }
    
    /**
     * Imprime menu de ajuda com exemplos.
     */
    private void printHelp() {
        System.out.println("╔════════════════════════════════════════════════╗");
        System.out.println("║                    AJUDA                       ║");
        System.out.println("╚════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("COMANDOS SQL:");
        System.out.println("  SELECT * FROM users");
        System.out.println("  SELECT count FROM users");
        System.out.println("  SELECT name, age FROM users");
        System.out.println("  SELECT * FROM users WHERE age > 30");
        System.out.println("  SELECT * FROM users WHERE name LIKE 'Ana'");
        System.out.println("  SELECT * FROM users WHERE age BETWEEN 25 AND 35");
        System.out.println("  SELECT * FROM users ORDER BY age ASC");
        System.out.println("  SELECT city FROM users GROUP BY city");
        System.out.println();
        System.out.println("  INSERT INTO users VALUES ('Nome', 25, 'Cidade')");
        System.out.println("  UPDATE users SET age=26 WHERE id=1");
        System.out.println("  DELETE FROM users WHERE id=31");
        System.out.println();
        System.out.println(" COMANDOS ESPECIAIS:");
        System.out.println("  help     - Mostra esta ajuda");
        System.out.println("  info     - Informações do banco de dados");
        System.out.println("  count    - Atalho para SELECT count FROM users");
        System.out.println("  clear    - Limpa a tela");
        System.out.println("  exit     - Sai do shell");
        System.out.println();
        System.out.println("DICAS:");
        System.out.println("  - Não use aspas ao redor dos comandos SQL");
        System.out.println("  - Use aspas simples para valores string: 'texto'");
        System.out.println("  - DELETE só aceita WHERE id=X (por segurança)");
        System.out.println("  - Novos INSERTs recebem IDs a partir de 31");
        System.out.println();
    }
    
    /**
     * Limpa a tela do console (multiplataforma).
     */
    private void clearScreen() {
        try {
            if (System.getProperty("os.name").contains("Windows")) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                // Linux/Mac - usar código ANSI
                System.out.print("\033[H\033[2J");
                System.out.flush();
            }
        } catch (Exception e) {
            // Se falhar, apenas imprime linhas vazias
            for (int i = 0; i < 50; i++) {
                System.out.println();
            }
        }
    }

    /**
     * Método main para executar o main.java.lib.QueryShell standalone.
     * 
     * @param args Argumentos de linha de comando (não utilizados)
     */
    public static void main(String[] args) {
        QueryShell shell = new QueryShell();
        shell.start();
    }
}
