import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.*;
import java.io.BufferedReader;

public class ClientHandler implements Runnable {

    private final Socket clientSocket;
    private final Map<String, ClientHandler> clientMap;
    public Set<String> blockList;
    private PrintWriter writer;
    private BufferedReader reader;
    private String clientName;

    private static final String SERVER_PREFIX = Colors.GREEN + "[SERVER]: " + Colors.RESET;

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

            writer.println(SERVER_PREFIX + Colors.CYAN + "Welcome! Please enter your username:" + Colors.RESET);

            String enteredUsername = reader.readLine();
            if (enteredUsername == null) throw new IOException("Client Disconnected");

            while(true) {
                synchronized(clientMap) {
                    if (clientMap.containsKey(enteredUsername)) {
                        writer.println(SERVER_PREFIX + Colors.RED + "Username already taken. Try again:" + Colors.RESET);

                        enteredUsername = reader.readLine();
                        if (enteredUsername == null) throw new IOException("Client Disconnected");
                    }
                    else {
                        this.clientName = enteredUsername;
                        clientMap.put(enteredUsername, this);
                        break;
                    }
                }
            }

            writer.println(SERVER_PREFIX + "Username set to " + Colors.GREEN + clientName + Colors.RESET + ".");
            broadcastMessage(SERVER_PREFIX + Colors.YELLOW + "✦ " + clientName + " has joined the chat ✦" + Colors.RESET);

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
        String help = Colors.YELLOW + "\n--- Available Commands ---\n" + Colors.RESET +
                Colors.CYAN + "/help" + Colors.RESET + "          - Show this menu\n" +
                Colors.CYAN + "/list" + Colors.RESET + "          - List online users\n" +
                Colors.CYAN + "/msg <u> <m>" + Colors.RESET + "   - Private message\n" +
                Colors.CYAN + "/msgall <m>" + Colors.RESET + "     - Global message\n" +
                Colors.CYAN + "/block <u>" + Colors.RESET + "      - Block a user\n" +
                Colors.CYAN + "/exit" + Colors.RESET + "          - Leave chat\n";
        sendMessage(SERVER_PREFIX + help);
    }

    /**
     * Handles an invalid command
     */
    private void handleInvalidCommand() {
        sendMessage(SERVER_PREFIX + Colors.RED + "Invalid Command. Type " + Colors.YELLOW + "/help" + Colors.RED + " for list." + Colors.RESET);
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
            sendMessage(SERVER_PREFIX + Colors.RED + "Usage: /block <username>" + Colors.RESET);
            return;
        }

        blockList.add(args[1]);
        sendMessage(SERVER_PREFIX + "User " + Colors.GREEN + args[1] + Colors.RESET + " blocked.");
    }

    /**
     * Handles /unblock command.
     * @param message The command received from the client.
     */
    private void handleUnblockCommand(String message) {
        int requiredLength = CommandType.UNBLOCK_USER.getArgCount();
        String[] args =  message.split(" ", requiredLength);

        if (args.length < requiredLength) {
            sendMessage(SERVER_PREFIX + Colors.RED + "Usage: /unblock <username>" + Colors.RESET);
            return;
        }
        blockList.remove(args[1]);
        sendMessage(SERVER_PREFIX + "User " + Colors.GREEN + args[1] + Colors.RESET + " unblocked.");
    }

    /**
     * Handles /list command.
     */
    private void handleListCommand() {
        StringBuilder users = new StringBuilder();
        synchronized (clientMap) {
            for (ClientHandler handler: clientMap.values()) users.append(handler.getClientName()).append("\n");
        }

        sendMessage(SERVER_PREFIX + Colors.CYAN + "Online: " + Colors.RESET + users);
    }

    /**
     * Handles /msgall command.
     * @param message The message that has to be broadcasted.
     */
    private void handleMessageAllCommand(String message) {
        int limit = CommandType.BROADCAST_MESSAGE.getArgCount();
        String[] args = message.split(" ", limit);
        if (args.length < limit) {
            sendMessage(SERVER_PREFIX + Colors.RED + "Usage: /msgall <message>" + Colors.RESET);
        } else {
            broadcastMessage(Colors.PURPLE + "[Global] " + clientName + ": " + Colors.RESET + Colors.CYAN + args[1] + Colors.RESET);
        }
    }

    /**
     * Handles /msg command.
     * @param message The message that has to be sent.
     */
    private void handlePrivateMessageCommand(String message) {
        int requiredLength = CommandType.SEND_PRIVATE_MESSAGE.getArgCount();
        String[] args = message.split(" ", requiredLength);

        if (args.length < requiredLength) {
            sendMessage(SERVER_PREFIX +  "Usage: /msg <username> <message>");
            return;
        }

        String targetName = args[1];
        String content = args[2];

        ClientHandler target = clientMap.get(targetName);

        if (target != null) {
            if (!target.blockList.contains(this.clientName)) target.sendMessage(Colors.BLUE + "[Private] " + clientName + ": " + Colors.RESET + Colors.CYAN + args[2] + Colors.RESET);
        }
        else sendMessage(SERVER_PREFIX + Colors.RED + "User " + targetName + " not found." + Colors.RESET);
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
            if (clientMap.containsKey(clientName)) {
                clientMap.remove(clientName);
                broadcastMessage(SERVER_PREFIX + clientName + " has left the server");
            }

            System.out.println(clientName + " has left the server");

            if (writer != null) writer.close();
            if (reader != null) reader.close();
            if (clientSocket != null && !clientSocket.isClosed()) clientSocket.close();
        }
        catch (IOException e) {
            System.out.println("Error closing resources for " + clientName + ": " + e.getMessage());
        }
    }
}
