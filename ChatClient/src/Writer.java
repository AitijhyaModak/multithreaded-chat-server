import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Writer implements Runnable {
    private final Socket socket;
    private PrintWriter writer;
    private BufferedReader reader;

    public Writer(Socket socket) {
        this.socket = socket;
    }

    public void run() {

        try(BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            writer = new PrintWriter(socket.getOutputStream(), true);

            while(true) {
                String message = reader.readLine();
                writer.println(message);
            }
        }
        catch (Exception e) {
            System.out.println("Closing writer thread...");
        }
        finally {
            if (writer != null) writer.close();
        }
    }
}
