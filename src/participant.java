import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;

public class participant {

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

            File log = new File(logFile);
            log.delete();

            //create a new thread for receiving user commands
            ReceiveCommand commandThread = new ReceiveCommand(ip, Integer.parseInt(port), particpantID, log);
            commandThread.start();


        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}

class ReceiveCommand extends Thread {
    Scanner scanner = new Scanner(System.in);
    ReceiveMulticast receiver;
    String id;
    File log;
    String ip;
    int port;

    //Constructor
    public ReceiveCommand(String ip, int port, String id, File log) {
        try {
            this.ip = ip;
            this.port = port;
            this.id = id;
            this.log = log;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void run() {
        while (true) {
            try (Socket socket = new Socket(ip, port)) {
                System.out.print(id + "> ");
                String line = scanner.nextLine().trim();
                String[] commands = line.split(" ");
                String command = commands[0];
                DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
                DataInputStream inputStream = new DataInputStream(socket.getInputStream());

                if (command.equals("register")) {
                    if (receiver != null) {
                        System.out.println("receiver is already registered, deregister before registering again");
                        continue;
                    }
                    int port = Integer.parseInt(commands[1]);
                    //create a new thread for receiving multicast messages from the coordinator
                    receiver = new ReceiveMulticast(log);
                    receiver.connect(port);
                    outputStream.writeUTF(command);
                    outputStream.writeUTF(id);
                    outputStream.writeInt(port);
                    String ack = inputStream.readUTF();
                    System.out.println(ack);
                } else if (command.equals("deregister")) {
                    if (receiver == null) {
                        System.out.println("receiver is not registered, register first");
                        continue;
                    }
                    outputStream.writeUTF(command);
                    outputStream.writeUTF(id);
                    receiver.disconnect();
                    receiver = null;
                    String ack = inputStream.readUTF();
                    System.out.println(ack);
                } else if (command.equals("disconnect")) {
                    if (receiver == null) {
                        System.out.println("receiver is not registered, register first");
                        continue;
                    } else if (!receiver.isRunning()) {
                        System.out.println("receiver has already disconnected");
                        continue;
                    }
                    outputStream.writeUTF(command);
                    outputStream.writeUTF(id);
                    receiver.disconnect();
                    String ack = inputStream.readUTF();
                    System.out.println(ack);
                } else if (command.equals("reconnect")) {
                    if (receiver == null) {
                        System.out.println("receiver is not registered, register first");
                        continue;
                    } else if (receiver.isRunning()) {
                        System.out.println("receiver already connected, disconnect first");
                        continue;
                    }
                    int port = Integer.parseInt(commands[1]);
                    receiver.connect(port);
                    outputStream.writeUTF(command);
                    outputStream.writeUTF(id);
                    outputStream.writeInt(port);
                    String ack = inputStream.readUTF();
                    System.out.println(ack);
                } else if (command.equals("msend")) {
                    if (receiver == null) {
                        System.out.println("receiver is not registered, register first");
                        continue;
                    } else if (!receiver.isRunning()) {
                        System.out.println("receiver is disconnected, reconnect first");
                        continue;
                    }
                    outputStream.writeUTF(command);
                    outputStream.writeUTF(id);
                    outputStream.writeUTF(line.substring(6));
                    String ack = inputStream.readUTF();
                    System.out.println(ack);
                } else if (command.equals("quit")) {
                    outputStream.writeUTF(command);
                    outputStream.writeUTF(id);
                    System.exit(0);
                } else {
                    System.out.println("Command doesn't exist, please try again");
                }
            } catch (Exception ignored) {
                System.out.println("Please pay attention to the number and validity of command arguments");
            }
        }
    }
}

class ReceiveMulticast {
    ServerSocket socket = null;
    boolean running = false;
    Thread thread;
    File log;

    //Constructor
    public ReceiveMulticast(File log) {
        this.log = log;
    }

    public void connect(int port) throws IOException {
        if (socket != null) {
            socket.close();
        }
        socket = new ServerSocket(port);
        running = true;
        thread = new Thread(() -> {
            while (true) {
                try (Socket receiver = socket.accept()) {
                    DataInputStream inputStream = new DataInputStream(receiver.getInputStream());
                    String message = inputStream.readUTF();
                    BufferedWriter bw = new BufferedWriter(new FileWriter(log, true));
                    bw.write(message);
                    bw.newLine();
                    bw.close();

                    //Files.writeString(log.toPath(), message+ "\n", CREATE, APPEND);
                } catch (Exception ignored) {
                }
            }
        });
        thread.start();
    }

    public boolean isRunning() {
        return running;
    }

    public void disconnect() throws IOException {
        running = false;
        thread.interrupt();
        socket.close();
    }
}