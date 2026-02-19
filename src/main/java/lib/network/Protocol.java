package lib.network;

/**
 * Protocolo de comunicação cliente-servidor.
 * 
 * @author SQL Parser Team
 * @version 1.0
 */
public class Protocol {
    
    // Comandos
    public static final String QUERY = "QUERY";
    public static final String PING = "PING";
    public static final String DISCONNECT = "DISCONNECT";
    
    // Respostas
    public static final String RESULT = "RESULT";
    public static final String ERROR = "ERROR";
    public static final String PONG = "PONG";
    public static final String OK = "OK";
    
    // Config
    public static final String SEP = "|";
    public static final int PORT = 5432;
    
    public static String msg(String cmd, String data) {
        return cmd + SEP + data + "\n";
    }
    
    public static String[] parse(String msg) {
        return msg.trim().split("\\" + SEP, 2);
    }
}
