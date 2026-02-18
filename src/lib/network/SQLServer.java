package lib.network;

import lib.UserQuery;
import lib.UserQueryParser;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Servidor SQL multithread que aceita conexões de clientes.
 */
public class SQLServer {

    private final int port;
    private ServerSocket serverSocket;
    private final UserQuery engine;
    private final ExecutorService threadPool;
    private boolean running;

    public SQLServer(int port) {
        this.port = port;
        this.engine = new UserQuery(true);
        this.threadPool = Executors.newFixedThreadPool(10);
        this.running = false;
    }

    /**
     * Encontra uma porta disponível começando pela porta especificada.
     */
    private int findAvailablePort(int startPort) {
        for (int port = startPort; port < startPort + 100; port++) {
            try {
                ServerSocket test = new ServerSocket(port);
                test.close();
                return port;
            } catch (IOException e) {
                // Porta em uso, tentar próxima
            }
        }
        return -1;
    }

    public void start() {
        try {
            int actualPort = port;

            // Tentar porta especificada primeiro
            try {
                serverSocket = new ServerSocket(port);
            } catch (BindException e) {
                System.out.println("Port " + port + " is already in use.");
                System.out.print("Finding available port... ");

                actualPort = findAvailablePort(port + 1);

                if (actualPort == -1) {
                    System.err.println("\nNo available ports found!");
                    return;
                }

                System.out.println("Using port " + actualPort);
                serverSocket = new ServerSocket(actualPort);
            }

            running = true;

            System.out.println("\n╔════════════════════════════════════════╗");
            System.out.println("║      SQL Server Started                ║");
            System.out.println("╚════════════════════════════════════════╝");
            System.out.println("Port: " + actualPort);
            System.out.println("Clients connect with:");
            System.out.println("  java -cp ... lib.network.SQLClient localhost " + actualPort);
            System.out.println("\nPress CTRL+C to stop\n");

            while (running) {
                try {
                    Socket client = serverSocket.accept();
                    System.out.println("Client connected: " +
                            client.getInetAddress().getHostAddress());
                    threadPool.execute(new ClientHandler(client, engine));
                } catch (IOException e) {
                    if (running) System.err.println("Error: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to start: " + e.getMessage());
        }
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null) serverSocket.close();
            threadPool.shutdown();
            engine.shutdown();
            System.out.println("\nServer stopped");
        } catch (IOException e) {
            System.err.println("Error stopping: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : Protocol.PORT;
        SQLServer server = new SQLServer(port);
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        server.start();
    }
}

class ClientHandler implements Runnable {
    private final Socket socket;
    private final UserQuery engine;
    private final UserQueryParser parser;
    private BufferedReader in;
    private PrintWriter out;

    public ClientHandler(Socket socket, UserQuery engine) {
        this.socket = socket;
        this.engine = engine;
        // IMPORTANTE: UserQueryParser cria seu próprio UserQuery
        // Para usar engine compartilhada, precisamos de abordagem diferente
        this.parser = new UserQueryParser();
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true); // autoFlush = true

            // Mensagem de boas-vindas
            sendResponse(Protocol.OK, "Connected to SQL Server");

            System.out.println("Client handler started for " + socket.getInetAddress());

            String line;
            while ((line = in.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                System.out.println("[RECEIVED] " + line);

                String[] parts = Protocol.parse(line);
                if (parts.length == 0) continue;

                String command = parts[0];
                String data = parts.length > 1 ? parts[1] : "";

                switch (command) {
                    case Protocol.QUERY -> handleQuery(data);
                    case Protocol.PING -> {
                        System.out.println("[SENDING] PONG");
                        // PING retorna sem dados
                        out.println("4");  // length de "PONG"
                        out.println(Protocol.PONG);
                    }
                    case Protocol.DISCONNECT -> {
                        System.out.println("[SENDING] Goodbye");
                        sendResponse(Protocol.OK, "Goodbye");
                        return;
                    }
                    default -> {
                        System.out.println("[SENDING] Unknown command error");
                        sendResponse(Protocol.ERROR, "Unknown command: " + command);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Client error: " + e.getMessage());
        } finally {
            close();
        }
    }

    private void handleQuery(String sql) {
        try {
            if (sql == null || sql.trim().isEmpty()) {
                System.out.println("[SENDING] Empty query error");
                sendResponse(Protocol.ERROR, "Empty query");
                return;
            }

            System.out.println("[EXECUTING] " + sql);

            String result = parser.execute(sql);

            if (result == null) {
                result = "OK";
            }

            System.out.println("[RESULT] Raw length=" + result.length());
            System.out.println("[RESULT] Preview=" + result.substring(0, Math.min(100, result.length())));

            // Enviar resultado
            if (result.startsWith("Invalid")) {
                sendResponse(Protocol.ERROR, result);
            } else {
                sendResponse(Protocol.RESULT, result);
            }

        } catch (Exception e) {
            System.err.println("[ERROR] Query execution failed: " + e.getMessage());
            e.printStackTrace();
            sendResponse(Protocol.ERROR, "Query failed: " + e.getMessage());
        }
    }

    /**
     * Envia resposta usando protocolo: LENGTH\nCOMMAND|DATA
     */
    private void sendResponse(String command, String data) {
        try {
            // Limpar data de quebras de linha
            data = data.replace("\r", "").replace("\n", "");

            // Montar mensagem: COMMAND|DATA
            String message = command + Protocol.SEP + data;

            // Enviar tamanho primeiro
            out.println(message.length());

            // Enviar mensagem
            out.println(message);

            System.out.println("[SENT] " + command + " (" + message.length() + " bytes)");

        } catch (Exception e) {
            System.err.println("[ERROR] Failed to send response: " + e.getMessage());
        }
    }

    private void close() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();
            System.out.println("Client disconnected");
        } catch (IOException e) {
            System.err.println("Error closing connection: " + e.getMessage());
        }
    }
}