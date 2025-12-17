import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class Listener implements Runnable {
    private final Socket socket;
    private BufferedReader reader;

    public Listener(Socket socket) {
        this.socket = socket;
        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        }
        catch(IOException e) {
            System.out.println("Error getting input stream: " + e.getMessage());
        }

    }

    public void run() {
        try {
            while(true) {
                String response = reader.readLine();
                if (response == null) {
                    System.out.println("SERVER DISCONNECTED");
                    break;
                }
                System.out.println(response);
            }
        }
        catch(IOException e) {
            System.out.println("Error connecting to server: " + e.getMessage());
        }
    }
}
