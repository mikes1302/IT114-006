package Project.Server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;
import java.util.logging.Logger;

import Project.Common.ConnectionPayload;
import Project.Common.Constants;
import Project.Common.Payload;
import Project.Common.PayloadType;
import Project.Common.RoomResultsPayload;
import Project.Common.TextFX;
import Project.Common.TextFX.Color;

/**
 * A server-side representation of a single client
 */
public class ServerThread extends Thread {
    private Socket client;
    private String clientName;
    private boolean isRunning = false;
    private long clientId = Constants.DEFAULT_CLIENT_ID;
    private ObjectOutputStream out;// exposed here for send()
    // private Server server;// ref to our server so we can call methods on it
    // more easily
    private Room currentRoom;
    private Logger logger = Logger.getLogger(ServerThread.class.getName());

    private void info(String message) {
        logger.info(String.format("Thread[%s]: %s", getClientName(), message));
    }

    public ServerThread(Socket myClient/* , Room room */) {
        info("Thread created");
        // get communication channels to single client
        this.client = myClient;
        // this.currentRoom = room;

    }

    protected void setClientId(long id) {
        clientId = id;
        if (id == Constants.DEFAULT_CLIENT_ID) {
            logger.info(TextFX.colorize("Client id reset", Color.WHITE));
        }
        sendClientId(id);
    }

    protected boolean isRunning() {
        return isRunning;
    }
    protected void setClientName(String name) {
        if (name == null || name.isBlank()) {
            logger.severe("Invalid client name being set");
            return;
        }
        clientName = name;
    }

    protected String getClientName() {
        return clientName;
    }

    protected synchronized Room getCurrentRoom() {
        return currentRoom;
    }

    protected synchronized void setCurrentRoom(Room room) {
        if (room != null) {
            currentRoom = room;
        } else {
            info("Passed in room was null, this shouldn't happen");
        }
    }

    public void disconnect() {
        info("Thread being disconnected by server");
        isRunning = false;
        cleanup();
    }

    // send methods
    protected boolean sendClientMapping(long id, String name) {
        ConnectionPayload cp = new ConnectionPayload();
        cp.setPayloadType(PayloadType.SYNC_CLIENT);
        cp.setClientId(id);
        cp.setClientName(name);
        return send(cp);
    }

    protected boolean sendJoinRoom(String roomName) {
        Payload p = new Payload();
        p.setPayloadType(PayloadType.JOIN_ROOM);
        p.setMessage(roomName);
        return send(p);
    }

    protected boolean sendClientId(long id) {
        ConnectionPayload cp = new ConnectionPayload();
        cp.setClientId(id);
        cp.setClientName(clientName);
        return send(cp);
    }
    private boolean sendListRooms(List<String> potentialRooms) {
        RoomResultsPayload rp = new RoomResultsPayload();
        rp.setRooms(potentialRooms);
        if (potentialRooms == null) {
            rp.setMessage("Invalid limit, please choose a value between 1-100");
        } else if (potentialRooms.size() == 0) {
            rp.setMessage("No rooms found matching your search criteria");
        }
        return send(rp);
    }

    public boolean sendMessage(long from, String message) {
        Payload p = new Payload();
        p.setPayloadType(PayloadType.MESSAGE);
        // p.setClientName(from);
        p.setClientId(from);
        p.setMessage(message);
        return send(p);
    }

    /**
     * Used to associate client names and their ids from the server perspective
     * 
     * @param whoId       id of who is connecting/disconnecting
     * @param whoName     name of who is connecting/disconnecting
     * @param isConnected status of connection (true connecting, false,
     *                    disconnecting)
     * @return
     */
    public boolean sendConnectionStatus(long whoId, String whoName, boolean isConnected) {
        ConnectionPayload p = new ConnectionPayload(isConnected);
        // p.setClientName(who);
        p.setClientId(whoId);
        p.setClientName(whoName);
        p.setMessage(isConnected ? "connected" : "disconnected");
        return send(p);
    }

    private boolean send(Payload payload) {
        // added a boolean so we can see if the send was successful
        try {
            out.writeObject(payload);
            return true;
        } catch (IOException e) {
            info("Error sending message to client (most likely disconnected)");
            // comment this out to inspect the stack trace
            // e.printStackTrace();
            cleanup();
            return false;
        } catch (NullPointerException ne) {
            info("Message was attempted to be sent before outbound stream was opened");
            return true;// true since it's likely pending being opened
        }
    }

    // end send methods
    @Override
    public void run() {
        info("Thread starting");
        try (ObjectOutputStream out = new ObjectOutputStream(client.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(client.getInputStream());) {
            this.out = out;
            isRunning = true;
            Payload fromClient;
            while (isRunning && // flag to let us easily control the loop
                    (fromClient = (Payload) in.readObject()) != null // reads an object from inputStream (null would
                                                                     // likely mean a disconnect)
            ) {

                info("Received from client: " + fromClient);
                processPayload(fromClient);

            } // close while loop
        } catch (Exception e) {
            // happens when client disconnects
            e.printStackTrace();
            info("Client disconnected");
        } finally {
            isRunning = false;
            info("Exited thread loop. Cleaning up connection");
            cleanup();
        }
    }

    /**
     * Used to process payloads from the client and handle their data
     * 
     * @param p
     */
    private void processPayload(Payload p) {
        switch (p.getPayloadType()) {
            case CONNECT:
                try {
                    ConnectionPayload cp = (ConnectionPayload) p;
                    setClientName(cp.getClientName());
                } catch (Exception e) {
                    e.printStackTrace();
                }

                break;
            case DISCONNECT:
                if (currentRoom != null) {
                    Room.disconnectClient(this, currentRoom);
                }
                break;
            case MESSAGE:
                if (currentRoom != null) {
//  New Code Begins
//  MS75
//  2-4-24
//  Here I altered the MESSAGE case so all messages typed by a client will be scanned for special characters
                    String processedMessage = messageProcessor(p.getMessage());
                    currentRoom.sendMessage(this, processedMessage);                } 
                    else {
//  New Code Ends
//  MS75
//  2-4-24 
                    // TODO migrate to lobby
                    Room.joinRoom(Constants.LOBBY, this);
                }
                break;
            case CREATE_ROOM:
                Room.createRoom(p.getMessage(), this);
                break;
            case JOIN_ROOM:
                Room.joinRoom(p.getMessage(), this);
                break;
            case LIST_ROOMS:
                String searchString = p.getMessage() == null ? "" : p.getMessage();
                int limit = 10;
                try {
                    RoomResultsPayload rp = ((RoomResultsPayload) p);
                    limit = rp.getLimit();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                List<String> potentialRooms = Room.listRooms(searchString, limit);
                this.sendListRooms(potentialRooms);
                break;
  //    New Code Begins
  //    MS75
  //    2-3-24
  //        Here I added the Cases for Payload Types FLIP, ROLL and HELLO.
  //        I used HELLO as a starter to help me understand client and server side interactions.
            case HELLO:
                currentRoom.sendMessage(this, "hello, hello, hello");
                break;
            case FLIP:
                String flip = flip();
                currentRoom.sendMessage(this, flip);
                
                break;
            case ROLL:
                String roll = roll(p.getMessage());
               currentRoom.sendMessage(this,roll);
                break;
//  New Code Ends
//  MS75
//  2-3-24
            default:
                break;

        }

    }
//  New Code Begins
//  MS75
//  2-3-24
//      Flip method created that uses random number genrator to generate variable integer "X,"
//      and if the number equals 0 then the string message uses Heads if it equals 1 then Tails. 
private String flip() {
    int x = (int) (Math.random() * 2); 
    String result = (x == 0) ? "Heads" : "Tails";
    return String.format(clientName+" flipped a coin and got: " +result);
}
//      Roll method created that first trims the String from switch case ROLL (line 259) and removes the command "/roll"
//      Then the remainder string is split into an array of string which gets tested in an if else statment to see if the command
//      format was met and if met for the first format a random number from 0 - the upper range sent by client is sent into a string message.
//      If the second format is met then a for loop is used to calculate and append the output string message with the values of the rolled dice.
//      The return type is String because I had issues having a integer or object return type because the second paramater for sendMessage() is a String.

    private String roll(String roll) {
        roll = roll.trim().substring("/roll".length()).trim(); 
        String[] parts = roll.split("\\s+");
    
        StringBuilder newString = new StringBuilder();
    
        if (parts.length == 1 && parts[0].matches("\\d+")) {
            int result = (int) (Math.random() * (Integer.parseInt(parts[0]) + 1));
            newString.append("Rolled a ").append(result);
        } else if (parts.length == 2 && parts[0].matches("\\d+") && parts[1].matches("\\d+d\\d+")) {
            String[] diceParams = parts[1].split("d");
            int diceCount = Integer.parseInt(diceParams[0]);
            int faceCount = Integer.parseInt(diceParams[1]);
            int total = 0;
            newString.append("Rolled ");
            for (int i = 0; i < diceCount; i++) {
                int rollResult = (int) (Math.random() * faceCount) + 1;
                newString.append(rollResult);
                total += rollResult;
                if (i < diceCount - 1) {
                    newString.append(", ");
                }
            }
            newString.append(" for a total of ").append(total);
        } else {
            newString.append("Incorrect Format. Use : '/roll x' or '/roll xdy'.");
        }
    
        return newString.toString();
    }
    
//      messageProcessor() method is created with a single string paramater apart of the switch case MESSAGE.
//      First this method checks and replaces all text enclosed between *asteriks* with html tags <br></br>
//      Second the method checks and replaces all text enclosed between -hyphens- with <i></i>  
//      Third this method checls and replaces all text enclosed between _underscores_ with <u></u>
//      Fourth the method checks and replaces all text enclosed between #rcolorr# with <font color='red'></font> as well as blue and green
//      Lastly the method checks and replaces all text enclosed between *-_#rmultiple#_0* special characters with the corresponding tags
    private String messageProcessor(String message) {
        message = message.replaceAll("\\*(.*?)\\*", "<b>$1</b>");

        message = message.replaceAll("\\-(.*?)\\-", "<i>$1</i>");

        message = message.replaceAll("\\_(.*?)\\_", "<u>$1</u>");

        message = message.replaceAll("\\#r(.*?)\\#", "<font color='red'>$1</font>");
        message = message.replaceAll("\\#b(.*?)\\#", "<font color='blue'>$1</font>");
        message = message.replaceAll("\\#g(.*?)\\#", "<font color='green'>$1</font>");

        message = message.replaceAll("\\*\\-(.*?)\\-\\*", "<b><i><u>$1</u></i></b>");
        message = message.replaceAll("\\-\\*(.*?)\\*\\-", "<i><u><font color='red'>$1</font></u></i>");

        return message;
    }
//  New Code Ends
//  MS75
//  2-4-23
    private void cleanup() {
        info("Thread cleanup() start");
        try {
            client.close();
        } catch (IOException e) {
            info("Client already closed");
        }
        info("Thread cleanup() complete");
    }

    public long getClientId() {
        return clientId;
    }
}