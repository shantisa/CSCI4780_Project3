import java.io.*;
import java.net.*;
import java.util.*;

public class participant {
    //initialize socket
    Socket socket = null;

    public static void main(String[] args) {
        try {
            participant participantSocket = new participant();
            participantSocket.connectCoord(args[0]);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void connectCoord(String file) throws IOException {
        try {
            File configFile = new File(file);
            Scanner scanner = new Scanner(configFile);

            String particpantID = scanner.nextLine();
            String logFile = scanner.nextLine();
            String ip = scanner.next();
            String port = scanner.next();

            //establish connection to coordinator
            socket = new Socket(ip, Integer.parseInt(port));


            //create a new thread for receiving user commands
            ReceiveCommand commandThread = new ReceiveCommand(socket);
            commandThread.start();

            //create a new thread for receiving multicast messages from the coordinator
            ReceiveMulticast multicastThread = new ReceiveMulticast(socket);
            multicastThread.start();


        } catch (Exception e){
            socket.close();
            e.printStackTrace();
        }

    }

}

class ReceiveCommand extends Thread {
    Socket socket = null;
    DataInputStream inputStream = null;
    DataOutputStream outputStream = null;

    //Constructor
    public ReceiveCommand(Socket socket) {
        try {
            this.socket = socket;
            inputStream = new DataInputStream(socket.getInputStream());
            outputStream = new DataOutputStream(socket.getOutputStream());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void run() {
        try {
            while (true) {
                String command = inputStream.readUTF();

                if(command.equals("register")){

                }else if(command.equals("deregister")){

                } else if(command.equals("disconnect")){

                } else if(command.equals("reconnect")){

                }else if(command.equals("msend")){

                }else{
                    System.out.println("Command doesn't exist, please try again");
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}

class ReceiveMulticast extends Thread {
    Socket socket = null;
    DataInputStream inputStream = null;
    DataOutputStream outputStream = null;

    //Constructor
    public ReceiveMulticast(Socket socket) {
        try {
            this.socket = socket;
            inputStream = new DataInputStream(socket.getInputStream());
            outputStream = new DataOutputStream(socket.getOutputStream());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}