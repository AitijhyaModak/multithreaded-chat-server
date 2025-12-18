import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class Listener implements Runnable {
    private final Socket socket;

    public Listener(Socket socket) {
        this.socket = socket;
    }

    public void run() {
        try(BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            while(true) {
                String response = reader.readLine();
                if (response == null) throw new IOException();
                System.out.println(response);
            }
        }
        catch(IOException e) {
            System.out.println("SERVER DISCONNECTED");
        }
    }
}
