import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server implements Runnable {
    private ArrayList<ConnectionHandler> connections;
    private ServerSocket server;
    private boolean finished;
    private ExecutorService pool;
    public Server() {
        connections = new ArrayList<>();
        finished = false;
    }
    @Override
    public void run() {
        try {
            server = new ServerSocket(9999);
            pool = Executors.newCachedThreadPool();
            while (!finished) {
                Socket client = server.accept();
                ConnectionHandler handler = new ConnectionHandler(client);
                connections.add(handler);
                pool.execute(handler);
            }
        } catch (IOException e) {
            shutdown();
        }
    }

    // broadcast newly joined username
    public void broadcast(String message) {
        for (ConnectionHandler chandler: connections){
            if (chandler != null) {
                chandler.sendMessage(message);
            }
        }
    }

    public void shutdown() {
        if(!(server.isClosed())) {
            finished = true;
            try {
                server.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }
    class ConnectionHandler implements Runnable {
        private Socket client;
        private BufferedReader in; // read from client
        private PrintWriter out; // write something to client
        private String username; //
        public ConnectionHandler(Socket client) {
            this.client = client;
        }
        @Override
        public void run() {
            try {
                out = new PrintWriter(client.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                out.println("Enter your name: ");
                username = in.readLine();
                System.out.println(username + " Connected!");
                broadcast(username + " joined the chat!");
                String message;
                while ((message = in.readLine()) != null){
                    if (message.startsWith("/chuname ")) {
                        String[] messageSplit = message.split(" ", 2);
                        if (messageSplit.length == 2){
                            broadcast(username + " changed themselves to " + messageSplit[1]);
                            System.out.println(username + " changed themselves to " + messageSplit[1]);
                            username = messageSplit[1];
                            out.println("Successuflly changed username to " + username);
                        }else {
                            out.println("No username provided!");
                        }

                    } else if (message.startsWith("/quit")){
                        broadcast(username + " left the chat!");
                        shutdown();
                    }
                    else {
                        broadcast(username + " : " + message);
                    }
                }
            } catch (IOException e) {
                shutdown();
            }

        }
        public void sendMessage (String message) {
            out.println(message);
        }

        public void shutdown() {
            try {
                in.close();
                out.close();
                if(!client.isClosed()) {
                    client.close();
                }
            } catch (IOException e) {
                // ignore
            }
        }
    }
    public static void main (String[] arg) {
        Server server = new Server();
        server.run();
    }

}
