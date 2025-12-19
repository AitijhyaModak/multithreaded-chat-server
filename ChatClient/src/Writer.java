import java.io.*;
import java.net.Socket;

public class Writer implements Runnable {
    private final Socket socket;
    private PrintWriter writer;

    public Writer(Socket socket) {
        this.socket = socket;
    }

    private File checkFileTransfer(String message) {
        if (message.charAt(0) == '@') {
            String[] args = message.split(" ", 2);
            System.out.println(args[0]);
            System.out.println(args[1]);
            if (args.length < 2) return null;
            File file = new File(args[1]);
            return file.exists() ? file : null;
        }

        return null;
    }

    public void run() {
        try(BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            writer = new PrintWriter(socket.getOutputStream(), true);

            while(true) {
                String message = reader.readLine();
                File file;

                if ((file = checkFileTransfer(message)) != null) {
                    Long fileSize = file.length();
                    System.out.println(fileSize);
                    writer.println("@ " + file.getName() + " " + fileSize.toString());

                    BufferedOutputStream bos = new BufferedOutputStream(socket.getOutputStream());
                    BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));

                    byte[] buffer = new byte[8192];

                    long remaining = file.length();

                    while(remaining > 0) {
                        long sizeToRead = Math.min(remaining, 8192);
                        int sizeRead = bis.read(buffer, 0, (int)sizeToRead);
                        remaining -= sizeRead;
                        bos.write(buffer, 0, sizeRead);
                    }

                    bos.flush();
                    bis.close();
                }
                else writer.println(message);
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
