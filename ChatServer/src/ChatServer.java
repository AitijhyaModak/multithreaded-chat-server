import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class ChatServer {
    private final int port;
    private static final List<ClientHandler> CLIENT_HANDLER_LIST = Collections.synchronizedList(new LinkedList<>());
    private static final Map<String, ClientHandler> CLIENT_HANDLER_MAP = Collections.synchronizedMap(new HashMap<>());

    public ChatServer(int port) {
        this.port = port;
    }

    public void start() {
        try(ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server started at port " + port + ".");
            System.out.println("Listening for clients...");

            while(true) {
                Socket clientSocket = serverSocket.accept();
                String hostIPAddress  = clientSocket.getInetAddress().getHostAddress();

                System.out.println("New client connected with IP " + hostIPAddress + ".");

                ClientHandler clientHandler = new ClientHandler(clientSocket, CLIENT_HANDLER_LIST);
                CLIENT_HANDLER_LIST.add(clientHandler);

                new Thread(clientHandler).start();
            }
        }
        catch(IOException e) {
            System.out.println("Error creating server socket: " + e.getMessage());
        }
    }

    public static void main(String []args) {
        int port = Integer.parseInt(args[0]);
        ChatServer chatServer = new ChatServer(port);
        chatServer.start();
    }
}
