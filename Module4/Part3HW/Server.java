package Module4.Part3HW;


import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.HashMap;
import java.util.Map;


public class Server {
   int port = 3001;
   // connected clients
   private List<ServerThread> clients = new ArrayList<ServerThread>();
   private Map<Long, String> clientUsernames = new HashMap<>();

   private void start(int port) {
       this.port = port;
       // server listening
       try (ServerSocket serverSocket = new ServerSocket(port);) {
           Socket incoming_client = null;
           System.out.println("Server is listening on port " + port);
           do {
               System.out.println("waiting for next client");
               if (incoming_client != null) {
                   System.out.println("Client connected");
                   ServerThread sClient = new ServerThread(incoming_client, this);
                  
                   clients.add(sClient);
                   sClient.start();
                   incoming_client = null;
                  
               }
           } while ((incoming_client = serverSocket.accept()) != null);
       } catch (IOException e) {
           System.err.println("Error accepting connection");
           e.printStackTrace();
       } finally {
           System.out.println("closing server socket");
       }
   }
   protected synchronized void disconnect(ServerThread client) {
       long id = client.getId();
       client.disconnect();
       broadcast("Disconnected", id);
   }
  
   protected synchronized void broadcast(String message, long id) {
       if(processCommand(message, id)){

           return;
       }
       //new code begins MS75 2/21/24
       //
       //
       //
       //
       //
       if (message.startsWith("/cointoss")) {
        String[] parts = message.split(" ", 4);
        String playerName = parts[1];
        String result = parts[2];
        message = playerName + " flipped a coin and got " + result;
    } 
    if (message.startsWith("/pm" ) || message.startsWith("/pm" )) {
        String[] parts = message.split(" ", 4);
        String senderName = getClientUsername(id);
        String targetUsername = parts[1];
        String privateMessage = parts[2] + " " + senderName + ": " + parts[3];
        long targetClientId = findClientUsername(targetUsername);
        if (targetClientId != -1) {
            sendMessage(privateMessage, id, targetClientId);
            return;
        }
    }
    
    //
    //
    //
    //
    //new code ends MS75

        // let's temporarily use the thread id as the client identifier to
       // show in all client's chat. This isn't good practice since it's subject to
       // change as clients connect/disconnect
       message = String.format("User[%d]: %s", id, message);
       // end temp identifier
      
       // loop over clients and send out the message
       Iterator<ServerThread> it = clients.iterator();
       while (it.hasNext()) {
           ServerThread client = it.next();
           boolean wasSuccessful = client.send(message);
           if (!wasSuccessful) {
               System.out.println(String.format("Removing disconnected client[%s] from list", client.getId()));
               it.remove();
               broadcast("Disconnected", id);
           }
       }
   }

    
   //new  code begins MS75
   //
   //
   //
   // 
    protected synchronized void setClientUsername(long clientId, String username) {
        clientUsernames.put(clientId, username);
    }
    private String getClientUsername(long clientId) {
        return clientUsernames.get(clientId);
    }

    private void sendMessage(String privateMessage, long senderId, long targetId) {
        ServerThread sender = findClientID(senderId);
        ServerThread target = findClientID(targetId);
    
        if (sender != null && target != null) {
            sender.send("Sent a private message to " + clientUsernames.get(targetId));
            target.send(clientUsernames.get(senderId) + " sent you a private message: " + privateMessage);
        }
    }
    
    private ServerThread findClientID(long id) {
        for (ServerThread client : clients) {
            if (client.getId() == id) {
                return client;
            }
        }
        return null;
    }   
    private long findClientUsername(String username) {
        for (ServerThread client : clients) {
            if (clientUsernames.containsKey(client.getId()) && clientUsernames.get(client.getId()).equals(username)) {
                return client.getId();
            }
        }
        return -1;
    }
    //
    //
    //
    //
    //new code ends MS75
    
    private boolean processCommand(String message, long clientId){
        System.out.println("Checking command: " + message);
        if(message.equalsIgnoreCase("disconnect")){
            Iterator<ServerThread> it = clients.iterator();
            while (it.hasNext()) {
                ServerThread client = it.next();
                if(client.getId() == clientId){
                    it.remove();
                    disconnect(client);
                                 
                    break;
                }
            }
            return true;
        }
        return false;
    }
    public static void main(String[] args) {
        System.out.println("Starting Server");
        Server server = new Server();
        int port = 3000;
        try {
            port = Integer.parseInt(args[0]);
        } catch (Exception e) {
            // can ignore, will either be index out of bounds or type mismatch
            // will default to the defined value prior to the try/catch
        }
        server.start(port);
        System.out.println("Server Stopped");
    }
 }
