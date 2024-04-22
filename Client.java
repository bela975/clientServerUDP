import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Client implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(Client.class.getName());
    private static final int WINDOW_SIZE = 5;
    private final DatagramSocket socket;
    private final InetAddress serverAddress;
    private final int serverPort;
    private final Timer timer;
    private int currentSequenceNumber = 0;
    private int lastConfirmedSequenceNumber = -1;
    private final Queue<String> messageQueue = new LinkedList<>();
    private volatile boolean running = true;
    private final Random random = new Random();

    public Client(String address, int port) throws IOException {
        this.socket = new DatagramSocket();
        this.serverAddress = InetAddress.getByName(address);
        this.serverPort = port;
        this.timer = new Timer();
    }

    public void send(String message, boolean simulateCorruption) throws IOException {
        if (!socket.isClosed()) {
            byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
            int checksum = CRC16CCITT.calculate(messageBytes);

            if (simulateCorruption && shouldSimulateError()) {
                // Simulate corruption by modifying the checksum
                checksum = checksum ^ 0xFFFF; // Applying bitwise XOR with all 1s
            }

            String packetData = checksum + ":" + message;
            byte[] packetBytes = packetData.getBytes(StandardCharsets.UTF_8);
            DatagramPacket packet = new DatagramPacket(packetBytes, packetBytes.length, serverAddress, serverPort);
            socket.send(packet);
            LOGGER.log(Level.INFO, "Sent packet with checksum: {0}", checksum);
            startTimer(packet, currentSequenceNumber++);
        } else {
            LOGGER.log(Level.INFO, "Socket is closed, cannot send messages.");
        }
    }

    private synchronized void sendNextMessages() throws IOException {
        while (!messageQueue.isEmpty() && currentSequenceNumber - lastConfirmedSequenceNumber <= WINDOW_SIZE) {
            String message = messageQueue.poll();
            byte[] messageBytes = message.getBytes();
            int checksum = CRC16CCITT.calculate(messageBytes);
            String packetData = checksum + ":" + message;
            byte[] packetBytes = packetData.getBytes();
            DatagramPacket packet = new DatagramPacket(packetBytes, packetBytes.length, serverAddress, serverPort);
            socket.send(packet);
            LOGGER.log(Level.INFO, "Sent packet with sequence number: {0}, checksum: {1}", new Object[]{currentSequenceNumber, checksum});
            startTimer(packet, currentSequenceNumber++);
        }
    }

    private void startTimer(DatagramPacket packet, int sequenceNumber) {
        timer.schedule(new TimerTask() {
            public void run() {
                try {
                    if (sequenceNumber > lastConfirmedSequenceNumber) {
                        LOGGER.log(Level.INFO, "Timeout! Retransmitting packet with sequence number: {0}", sequenceNumber);
                        socket.send(packet);
                    }
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, "Error during packet retransmission", e);
                }
            }
        }, 7000);
    }

    public void receiveAckFromServer() {
        byte[] buffer = new byte[1024];
        DatagramPacket response = new DatagramPacket(buffer, buffer.length);
        while (running) {
            try {
                socket.receive(response);
                String ack = new String(response.getData(), 0, response.getLength()).trim();
                if (ack.startsWith("ACK")) {
                    int ackNum = Integer.parseInt(ack.substring(3));
                    lastConfirmedSequenceNumber = Math.max(lastConfirmedSequenceNumber, ackNum);
                    LOGGER.log(Level.INFO, "Received ACK: {0}", ackNum);
                    sendNextMessages();
                } else if (ack.startsWith("NAK")) {
                    int sequenceNumber = Integer.parseInt(ack.substring(3));
                    LOGGER.log(Level.INFO, "Received NAK for sequence number: {0}, resending...", sequenceNumber);
                    // Handle resending logic here if required
                }
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "IOException while receiving ACK", e);
                running = false;
            }
        }
    }


    private boolean shouldSimulateError() {
        // Simulate error with 20% probability
         return random.nextDouble() < 0.2;
    }

    @Override
    public void run() {
        Thread ackReceiverThread = new Thread(this::receiveAckFromServer);
        ackReceiverThread.start();

        try {
            Scanner scanner = new Scanner(System.in);
            while (running) {
                String userInput = scanner.nextLine();
                if ("exit".equalsIgnoreCase(userInput)) {
                    running = false;
                    break;
                }
                try {
                    send(userInput, false);  // Change second parameter to true to simulate errors
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        } finally {
            timer.cancel();
            socket.close();
        }
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            LOGGER.log(Level.SEVERE, "Usage: java Client <server_address> <server_port>");
            return;
        }
        try {
            String serverAddress = args[0];
            int serverPort = Integer.parseInt(args[1]);
            Client client = new Client(serverAddress, serverPort);
            Thread clientThread = new Thread(client);
            clientThread.start();
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Failed to initialize the client", ex);
        }
    }
}
