package Project.Server;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import Project.Common.Constants;

public class Room implements AutoCloseable {
    // protected static Server server;// used to refer to accessible server
    // functions
    private String name;
    private List<ServerThread> clients = new ArrayList<ServerThread>();

    private boolean isRunning = false;
    // Commands
    private final static String COMMAND_TRIGGER = "/";
    // private final static String CREATE_ROOM = "createroom";
    // private final static String JOIN_ROOM = "joinroom";
    // private final static String DISCONNECT = "disconnect";
    // private final static String LOGOUT = "logout";
    // private final static String LOGOFF = "logoff";
    private Logger logger = Logger.getLogger(Room.class.getName());

    public Room(String name) {
        this.name = name;
        isRunning = true;
    }

    private void info(String message) {
        logger.info(String.format("Room[%s]: %s", name, message));
    }

    public String getName() {
        return name;
    }

    protected synchronized void addClient(ServerThread client) {
        if (!isRunning) {
            return;
        }
        client.setCurrentRoom(this);
        client.sendJoinRoom(getName());// clear first
        if (clients.indexOf(client) > -1) {
            info("Attempting to add a client that already exists");
        } else {
            clients.add(client);
            // connect status second
            sendConnectionStatus(client, true);
            syncClientList(client);
        }


    }

    protected synchronized void removeClient(ServerThread client) {
        if (!isRunning) {
            return;
        }
        clients.remove(client);
        // we don't need to broadcast it to the server
        // only to our own Room
        if (clients.size() > 0) {
            // sendMessage(client, "left the room");
            sendConnectionStatus(client, false);
        }
        checkClients();
    }

    /***
     * Checks the number of clients.
     * If zero, begins the cleanup process to dispose of the room
     */
    private void checkClients() {
        // Cleanup if room is empty and not lobby
        if (!name.equalsIgnoreCase(Constants.LOBBY) && clients.size() == 0) {
            close();
        }
    }

    /***
     * Helper function to process messages to trigger different functionality.
     * 
     * @param message The original message being sent
     * @param client  The sender of the message (since they'll be the ones
     *                triggering the actions)
     */
    private boolean processCommands(String message, ServerThread client) {
        boolean wasCommand = false;
        try {
            if (message.startsWith(COMMAND_TRIGGER)) {
                String[] comm = message.split(COMMAND_TRIGGER);
                String part1 = comm[1];
                String[] comm2 = part1.split(" ");
                String command = comm2[0];
                // String roomName;
                wasCommand = true;
                switch (command) {
/*/  New Code Starts 
//  MS75
//  4-2-24
                    case "hello":
                    sendMessage(client, "hello, hello, hello");
                    break;
                    case "flip":
                    flip(client);
                    break;
                    case "roll": 
                    if (comm2.length > 1) {
                        roll(client, comm2[1]);
                    } else {
                        sendMessage(client, "type /roll n or ndn where n is a number ");
//                 logger.warning(String.format("type /roll n or ndn where n is a number"));

                    }
                    break;
//
//
//  New Code Ends  */ 
                    /*
                     * case CREATE_ROOM:
                     * roomName = comm2[1];
                     * Room.createRoom(roomName, client);
                     * break;
                     * case JOIN_ROOM:
                     * roomName = comm2[1];
                     * Room.joinRoom(roomName, client);
                     * break;
                     */
                    /*
                     * case DISCONNECT:
                     * case LOGOUT:
                     * case LOGOFF:
                     * Room.disconnectClient(client, this);
                     * break;
                     */
                    default:
                        wasCommand = false;
                        break;
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return wasCommand;
    }

    // Command helper methods
    private synchronized void syncClientList(ServerThread joiner) {
        Iterator<ServerThread> iter = clients.iterator();
        while (iter.hasNext()) {
            ServerThread st = iter.next();
            if (st.getClientId() != joiner.getClientId()) {
                joiner.sendClientMapping(st.getClientId(), st.getClientName());
            }
        }
    }
    protected static void createRoom(String roomName, ServerThread client) {
        if (Server.INSTANCE.createNewRoom(roomName)) {
            // server.joinRoom(roomName, client);
            Room.joinRoom(roomName, client);
        } else {
            client.sendMessage(Constants.DEFAULT_CLIENT_ID, String.format("Room %s already exists", roomName));
        }
    }

    protected static void joinRoom(String roomName, ServerThread client) {
        if (!Server.INSTANCE.joinRoom(roomName, client)) {
            client.sendMessage(Constants.DEFAULT_CLIENT_ID, String.format("Room %s doesn't exist", roomName));
        }
    }

    protected static List<String> listRooms(String searchString, int limit) {
        return Server.INSTANCE.listRooms(searchString, limit);
    }

    protected static void disconnectClient(ServerThread client, Room room) {
        client.setCurrentRoom(null);
        client.disconnect();
        room.removeClient(client);
    }
    // end command helper methods

    /***
     * Takes a sender and a message and broadcasts the message to all clients in
     * this room. Client is mostly passed for command purposes but we can also use
     * it to extract other client info.
     * 
     * @param sender  The client sending the message
     * @param message The message to broadcast inside the room
     */
    protected synchronized void sendMessage(ServerThread sender, String message) {
        if (!isRunning) {
            return;
        }
        info("Sending message to " + clients.size() + " clients");
        if (sender != null && processCommands(message, sender)) {
            // it was a command, don't broadcast
            return;
        }

        /// String from = (sender == null ? "Room" : sender.getClientName());
        long from = (sender == null) ? Constants.DEFAULT_CLIENT_ID : sender.getClientId();
        Iterator<ServerThread> iter = clients.iterator();
        while (iter.hasNext()) {
            ServerThread client = iter.next();
            boolean messageSent = client.sendMessage(from, message);
            if (!messageSent) {
                handleDisconnect(iter, client);
            }
        }
    }

    protected synchronized void sendConnectionStatus(ServerThread sender, boolean isConnected) {
        Iterator<ServerThread> iter = clients.iterator();
        while (iter.hasNext()) {
            ServerThread client = iter.next();
            boolean messageSent = client.sendConnectionStatus(sender.getClientId(), sender.getClientName(),
                    isConnected);
            if (!messageSent) {
                handleDisconnect(iter, client);
            }
        }
    }

    private void handleDisconnect(Iterator<ServerThread> iter, ServerThread client) {
        iter.remove();
        info("Removed client " + client.getClientName());
        checkClients();
        sendMessage(null, client.getClientName() + " disconnected");
    }
/*/  New Code Starts
//  MS75
//  4-2-24
private void flip(ServerThread client) {
    boolean isHeads = Math.random() < 0.5;
    String flip = isHeads ? "Heads" : "Tails";
    sendMessage(client, flip);
}

    private void roll(ServerThread client, String param) {
        if (param.matches("\\d+d\\d+")) { 
            String[] parts = param.split("d");
            int numberOfDice = Integer.parseInt(parts[0]);
            int faces = Integer.parseInt(parts[1]);
            int total = 0;
            StringBuilder result = new StringBuilder();
            result.append("Rolled ");
            for (int i = 0; i < numberOfDice; i++) {
                int roll = (int) (Math.random() * faces) + 1;
                result.append(roll);
                total += roll;
                if (i < numberOfDice - 1) {
                    result.append(", ");
                }
            }
            result.append(" for a total of ").append(total);
            sendMessage(client, result.toString());
        } else {
            int end;
            try {
                end = Integer.parseInt(param);
            } catch (NumberFormatException e) {
            logger.warning(String.format("\"Wrong format type /roll n where n is a number or ndn to roll dice where the first number\"\n" +
                                " is the number of dice and the second number is the face on the dices.\""));
                return;
            }
            if (end <= 0) {
                logger.warning(String.format("Please use a number greater than 0"));
                return;
            }
    
            int value = (int) (Math.random() * (end + 1));
            sendMessage(client, String.format("Rolled a %d", value));
        }
    }
    

//
//
//  New Code Ends */
    public void close() {
        Server.INSTANCE.removeRoom(this);
        // server = null;
        isRunning = false;
        clients = null;
    }
}