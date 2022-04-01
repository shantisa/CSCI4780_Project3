import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class coordinator {
    ServerSocket server = null;
    Map<String, CommandParser.Participant> registry = new HashMap<>();

    public static void main(String[] args) {
        try {
            coordinator serverSocket = new coordinator();
            serverSocket.connection(args[0]);
        } catch (IOException ignored) {
        }
    }

    // connection is a function that establishes a connection with the client
    public void connection(String file) throws IOException {
        try {
            File configFile = new File(file);
            Scanner scanner = new Scanner(configFile);

            int port = Integer.parseInt(scanner.next());
            long timeout = Integer.parseInt(scanner.next()) * 1000L;
            server = new ServerSocket(port);

            System.out.print("Coordinator is Running... ");

            while (true) {
                Socket socket = server.accept();
                CommandParser thread = new CommandParser(socket, registry, timeout);
                thread.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
            server.close();
        }
    }


}

class CommandParser extends Thread {
    Socket socket = null;
    DataOutputStream outputStream = null;
    DataInputStream inputStream = null;
    Map<String, Participant> registry;
    long timeout;

    //Constructor
    public CommandParser(Socket socket, Map<String, Participant> registry, long timeout) {
        try {
            this.socket = socket;
            outputStream = new DataOutputStream(socket.getOutputStream());
            inputStream = new DataInputStream(socket.getInputStream());
            this.registry = registry;
            this.timeout = timeout;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void run() {
        try {
            String command = inputStream.readUTF();
            String id = inputStream.readUTF();

            if(command.equals("register")){
                int port = inputStream.readInt();
                if(registry.get(id) != null){
                    System.out.println("receiver is already registered");
                }else{
                    Participant participant = new Participant(port, socket.getInetAddress());
                    registry.put(id,participant);
                    outputStream.writeUTF("participant is registered");
                }
            }else if(command.equals("deregister")){
                if(registry.get(id) == null){
                    System.out.println("receiver is not registered");
                }
                else{
                    registry.remove(id);
                    outputStream.writeUTF("participant is deregistered");
                }
            } else if(command.equals("disconnect")){
                Participant state = registry.get(id);
                if(state == null){
                    System.out.println("receiver is not registered");
                }
                else{
                    state.dc();
                    outputStream.writeUTF("participant is disconnected");
                }
            } else if(command.equals("reconnect")){
                int port = inputStream.readInt();
                Participant state = registry.get(id);
                if(state == null){
                    System.out.println("receiver is not registered");
                }
                else{
                    state.reconnect(port);
                    outputStream.writeUTF("participant is reconnected");
                }
            }else if(command.equals("msend")){
                Participant state = registry.get(id);
                if(state == null){
                    System.out.println("receiver is not registered, you cannot send a message");
                }
                long now = System.currentTimeMillis();
                String message = inputStream.readUTF();
                outputStream.writeUTF("message acknowledged");
                for(Map.Entry<String, Participant> entry : registry.entrySet()){
                    Participant participant = entry.getValue();
                    if(participant != null){
                        participant.handleMessage(now, timeout, message);
                    }
                }
            } else if(command.equals("quit")){
                registry.remove(id);
            } else{
                System.out.println("Command doesn't exist, please try again");
            }
        } catch (Exception e){
        }
    }

    static class Participant {
        private Integer port;
        private boolean online;
        private Long dcTime;
        final private InetAddress ip;
        private List<String> savedMessages = new ArrayList<>();

        public Participant(int port, InetAddress ip){
            this.ip = ip;
            this.port = port;
            dcTime = null;
            online = true;
        }

        public void dc(){
            dcTime = System.currentTimeMillis();
            port = null;
            online = false;
        }

        public void reconnect(int port){
            this.port = port;
            dcTime = null;
            online = true;
            for(String message : savedMessages){
                sendMessage(message);
            }
            savedMessages = new ArrayList<>();
        }

        public void handleMessage(long now ,long timeout, String message){
            if(online){
                sendMessage(message);
            }
            else if(keepAliveValid(now,timeout)){
                saveMessage(message);
            }
        }

        private void sendMessage(String message){
            new Thread(() -> {
                try (Socket socket = new Socket(ip, port)) {
                    DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
                    outputStream.writeUTF(message);
                } catch (IOException ignored) {
                }
            }).start();
        }

        private boolean keepAliveValid(long now, long timeout){
            if(dcTime == null){
                return false;
            }
            long diff = now - dcTime;
            return 0 <= diff && diff <= timeout;
        }

        private void saveMessage(String message){
            savedMessages.add(message);
        }
    }
}
