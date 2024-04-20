import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;

public class Client implements Runnable {
    private DatagramSocket socket;
    private InetAddress serverAddress;
    private int serverPort;
    private Timer timer;
    private int currentSequenceNumber = 0;

    private static final int WINDOW_SIZE = 5;

    private Queue<String> messageQueue = new LinkedList<>();

    public Client(String address, int port) throws Exception {
        this.socket = new DatagramSocket();
        this.serverAddress = InetAddress.getByName(address);
        this.serverPort = port;
        this.timer = new Timer();
    }

    public void send(String message) throws Exception {
        messageQueue.add(message);
        sendNextMessages();
    }

    private void sendNextMessages() throws Exception {
        while (!messageQueue.isEmpty() && currentSequenceNumber < currentSequenceNumber + WINDOW_SIZE) {
            if (true) {
                String message = messageQueue.poll();
                byte[] messageBytes = message.getBytes();
                DatagramPacket packet = new DatagramPacket(messageBytes, messageBytes.length, serverAddress, serverPort);
                socket.send(packet);
                System.out.println("Sent packet with sequence number: " + currentSequenceNumber);
                startTimer(packet);
                currentSequenceNumber++;
            } else {
                System.out.println("Window is full. Cannot send packet now.");
                break;
            }
        }
    }

    private void startTimer(DatagramPacket packet) {
        timer.schedule(new TimerTask() {
            public void run() {
                try {
                    System.out.println("Timeout! Retransmitting packet with sequence number: " + currentSequenceNumber);
                    socket.send(packet);
                } catch (Exception e) {
                    System.out.println("Error during packet retransmission: " + e.getMessage());
                }
            }
        }, 7000);
    }

    public void receiveAckFromServer() throws IOException {
        byte[] buffer = new byte[1024];
        DatagramPacket response = new DatagramPacket(buffer, buffer.length);
        while (true) {
            try {
                socket.receive(response);
                String ack = new String(response.getData(), 0, response.getLength()).trim();
                System.out.println("Received ACK: " + ack);
            } catch (IOException e) {
                System.out.println("Error receiving acknowledgment: " + e.getMessage());
                throw e; // Re-throw the exception to indicate that an error occurred while receiving acknowledgment
            }
        }
    }

    @Override
    public void run() {
        try {
            send("Message 1");
            send("Message 2");
            send("Message 3");
        } catch (Exception e) {
            System.out.println("Error in client thread: " + e.getMessage());
        }
    }

    public static void main(String[] args) throws Exception {
        Client client = new Client("localhost", 12345);

        Thread receiverThread = new Thread(() -> {
            try {
                client.receiveAckFromServer();
            } catch (IOException e) {
                System.out.println("Error in receiveAckFromServer: " + e.getMessage());
            }
        });
        Thread clientThread = new Thread(client::run);

        receiverThread.start();
        clientThread.start();
    }
}