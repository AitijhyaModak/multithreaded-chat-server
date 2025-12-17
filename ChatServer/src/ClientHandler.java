import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.*;
import java.io.BufferedReader;

enum CommandType{
    SEND_PRIVATE_MESSAGE(3),
    BROADCAST_MESSAGE( 2),
    LIST_USERS( 1),
    BLOCK_USER( 2),
    UNBLOCK_USER(2),
    HELP( 1),
    INVALID_COMMAND( 1),
    EXIT(1);


    private final int argCount;

    CommandType( int argCount) {
        this.argCount = argCount;
    }

    public int getArgCount() { return this.argCount; }
}

public class ClientHandler implements Runnable {

    private final Socket clientSocket;
    private final Map<String, ClientHandler> clientMap;
    public Set<String> blockList;
    private PrintWriter writer;
    private BufferedReader reader;
    private String clientName;

    public ClientHandler(Socket clientSocket, Map<String, ClientHandler> clientMap) {
        this.clientSocket = clientSocket;
        this.clientMap = clientMap;
        this.blockList = Collections.synchronizedSet(new HashSet<>());
    }

    public String getClientName() {
        return clientName;
    }

    /**
     * Gets command type from message.
     * @param message Message received from client.
     * @return Command type as parsed from message.
     */
    private CommandType getCommandType(String message) {
        String []args = message.split(" ", 2);
        return switch (args[0]) {
            case "/msg" -> CommandType.SEND_PRIVATE_MESSAGE;
            case "/msgall" -> CommandType.BROADCAST_MESSAGE;
            case "/list" -> CommandType.LIST_USERS;
            case "/block" -> CommandType.BLOCK_USER;
            case "/unblock" -> CommandType.UNBLOCK_USER;
            case "/help" -> CommandType.HELP;
            case "/exit" -> CommandType.EXIT;
            default -> CommandType.INVALID_COMMAND;
        };
    }

    @Override
    public void run() {
        try {
            reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            writer = new PrintWriter(clientSocket.getOutputStream(), true);

            writer.println("SERVER: Welcome to chat-server. Please enter your username: ");

            String enteredUsername = reader.readLine();
            if (enteredUsername == null) return;

            while(true) {
                synchronized(clientMap) {
                    if (clientMap.containsKey(enteredUsername)) {
                        writer.println("SERVER: Username taken. <");

                        enteredUsername = reader.readLine();
                        if (enteredUsername == null) return;
                    }
                    else {
                        this.clientName = enteredUsername;
                        clientMap.put(enteredUsername, this);
                        break;
                    }
                }
            }

            writer.println("SERVER: Your username is set to " + enteredUsername + ". <");
            broadcastMessage("SERVER: [" + clientName + "] has joined the server. <");

            String message;

            while((message = reader.readLine()) != null) {
                handleMessage(message);
            }
        }
        catch(IOException e) {
            System.out.println("Something went wrong at handling client " + clientName + ": " + e.getMessage());
        }
        finally {
            closeConnection();
        }
    }

    private void handleMessage(String message) {
        CommandType type = getCommandType(message);

        switch (type) {
            case HELP -> handleHelpCommand();
            case LIST_USERS -> handleListCommand();
            case BROADCAST_MESSAGE -> handleMessageAllCommand(message);
            case SEND_PRIVATE_MESSAGE -> handlePrivateMessageCommand(message);
            case BLOCK_USER -> handleBlockCommand(message);
            case UNBLOCK_USER -> handleUnblockCommand(message);
            case EXIT -> handleExitCommand();
            default -> handleInvalidCommand();
        }
    }

    /**
     * Handles /help command
     */
    private void handleHelpCommand() {
        String HELP_MESSAGE = """
                The list of available commands:
                /help: Lists available commands
                /msg <username> <message>: Sends <message> to <username>
                /msgall <message>: Broadcasts <message> to everyone
                /block <username>: Blocks <username> for you. You won't be able to see any messages from <username>
                /unblock <username>: Unblocks <username> for you
                /list: Lists currently online users
                """;
        sendMessage("SERVER: " + HELP_MESSAGE);
    }

    /**
     * Handles an invalid command
     */
    private void handleInvalidCommand() {
        sendMessage("SERVER: Invalid Command. Type '\\help' to list all available commands and arguments <");
    }

    /**
     * Handles /block command.
     * Handles /block command.
     * @param message The command received by user.
     */
    private void handleBlockCommand(String message) {
        int requiredLength = CommandType.BLOCK_USER.getArgCount();
        String[] args =  message.split(" ", requiredLength);
        if (args.length < requiredLength) {
            writer.println("SERVER: Invalid message format. The format for //block is '//block <username>'. <");
            return;
        }

        blockList.add(args[1]);
        sendMessage("SERVER: " + args[1] + " successfully blocked. <");
    }

    /**
     * Handles /unblock command.
     * @param message The command received from the client.
     */
    private void handleUnblockCommand(String message) {
        int requiredLength = CommandType.UNBLOCK_USER.getArgCount();
        String[] args =  message.split(" ", requiredLength);

        if (args.length < requiredLength) {
            writer.println("SERVER: Invalid message format. The format for //block is '//block <username>'. <");
            return;
        }
        blockList.remove(args[1]);
        sendMessage("SERVER: " + args[1] + " successfully unblocked. <");
    }

    /**
     * Handles /list command.
     */
    private void handleListCommand() {
        StringBuilder fullMessage = new StringBuilder();
        synchronized (clientMap) {
            for (ClientHandler handler: clientMap.values()) fullMessage.append(handler.getClientName()).append("\n");
        }

        sendMessage(fullMessage.toString());
    }

    /**
     * Handles /msgall command.
     * @param message The message that has to be broadcasted.
     */
    private void handleMessageAllCommand(String message) {
        int requiredLength = CommandType.BROADCAST_MESSAGE.getArgCount();
        String[] args =  message.split(" ", requiredLength);

        if (args.length < requiredLength) writer.println("SERVER: Invalid Command: Format for '//msgall' is '//msgall <message>'. <");
        else broadcastMessage("[" + clientName + "]: " + args[1]);
    }

    /**
     * Handles /msg command.
     * @param message The message that has to be sent.
     */
    private void handlePrivateMessageCommand(String message) {
        int requiredLength = CommandType.SEND_PRIVATE_MESSAGE.getArgCount();
        String[] args = message.split(" ", requiredLength);

        if (args.length < requiredLength) {
            sendMessage("SERVER: Usage: /msg <username> <message>");
            return;
        }

        String targetName = args[1];
        String content = args[2];

        ClientHandler target = clientMap.get(targetName);

        if (target != null) {
            if (!target.blockList.contains(this.clientName)) target.sendMessage("[Private from " + clientName + "]: " + content);
        }
        else sendMessage("SERVER: User " + targetName + " is not online. <");
    }

    /**
     * Handles /exit command
     */
    private void handleExitCommand() {
        closeConnection();
    }

    /**
     * Broadcasts a message to all connected clients.
     * @param message The string to be sent to clients.
     */
    private void broadcastMessage(String message) {
        synchronized(clientMap) {
            for(ClientHandler handler: clientMap.values()) {
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
     * Safely closes resources and removes handler from global list and map.
     */
    private void closeConnection() {
        try {
            clientMap.remove(clientName);
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
