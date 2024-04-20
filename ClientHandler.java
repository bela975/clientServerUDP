import java.io.IOException;
import java.net.*;
import java.util.ArrayList;

public class ClientHandler implements Runnable {

    public static ArrayList<ClientHandler> clientHandlers = new ArrayList<>();

    private DatagramSocket datagramSocket;
    private InetAddress serverAddress;
    private int serverPort;

    private String clientUsername;

    public ClientHandler(DatagramSocket datagramSocket, InetAddress serverAddress, int serverPort, String clientUsername) {
        this.datagramSocket = datagramSocket;
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.clientUsername = clientUsername;
        clientHandlers.add(this);
        broadcastMessage("SERVIDOR: " + clientUsername + " entrou no chat");
    }

    public void run() {
        byte[] receiveBuffer = new byte[1024];

        while (true) {
            try {
                DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                datagramSocket.receive(receivePacket);

                String messageFromC = new String(receivePacket.getData(), 0, receivePacket.getLength());
                broadcastMessage(messageFromC);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void broadcastMessage(String messageToSend) {
        byte[] sendBuffer = messageToSend.getBytes();

        for (ClientHandler clientHandler : clientHandlers) {
            try {
                if (!clientHandler.clientUsername.equals(clientUsername)) {
                    DatagramPacket sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, clientHandler.serverAddress, clientHandler.serverPort);
                    datagramSocket.send(sendPacket);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void removeClientHandler() {
        clientHandlers.remove(this);
        broadcastMessage("SERVIDOR: " + clientUsername + " saiu do grupo :/");
    }
}