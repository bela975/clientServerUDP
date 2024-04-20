import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

public class Server {
    private DatagramSocket serverSocket;
    private int expectedSequenceNumber = 0;
    private List<InetAddress> clientAddresses = new ArrayList<>();
    private List<Integer> clientPorts = new ArrayList<>();

    public Server(int port) throws SocketException {
        this.serverSocket = new DatagramSocket(port);
        System.out.println("Server running on port: " + port);
    }

    public void startServer() {
        byte[] receiveBuffer = new byte[1024];

        try {
            while (true) {
                DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                serverSocket.receive(receivePacket);

                InetAddress clientAddress = receivePacket.getAddress();
                int clientPort = receivePacket.getPort();

                // If new client, add to list
                if (!clientAddresses.contains(clientAddress) || !clientPorts.contains(clientPort)) {
                    clientAddresses.add(clientAddress);
                    clientPorts.add(clientPort);
                }

                processPacket(receivePacket);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeServerSocket();
        }
    }

    private void processPacket(DatagramPacket packet) throws IOException {
        InetAddress clientAddress = packet.getAddress();
        int clientPort = packet.getPort();
        String message = new String(packet.getData(), 0, packet.getLength());

        System.out.println("Received message from " + clientAddress + ":" + clientPort + ": " + message);

        // Echo message to all clients
        for (int i = 0; i < clientAddresses.size(); i++) {
            InetAddress destinationAddress = clientAddresses.get(i);
            int destinationPort = clientPorts.get(i);

            if (!(clientAddress.equals(destinationAddress) && clientPort == destinationPort)) {
                sendResponse(destinationAddress, destinationPort, message);
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

    public static void main(String[] args) throws SocketException {
        Server server = new Server(1234);
        server.startServer();
    }
}