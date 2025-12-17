import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.Buffer;

public class Writer implements Runnable {
    private final Socket socket;
    private PrintWriter writer;

    public Writer(Socket socket) {
        this.socket = socket;
        try {
            writer = new PrintWriter(socket.getOutputStream(), true);
        }
        catch (IOException e) {
            System.out.println("Error establishing connection with server: " + e.getMessage());
        }
    }

    public void run() {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        try {
            while(true) {
                String message = reader.readLine();
                writer.println(message);
            }
        }
        catch (IOException e) {
            System.out.println("IO Exception: " + e.getMessage());
        }
    }
}
