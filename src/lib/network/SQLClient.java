package lib.network;

import java.io.*;
import java.net.*;
import java.util.Scanner;

/**
 * Cliente SQL interativo.
 */
public class SQLClient {

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private final String host;
    private final int port;
    private boolean connected;

    public SQLClient(String host, int port) {
        this.host = host;
        this.port = port;
        this.connected = false;
    }

    public void connect() throws IOException {
        try {
            System.out.println("Connecting to " + host + ":" + port + "...");

            socket = new Socket();
            socket.connect(new InetSocketAddress(host, port), 5000); // 5s timeout
            socket.setSoTimeout(30000); // 30s read timeout

            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // Ler mensagem de boas-vindas (NOVO PROTOCOLO: LENGTH\nMESSAGE)
            String lengthStr = in.readLine();
            if (lengthStr == null) {
                throw new IOException("Server closed connection immediately");
            }

            String welcome = in.readLine();
            if (welcome == null) {
                throw new IOException("Server closed connection immediately");
            }

            System.out.println("Server response: " + welcome);

            String[] parts = Protocol.parse(welcome);
            if (parts.length > 1) {
                System.out.println(parts[1]);
            }

            connected = true;
            System.out.println("Connected successfully!\n");

        } catch (SocketTimeoutException e) {
            throw new IOException("Connection timeout - server not responding");
        } catch (ConnectException e) {
            throw new IOException("Connection refused - server not running on port " + port);
        }
    }

    public String execute(String sql) throws IOException {
        if (!connected) {
            throw new IOException("Not connected to server");
        }

        try {
            // Enviar query
            String message = Protocol.msg(Protocol.QUERY, sql).trim();
            out.println(message);

            System.out.println("[CLIENT DEBUG] Sent: " + message);

            // IMPORTANTE: Garantir que foi enviado
            if (out.checkError()) {
                connected = false;
                throw new IOException("Failed to send query to server");
            }

            // LER PROTOCOLO: LENGTH\nCOMMAND|DATA

            // 1. Ler tamanho
            System.out.println("[CLIENT DEBUG] Reading response length...");
            String lengthStr = in.readLine();

            if (lengthStr == null || lengthStr.trim().isEmpty()) {
                connected = false;
                throw new IOException("Server closed connection (no length)");
            }

            int length = Integer.parseInt(lengthStr.trim());
            System.out.println("[CLIENT DEBUG] Expected length: " + length);

            // 2. Ler mensagem
            System.out.println("[CLIENT DEBUG] Reading message...");
            String response = in.readLine();

            System.out.println("[CLIENT DEBUG] Received: " +
                    (response != null ? response.substring(0, Math.min(100, response.length())) : "NULL"));
            System.out.println("[CLIENT DEBUG] Actual length: " + (response != null ? response.length() : 0));

            if (response == null) {
                connected = false;
                throw new IOException("Server closed connection");
            }

            if (response.length() != length) {
                System.err.println("[CLIENT WARN] Length mismatch! Expected " + length + ", got " + response.length());
            }

            // Parse resposta
            String[] parts = Protocol.parse(response);

            System.out.println("[CLIENT DEBUG] Command: " + (parts.length > 0 ? parts[0] : "none"));
            System.out.println("[CLIENT DEBUG] Data length: " + (parts.length > 1 ? parts[1].length() : 0));

            if (parts.length < 1) {
                return response;
            }

            String command = parts[0];
            String data = parts.length > 1 ? parts[1] : "";

            // Processar baseado no tipo
            if (command.equals(Protocol.ERROR)) {
                return "ERROR: " + data;
            }

            if (command.equals(Protocol.RESULT)) {
                System.out.println("[CLIENT DEBUG] Formatting result...");
                return ResultFormatter.formatAsTable(data);
            }

            return data;

        } catch (SocketTimeoutException e) {
            connected = false;
            throw new IOException("Server timeout - no response received");
        } catch (NumberFormatException e) {
            connected = false;
            throw new IOException("Protocol error - invalid length");
        } catch (IOException e) {
            connected = false;
            throw e;
        }
    }

    public void disconnect() {
        if (!connected) {
            return;
        }

        try {
            if (out != null) {
                out.println(Protocol.msg(Protocol.DISCONNECT, ""));
                out.flush();

                String response = in.readLine();
                if (response != null) {
                    String[] parts = Protocol.parse(response);
                    if (parts.length > 1) {
                        System.out.println(parts[1]);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error during disconnect: " + e.getMessage());
        } finally {
            close();
        }
    }

    private void close() {
        connected = false;
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            // Ignorar
        }
    }

    public void startREPL() {
        Scanner scanner = new Scanner(System.in);

        System.out.println("╔════════════════════════════════════════╗");
        System.out.println("║         SQL Client                     ║");
        System.out.println("╚════════════════════════════════════════╝");
        System.out.println("Connected to: " + host + ":" + port);
        System.out.println("Type 'exit' to quit, 'ping' to test connection\n");

        while (connected) {
            System.out.print("sql> ");

            if (!scanner.hasNextLine()) {
                break;
            }

            String line = scanner.nextLine().trim();

            if (line.isEmpty()) continue;

            if (line.equalsIgnoreCase("exit") || line.equalsIgnoreCase("quit")) {
                disconnect();
                break;
            }

            if (line.equalsIgnoreCase("ping")) {
                try {
                    out.println(Protocol.PING);
                    // Ler length
                    String lengthStr = in.readLine();
                    // Ler resposta (PONG)
                    String response = in.readLine();
                    System.out.println(response != null ? response : "No response");
                    System.out.println();
                } catch (IOException e) {
                    System.err.println("Error: " + e.getMessage());
                    connected = false;
                    break;
                }
                continue;
            }

            try {
                String result = execute(line);
                System.out.println(result);
                System.out.println();
            } catch (IOException e) {
                System.err.println("Error: " + e.getMessage());
                connected = false;
                break;
            }
        }

        scanner.close();
    }

    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : "localhost";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : Protocol.PORT;

        SQLClient client = new SQLClient(host, port);

        try {
            client.connect();
            client.startREPL();
        } catch (IOException e) {
            System.err.println("\nConnection failed: " + e.getMessage());
            System.err.println("\nTroubleshooting:");
            System.err.println("1. Make sure the server is running:");
            System.err.println("   java -cp ... lib.network.SQLServer");
            System.err.println("2. Check if using correct port: " + port);
            System.err.println("3. Check firewall settings");
            System.err.println("4. Try: netstat -ano | findstr \":" + port + "\"");
        }
    }
}