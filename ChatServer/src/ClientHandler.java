import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.io.BufferedReader;
import java.util.Map;
import java.util.Set;

enum CommandType{
    SEND_PRIVATE_MESSAGE("/msg", 3),
    BROADCAST_MESSAGE("/msgall", 2),
    LIST_USERS("/list", 1),
    BLOCK_USER("/block", 2),
    UNBLOCK_USER("/unblock", 2),
    HELP("/help", 1),
    INVALID_COMMAND("", 1);

    private final String command;
    private final int argCount;

    CommandType(String command, int argCount) {
        this.command = command;
        this.argCount = argCount;
    }

    public String getCommand() { return this.command; }
    public int getArgCount() { return this.argCount; }
}

public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final List<ClientHandler> allClientHandlers;
    private final Map<String, ClientHandler> clientMap;
    private Set<String> blockList;
    private PrintWriter writer;
    private BufferedReader reader;
    private String clientName;

    private final String HELP_MESSAGE = "The list of available commands: \n" +
            "/help: Lists available commands\n" +
            "/msg <username> <message>: Sends <message> to <username>\n" +
            "/msgall <message>: Broadcasts <message> to everyone\n" +
            "/block <username>: Blocks <username> for you. You won't be able to see any messages from <username>\n" +
            "/unblock <username>: Unblocks <username> for you\n" +
            "/list: Lists currently online users\n";

    public ClientHandler(Socket clientSocket, List<ClientHandler> allClientHandlers, Map<String, ClientHandler> clientMap) {
        this.clientSocket = clientSocket;
        this.allClientHandlers = allClientHandlers;
        this.clientMap = clientMap;
    }

    public String getClientName() {
        return clientName;
    }

    private CommandType getCommandType(String message) {
        String []args = message.split(" ", 2);
        return switch (args[0]) {
            case "/msg" -> CommandType.SEND_PRIVATE_MESSAGE;
            case "/msgall" -> CommandType.BROADCAST_MESSAGE;
            case "/list" -> CommandType.LIST_USERS;
            case "/block" -> CommandType.BLOCK_USER;
            case "/unblock" -> CommandType.UNBLOCK_USER;
            case "/help" -> CommandType.HELP;
            default -> CommandType.INVALID_COMMAND;
        };
    }

    @Override
    public void run() {
        try {
            reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            writer = new PrintWriter(clientSocket.getOutputStream(), true);
            writer.println("Welcome to Chat server!! Please enter your name...");
            clientName = reader.readLine();
            System.out.printf("Client " + clientName + " is now logged in");

            broadcastMessage("SERVER: " + clientName + " joined the server <");

            String clientMessage;
            String fullMessage;

            while((clientMessage = reader.readLine()) != null) {
                MessageType type = getType(clientMessage);

                switch(type) {
                    case EVERYONE:
                        fullMessage = "[" + clientName + "]" + clientMessage;
                        broadcastMessage(fullMessage);
                        break;
                    case PRIVATE:
                        String []args = clientMessage.split(" ",3);
                        if (args.length < 3) {
                            sendMessage("SERVER: Usage: /msg <name> <message>");
                            break;
                        }
                        fullMessage = "[" + clientName + "]" + args[2];
                        sendPrivateMessage(args[1], fullMessage);
                        break;
                }
            }
        }
        catch(IOException e) {
            System.out.println("Client disconnected due to some error: " + e.getMessage());
        }
        finally {
            closeConnection();
        }
    }

    private void handleHelpCommand(ClientHandler toClient) {
        toClient.sendMessage(HELP_MESSAGE);
    }

    private void handleInvalidCommand(ClientHandler toClient) {
        toClient.sendMessage("Invalid Command. Type '\\help' to list all available commands and arguments <");
    }

    private void handleBlockCommand(String username) {
        blockList.add(username);
        sendMessage("SERVER: " + username + " successfully blocked <");
    }

    private void handleUnblockCommand(String username) {
        blockList.remove(username);
        sendMessage("SERVER: " + username + " successfully unblocked <");
    }

    private void handleListCommand() {
        StringBuilder fullMessage = new StringBuilder();
        synchronized (allClientHandlers) {
            for (ClientHandler handler: allClientHandlers) fullMessage.append(handler.getClientName()).append("\n");
        }

        sendMessage(fullMessage.toString());
    }

    private void handleMessageAllCommand(String message) {
        broadcastMessage(message);
    }

    private void handlePrivateMessageCommand(String username, String message) {

    }
    /**
     * Broadcasts a message to all connected clients.
     * @param message The string to be sent to clients.
     */
    public void broadcastMessage(String message) {
        synchronized(allClientHandlers) {
            for(ClientHandler handler: allClientHandlers) {
                if (handler != this) {
                    handler.sendMessage(message);
                }
            }
        }
    }

    /**
     * Send a message to current client.
     * @param message The message to be sent to the client.
     */
    public void sendMessage(String message) {
        writer.println(message);
    }

    /**
     * Send a private message to a particular client.
     * @param toClientName The client name to whom message has to be sent.
     * @param message The message that has to be sent.
     */
    public void sendPrivateMessage(String toClientName, String message) {
        boolean messageSent = false;
        synchronized (allClientHandlers) {
            for (ClientHandler handler: allClientHandlers) {
                String handlerClientName = handler.getClientName();
                if (toClientName.equals(handlerClientName)) {
                    handler.sendMessage(message);
                    messageSent = true;
                }
            }
        }

        if (!messageSent) sendMessage("SERVER: " + toClientName + "CLIENT NAME DOES NOT EXIST OR HAS LEFT THE SERVER <");
    }


    /**
     * Safely closes resources and removes handler from global list.
     */
    private void closeConnection() {
        try {
            allClientHandlers.remove(this);
            broadcastMessage("SERVER: " + clientName + " has left the server <");

            System.out.println(clientName + " has left the server.");

            if (writer != null) writer.close();
            if (reader != null) reader.close();
            if (clientSocket != null && !clientSocket.isClosed()) clientSocket.close();
        }
        catch (IOException e) {
            System.out.println("Error closing resources for " + clientName + ": " + e.getMessage());
        }
    }
}
