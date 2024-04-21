import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Server {
    private final DatagramSocket serverSocket;
    private final Map<InetAddress, Integer> clientEndpoints = new HashMap<>();
    private final Map<InetAddress, Integer> clientSequenceNumbers = new HashMap<>();
    private static final Logger LOGGER = Logger.getLogger(Server.class.getName());

    public Server(int port) throws IOException {
        this.serverSocket = new DatagramSocket(port);
        LOGGER.log(Level.INFO, "Server running on port: {0}", port);
    }

    public void startServer() {
        byte[] receiveBuffer = new byte[1024];

        try {
            while (true) {
                DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                serverSocket.receive(receivePacket);

                InetAddress clientAddress = receivePacket.getAddress();
                int clientPort = receivePacket.getPort();

                // Add the client if not already in the list
                clientEndpoints.putIfAbsent(clientAddress, clientPort);
                clientSequenceNumbers.putIfAbsent(clientAddress, 0);

                processPacket(receivePacket);
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "IOException in startServer", e);
        } finally {
            closeServerSocket();
        }
    }

    private void processPacket(DatagramPacket packet) throws IOException {
        InetAddress clientAddress = packet.getAddress();
        int clientPort = packet.getPort();
        String packetData = new String(packet.getData(), 0, packet.getLength()).trim();
        int separatorIndex = packetData.indexOf(':');
        int receivedChecksum = Integer.parseInt(packetData.substring(0, separatorIndex));
        String message = packetData.substring(separatorIndex + 1);
        byte[] messageBytes = message.getBytes();
        int calculatedChecksum = CRC16CCITT.calculate(messageBytes);

        LOGGER.log(Level.INFO, "Received message from {0}:{1}: {2}, checksum: {3}", new Object[]{clientAddress, clientPort, message, receivedChecksum});

        if (receivedChecksum == calculatedChecksum) {
            // Increment and get the current sequence number for the ACK
            int currentSequenceNumber = clientSequenceNumbers.getOrDefault(clientAddress, 0) + 1;
            clientSequenceNumbers.put(clientAddress, currentSequenceNumber);
            sendAck(clientAddress, clientPort, currentSequenceNumber);
        } else {
            sendNak(clientAddress, clientPort, receivedChecksum);  // Send NAK for wrong checksum
        }

        echoMessageToAllClients(message, clientAddress, clientPort);
    }

    private void sendAck(InetAddress clientAddress, int clientPort, int sequenceNumber) throws IOException {
        String ackMessage = "ACK" + sequenceNumber;
        byte[] ackData = ackMessage.getBytes();
        DatagramPacket ackPacket = new DatagramPacket(ackData, ackData.length, clientAddress, clientPort);
        LOGGER.log(Level.INFO, "Sending ACK for sequence number: {0} to {1}:{2}", new Object[]{sequenceNumber, clientAddress, clientPort});
        serverSocket.send(ackPacket);
    }

    private void sendNak(InetAddress clientAddress, int clientPort, int wrongChecksum) throws IOException {
        String nakMessage = "NAK" + wrongChecksum;
        byte[] nakData = nakMessage.getBytes();
        DatagramPacket nakPacket = new DatagramPacket(nakData, nakData.length, clientAddress, clientPort);
        LOGGER.log(Level.INFO, "Sending NAK for wrong checksum: {0} to {1}:{2}", new Object[]{wrongChecksum, clientAddress, clientPort});
        serverSocket.send(nakPacket);
    }

    private void echoMessageToAllClients(String message, InetAddress senderAddress, int senderPort) throws IOException {
        for (Map.Entry<InetAddress, Integer> entry : clientEndpoints.entrySet()) {
            if (!senderAddress.equals(entry.getKey()) || senderPort != entry.getValue()) {
                sendResponse(entry.getKey(), entry.getValue(), message);
            }
        }
    }

    private void sendResponse(InetAddress address, int port, String response) throws IOException {
        byte[] sendData = response.getBytes();
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, address, port);
        serverSocket.send(sendPacket);
    }

    public void closeServerSocket() {
        if (serverSocket != null && !serverSocket.isClosed()) {
            serverSocket.close();
        }
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            LOGGER.log(Level.SEVERE, "Usage: java Server <port_number>");
            return;
        }
        try {
            int port = Integer.parseInt(args[0]);
            Server server = new Server(port);
            server.startServer();
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Failed to initialize the server", ex);
        }
    }
}
