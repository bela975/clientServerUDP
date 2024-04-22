import java.io.IOException;
import java.net.*;
import java.util.ArrayList;

public class ClientHandler implements Runnable {

    // Lista para armazenar os manipuladores de clientes
    public static ArrayList<ClientHandler> clientHandlers = new ArrayList<>();

    // Variáveis de instância
    private DatagramSocket datagramSocket;
    private InetAddress serverAddress;
    private int serverPort;
    private String clientUsername;

    // Construtor
    public ClientHandler(DatagramSocket datagramSocket, InetAddress serverAddress, int serverPort, String clientUsername) {
        this.datagramSocket = datagramSocket;
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.clientUsername = clientUsername;
        // Adiciona este manipulador de cliente à lista
        clientHandlers.add(this);
        // Anuncia quando um novo cliente entra no chat
        broadcastMessage("SERVIDOR: " + clientUsername + " entrou no chat");
    }

    // Método run (para thread)
    public void run() {
        // Buffer para receber mensagens
        byte[] receiveBuffer = new byte[1024];

        // Loop para receber mensagens continuamente
        while (true) {
            try {
                // Recebe um datagrama
                DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                datagramSocket.receive(receivePacket);

                // Converte a mensagem recebida em uma string
                String messageFromC = new String(receivePacket.getData(), 0, receivePacket.getLength());

                // Transmite a mensagem recebida para todos os clientes
                broadcastMessage(messageFromC);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Método para enviar uma mensagem para todos os clientes, exceto o remetente
    public void broadcastMessage(String messageToSend) {
        // Converte a mensagem em bytes
        byte[] sendBuffer = messageToSend.getBytes();

        // Itera sobre todos os manipuladores de clientes
        for (ClientHandler clientHandler : clientHandlers) {
            try {
                // Verifica se o cliente atual não é o remetente original da mensagem
                if (!clientHandler.clientUsername.equals(clientUsername)) {
                    // Cria um pacote com a mensagem e envia para o cliente
                    DatagramPacket sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, clientHandler.serverAddress, clientHandler.serverPort);
                    datagramSocket.send(sendPacket);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Método para remover este manipulador de cliente da lista
    public void removeClientHandler() {
        clientHandlers.remove(this);
        // Anuncia quando um cliente sai do chat
        broadcastMessage("SERVIDOR: " + clientUsername + " saiu do grupo :/");
    }
}
