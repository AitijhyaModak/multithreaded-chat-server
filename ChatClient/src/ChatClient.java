import java.io.IOException;
import java.net.Socket;

public class ChatClient {
    private final String hostname;
    private final int port;

    ChatClient(String hostname, int port) {
        this.hostname = hostname;
        this.port = port;
    }

    public void start() {
        try {
            Socket socket = new Socket(hostname, port);
            System.out.println("----Connected to chat-server----");

            Thread listenerThread = new Thread(new Listener(socket));
            Thread readerThread = new Thread(new Writer(socket));

            listenerThread.start();
            readerThread.start();

            readerThread.join();

            if (!socket.isClosed()) socket.close();

            listenerThread.join();

            System.out.println("Successfully closed connection");
        }
        catch (IOException e) {
            System.out.println("Error connecting to socket: " + e.getMessage());
        }
        catch (InterruptedException e) {
            System.out.println("Interrupted thread exception: " + e.getMessage());
        }
    }

    public static void main(String []args) {
        ChatClient chatClient = new ChatClient(args[0], Integer.parseInt(args[1]));
        chatClient.start();
    }
}
