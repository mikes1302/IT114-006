package Project.Client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import Project.Common.ConnectionPayload;
import Project.Common.Constants;
import Project.Common.Payload;
import Project.Common.PayloadType;
import Project.Common.RoomResultsPayload;
import Project.Common.TextFX;
import Project.Common.TextFX.Color;

public enum Client {
    INSTANCE;

    private Socket server = null;
    private ObjectOutputStream out = null;
    private ObjectInputStream in = null;
    final String ipAddressPattern = "/connect\\s+(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}:\\d{3,5})";
    final String localhostPattern = "/connect\\s+(localhost:\\d{3,5})";
    private boolean isRunning = false;
    private Thread inputThread;
    private Thread fromServerThread;
    private String clientName = "";

    private static final String CREATE_ROOM = "/createroom";
    private static final String JOIN_ROOM = "/joinroom";
    private static final String LIST_ROOMS = "/listrooms";
    private static final String LIST_USERS = "/users";
    private static final String DISCONNECT = "/disconnect";
    private static final String HELLO_COMMAND = "/hello";
    private static final String ROLL_COMMAND = "/roll";
    private static final String FLIP_COMMAND = "/flip";
    // New Code Milestone 3 MS75 4-27-24 
    // Mute Feature 
    private static final String MUTE_COMMAND = "/mute";

    // client id, is the key, client name is the value
    private ConcurrentHashMap<Long, String> clientsInRoom = new ConcurrentHashMap<Long, String>();
    private long myClientId = Constants.DEFAULT_CLIENT_ID;
    private Logger logger = Logger.getLogger(Client.class.getName());
   // CAllback that updates the UI
    private static IClientEvents events;

    // New Code Milestone 3 MS75 4-27-24 
    // Mute Feature 

private List<Long> mutedClients = new ArrayList<>();

    public void muteClient(long clientId) {
        mutedClients.add(clientId);
    }

    public void unmuteClient(long clientId) {
        mutedClients.remove(clientId);
    }

    private boolean isMuted(long clientId) {
        return mutedClients.contains(clientId);
    }


    public boolean isConnected() {
        if (server == null) {
            return false;
        }
        // https://stackoverflow.com/a/10241044
        // Note: these check the client's end of the socket connect; therefore they
        // don't really help determine
        // if the server had a problem
        return server.isConnected() && !server.isClosed() && !server.isInputShutdown() && !server.isOutputShutdown();

    }

    /**
     * Takes an ip address and a port to attempt a socket connection to a server.
     * 
     * @param address
     * @param port
     * @return true if connection was successful
     */
// New boolean connect added for Milestone 3
    public boolean connect(String address, int port, String username, IClientEvents callback) {
        clientName = username;
        Client.events = callback;
        try {
            server = new Socket(address, port);
            // channel to send to server
            out = new ObjectOutputStream(server.getOutputStream());
            // channel to listen to server
            in = new ObjectInputStream(server.getInputStream());
            logger.info("Client connected");
            listenForServerPayload();
            sendConnect();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return isConnected();
    }

    

    /**
     * <p>
     * Check if the string contains the <i>connect</i> command
     * followed by an ip address and port or localhost and port.
     * </p>
     * <p>
     * Example format: 123.123.123:3000
     * </p>
     * <p>
     * Example format: localhost:3000
     * </p>
     * https://www.w3schools.com/java/java_regex.asp
     * 
     * @param text
     * @return
     */
    private boolean isConnection(String text) {
        // https://www.w3schools.com/java/java_regex.asp
        return text.matches(ipAddressPattern)
                || text.matches(localhostPattern);
    }

    private boolean isQuit(String text) {
        return text.equalsIgnoreCase("/quit");
    }

    private boolean isName(String text) {
        if (text.startsWith("/name")) {
            String[] parts = text.split(" ");
            if (parts.length >= 2) {
                clientName = parts[1].trim();
                logger.info("Name set to " + clientName);
            }
            return true;
        }
        return false;
    }

    /**
     * Controller for handling various text commands.
     * <p>
     * Add more here as needed
     * </p>
     * 
     * @param text
     * @return true if a text was a command or triggered a command
     * @throws IOException 
     */
    private boolean processClientCommand(String text) throws IOException {
        if (isConnection(text)) {
            if (clientName.isBlank()) {
                logger.warning("You must set your name before you can connect via: /name your_name");
                return true;
            }
            // replaces multiple spaces with single space
            // splits on the space after connect (gives us host and port)
            // splits on : to get host as index 0 and port as index 1
            String[] parts = text.trim().replaceAll(" +", " ").split(" ")[1].split(":");
            connect(parts[0].trim(), Integer.parseInt(parts[1].trim()), text, null);
            return true;
        } else if (isQuit(text)) {
            isRunning = false;
            return true;
        } else if (isName(text)) {
            return true;
        } else if (text.startsWith(CREATE_ROOM)) {

            try {
                String roomName = text.replace(CREATE_ROOM, "").trim();
                sendCreateRoom(roomName);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return true;
        } else if (text.startsWith(JOIN_ROOM)) {

            try {
                String roomName = text.replace(JOIN_ROOM, "").trim();
                sendJoinRoom(roomName);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return true;
        } else if (text.startsWith(LIST_ROOMS)) {

            try {
                String searchQuery = text.replace(LIST_ROOMS, "").trim();
                sendListRooms(searchQuery);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return true;
        } else if (text.equalsIgnoreCase(LIST_USERS)) {
            logger.info("Users in Room: ");
            clientsInRoom.forEach(((t, u) -> {
                logger.info(String.format("%s - %s", t, u));
            }));
            return true;
        }
        else if (text.equalsIgnoreCase(DISCONNECT)) {
            try {
                sendDisconnect();
            }
            catch(Exception e){
              e.printStackTrace(); 
            }
            return true;
        }
        else if (text.equalsIgnoreCase(HELLO_COMMAND)) {
            try {
                sendHello();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return true;
        }
         else if (text.equalsIgnoreCase(FLIP_COMMAND)){
            try{
                sendFlip();
            } catch (IOException e){
                e.printStackTrace();
            }
            return true;
        }    
        else if (text.startsWith(ROLL_COMMAND)) {
            try {
                String rollText = text.replace(ROLL_COMMAND,"").trim();
                sendRoll(rollText);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return true;
        }
    // New Code Milestone 3 MS75 4-27-24 
    // Mute Feature 
        else if (text.startsWith(MUTE_COMMAND)) {
        String[] parts = text.split(" ");
        if (parts.length >= 2) {
            String clientName = parts[1].trim(); 
            long clientId = getClientIdByName(clientName);
            if (clientId != -1) {
                try {
                    sendMute(clientId);
                    return true;
                } catch (IOException e) {
                    e.printStackTrace();
                    return true;
                }
            } else {
                logger.warning("Client '" + clientName + "' not found.");
                return true;
            }
        } else {
            logger.warning("Invalid mute command format. Usage: /mute clientName");
            return true;
        }
    }
        
    return false;
}
    // Send methods

    // New Code Milestone 3 MS75 4-27-24 
    // Mute Feature 
    public void sendMute(long clientIdToMute) throws IOException {
        Payload p = new Payload();
        p.setPayloadType(PayloadType.MUTE);
        p.setMessage(String.valueOf(clientIdToMute)); // Convert client ID to string and set it as the message
        out.writeObject(p);
    }
    
    public void sendRoll(String rollText) throws IOException{
        Payload p = new Payload();
        p.setPayloadType(PayloadType.ROLL);
        p.setMessage(rollText);
        out.writeObject(p);
}

    private void sendHello() throws IOException {
        Payload p = new Payload();
        p.setPayloadType(PayloadType.HELLO);
        out.writeObject(p);
    }
    private void sendFlip() throws IOException {
        Payload p = new Payload();
        p.setPayloadType(PayloadType.FLIP);
        out.writeObject(p);
    }
    void sendDisconnect() throws IOException {
        ConnectionPayload cp = new ConnectionPayload();
        cp.setPayloadType(PayloadType.DISCONNECT);
        out.writeObject(cp);
    }

    public void sendCreateRoom(String roomName) throws IOException {
        Payload p = new Payload();
        p.setPayloadType(PayloadType.CREATE_ROOM);
        p.setMessage(roomName);
        out.writeObject(p);
    }

    public void sendJoinRoom(String roomName) throws IOException {
        Payload p = new Payload();
        p.setPayloadType(PayloadType.JOIN_ROOM);
        p.setMessage(roomName);
        out.writeObject(p);
    } 

    public void sendListRooms(String searchString) throws IOException {
        // Updated after video to use RoomResultsPayload so we can (later) use a limit
        // value
        RoomResultsPayload p = new RoomResultsPayload();
        p.setMessage(searchString);
        p.setLimit(10);
        out.writeObject(p);
    }

    private void sendConnect() throws IOException {
        ConnectionPayload p = new ConnectionPayload(true);

        p.setClientName(clientName);
        out.writeObject(p);
    }

    public void sendMessage(String message) throws IOException {
        Payload p = new Payload();
        p.setPayloadType(PayloadType.MESSAGE);
        p.setMessage(message);
        // no need to send an identifier, because the server knows who we are
        // p.setClientName(clientName);
        out.writeObject(p);
    } 

    // end send methods
    private void listenForKeyboard() {
        inputThread = new Thread() {
            @Override
            public void run() {
                logger.info("Listening for input");
                try (Scanner si = new Scanner(System.in);) {
                    String line = "";
                    isRunning = true;
                    while (isRunning) {
                        try {
                            logger.info("Waiting for input");
                            line = si.nextLine();
                            if (!processClientCommand(line)) {
                                if (isConnected()) {
                                    if (line != null && line.trim().length() > 0) {
                                        sendMessage(line);
                                    }

                                } else {
                                    logger.warning("Not connected to server");
                                }
                            }
                        } catch (Exception e) {
                            logger.severe("Connection dropped");
                            break;
                        }
                    }
                    logger.info("Exited loop");
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    close();
                }
            }
        };
        inputThread.start();
    }

    private void listenForServerPayload() {
        fromServerThread = new Thread() {
            @Override
            public void run() {
                try {
                    Payload fromServer;

                    // while we're connected, listen for strings from server
                    while (!server.isClosed() && !server.isInputShutdown()
                            && (fromServer = (Payload) in.readObject()) != null) {

                        logger.info("Debug Info: " + fromServer);
                        processPayload(fromServer);

                    }
                    logger.info("Loop exited");
                } catch (Exception e) {
                    e.printStackTrace();
                    if (!server.isClosed()) {
                        logger.severe("Server closed connection");
                    } else {
                        logger.severe("Connection closed");
                    }
                } finally {
                    close();
                    logger.info("Stopped listening to server input");
                }
            }
        };
        fromServerThread.start();// start the thread
    }

    private void addClientReference(long id, String name) {
        if (!clientsInRoom.containsKey(id)) {
            clientsInRoom.put(id, name);
        }
    }

    private void removeClientReference(long id) {
        if (clientsInRoom.containsKey(id)) {
            clientsInRoom.remove(id);
        }
    }

    protected String getClientNameFromId(long id) {
        if (clientsInRoom.containsKey(id)) {
            return clientsInRoom.get(id);
        }
        if (id == Constants.DEFAULT_CLIENT_ID) {
            return "[Room]";
        }
        return "[name not found]";
    }
    /**
     * Used to process payloads from the server-side and handle their data
     * 
     * @param p
     */
    private void processPayload(Payload p) {
        String message;
        switch (p.getPayloadType()) {
            case CLIENT_ID:
                if (myClientId == Constants.DEFAULT_CLIENT_ID) {
                    myClientId = p.getClientId();
                    addClientReference(myClientId, ((ConnectionPayload) p).getClientName());
                    logger.info(TextFX.colorize("My Client Id is " + myClientId, Color.GREEN));
                } else {
                    logger.info(TextFX.colorize("Setting client id to default", Color.RED));
                }
                events.onReceiveClientId(p.getClientId());
                break;
            case CONNECT:// for now connect,disconnect are all the same

            case DISCONNECT:
                ConnectionPayload cp = (ConnectionPayload) p;
                message = TextFX.colorize(String.format("*%s %s*",
                        cp.getClientName(),
                        cp.getMessage()), Color.YELLOW);
                logger.info(message);
            case SYNC_CLIENT:
                ConnectionPayload cp2 = (ConnectionPayload) p;
                if (cp2.getPayloadType() == PayloadType.CONNECT || cp2.getPayloadType() == PayloadType.SYNC_CLIENT) {
                    addClientReference(cp2.getClientId(), cp2.getClientName());

                } else if (cp2.getPayloadType() == PayloadType.DISCONNECT) {
                    removeClientReference(cp2.getClientId());
                }
                // TODO refactor this to avoid all these messy if condition (resulted from poor
                // planning ahead)
                if (cp2.getPayloadType() == PayloadType.CONNECT) {
                    events.onClientConnect(p.getClientId(), cp2.getClientName(), p.getMessage());
                } else if (cp2.getPayloadType() == PayloadType.DISCONNECT) {
                    events.onClientDisconnect(p.getClientId(), cp2.getClientName(), p.getMessage());
                } else if (cp2.getPayloadType() == PayloadType.SYNC_CLIENT) {
                    events.onSyncClient(p.getClientId(), cp2.getClientName());
                }

                break;
            case JOIN_ROOM:
                clientsInRoom.clear();// we changed a room so likely need to clear the list
                events.onResetUserList();
                break;
            case MESSAGE:
                // New Code Milestone 3 MS75 4-27-24 
                // Mute Feature and DM Feature
                if (!isMuted(p.getClientId())) {
                    String originalMessage = p.getMessage();
                    String[] messageParts = originalMessage.split("\\s+", 2); 
                    String recipientName = null;
                    String messageContent = originalMessage; 
    
                    if (messageParts.length > 1 && messageParts[0].startsWith("@")) {
                        recipientName = messageParts[0].substring(1); 
                        messageContent = messageParts[1]; 
                    }
    
                    if (recipientName != null) {
                        long recipientId = getClientIdByName(recipientName);
                        if (recipientId != -1) {
                            message = TextFX.colorize(String.format("(Private) %s: %s", getClientNameFromId(p.getClientId()), messageContent), Color.CYAN);
                            events.onMessageReceive(p.getClientId(), messageContent); 
                            events.onMessageReceive(recipientId, messageContent); 
                            return; 
                        } else {
                            message = TextFX.colorize(String.format("User '%s' not found", recipientName), Color.RED);
                            events.onMessageReceive(p.getClientId(), message); 
                            return; 
                        }
                    }
    
                    message = TextFX.colorize(String.format("%s: %s", getClientNameFromId(p.getClientId()), originalMessage), Color.BLUE);
                    events.onMessageReceive(p.getClientId(), originalMessage); 
                    System.out.println(message); 
                }
                break;
                // New Code Milestone 3 MS75 4-27-24 
                // Mute Feature and DM Feature
            case MUTE:
                String username = p.getMessage();
                logger.info("User '" + username + "' has been muted.");
                events.onMuteRecieve(p.getClientId(), p.getMessage());
                break;
            case LIST_ROOMS:
                try {
                    RoomResultsPayload rp = (RoomResultsPayload) p;
                    // if there's a message, print it
                    if (rp.getMessage() != null && !rp.getMessage().isBlank()) {
                        message = TextFX.colorize(rp.getMessage(), Color.RED);
                        logger.info(message);
                    }
                    // print room names found
                    List<String> rooms = rp.getRooms();
                    System.out.println(TextFX.colorize("Room Results", Color.CYAN));
                    for (int i = 0; i < rooms.size(); i++) {
                        String msg = String.format("%s %s", (i + 1), rooms.get(i));
                        System.out.println(TextFX.colorize(msg, Color.CYAN));
                    }
                    events.onReceiveRoomList(rp.getRooms(), rp.getMessage());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;   
//New case MS75 4-26-24
            case ROLL:
                message = TextFX.colorize(p.getMessage(), Color.CYAN);
                System.out.println(message);
                events.onRollReceive(p.getClientId(), p.getMessage());
                break;
            case FLIP:
                message = TextFX.colorize(p.getMessage(), Color.CYAN);
                System.out.println(message);
                events.onFlipReceive(p.getClientId(), p.getMessage());
                break;            
            default:
                break;

        }
    }

    
    private long getClientIdByName(String name) {
        for (Long id : clientsInRoom.keySet()) {
            if (clientsInRoom.get(id).equals(name)) {
                return id;
            }
        }
        return -1; // Return -1 if the client name is not found
    }
    public void start() throws IOException {
        listenForKeyboard();
    }

    private void close() {
        myClientId = Constants.DEFAULT_CLIENT_ID;
        clientsInRoom.clear();
        try {
            inputThread.interrupt();
        } catch (Exception e) {
            logger.severe("Error interrupting input");
            e.printStackTrace();
        }
        try {
            fromServerThread.interrupt();
        } catch (Exception e) {
            logger.severe("Error interrupting listener");
            e.printStackTrace();
        }
        try {
            logger.info("Closing output stream");
            out.close();
        } catch (NullPointerException ne) {
            logger.severe("Server was never opened so this exception is ok");
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            logger.info("Closing input stream");
            in.close();
        } catch (NullPointerException ne) {
            logger.severe("Server was never opened so this exception is ok");
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            logger.info("Closing connection");
            server.close();
            logger.severe("Closed socket");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NullPointerException ne) {
            logger.warning("Server was never opened so this exception is ok");
        }
    }

    public static void main(String[] args) {
        Client client = Client.INSTANCE; 

        try {
            client.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}