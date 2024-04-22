import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Server {
    // Socket do servidor UDP
    private final DatagramSocket serverSocket;

    // Mapeia endereços dos clientes para portas
    private final Map<InetAddress, Integer> clientEndpoints = new HashMap<>();

    // Mapeia endereços dos clientes para números de sequência
    private final Map<InetAddress, Integer> clientSequenceNumbers = new HashMap<>();

    // Logger para registrar mensagens
    private static final Logger LOGGER = Logger.getLogger(Server.class.getName());

    // Construtor do servidor
    public Server(int port) throws IOException {
        // Inicializa o socket do servidor na porta especificada
        this.serverSocket = new DatagramSocket(port);
        LOGGER.log(Level.INFO, "Server running on port: {0}", port);
    }

    // Método para iniciar o servidor
    public void startServer() {
        // Buffer para receber mensagens
        byte[] receiveBuffer = new byte[1024];

        try {
            // Loop principal do servidor
            while (true) {
                // Recebe um pacote
                DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                serverSocket.receive(receivePacket);

                // Obtém o endereço IP e a porta do cliente
                InetAddress clientAddress = receivePacket.getAddress();
                int clientPort = receivePacket.getPort();

                // Adiciona o cliente se não estiver na lista
                clientEndpoints.putIfAbsent(clientAddress, clientPort);
                clientSequenceNumbers.putIfAbsent(clientAddress, 0);

                // Processa o pacote recebido
                processPacket(receivePacket);
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "IOException in startServer", e);
        } finally {
            // Fecha o socket do servidor ao finalizar
            closeServerSocket();
        }
    }

    // Método para processar um pacote recebido
    private void processPacket(DatagramPacket packet) throws IOException {
        // Extrai informações do pacote
        InetAddress clientAddress = packet.getAddress();
        int clientPort = packet.getPort();
        String packetData = new String(packet.getData(), 0, packet.getLength()).trim();
        int separatorIndex = packetData.indexOf(':');
        int receivedChecksum = Integer.parseInt(packetData.substring(0, separatorIndex));
        String message = packetData.substring(separatorIndex + 1);
        byte[] messageBytes = message.getBytes();
        int calculatedChecksum = CRC16CCITT.calculate(messageBytes);

        // Registra informações sobre a mensagem recebida
        LOGGER.log(Level.INFO, "Received message from {0}:{1}: {2}, checksum: {3}", new Object[]{clientAddress, clientPort, message, receivedChecksum});

        // Verifica se o checksum recebido corresponde ao checksum calculado
        if (receivedChecksum == calculatedChecksum) {
            // Incrementa e obtém o número de sequência atual para o ACK
            int currentSequenceNumber = clientSequenceNumbers.getOrDefault(clientAddress, 0) + 1;
            clientSequenceNumbers.put(clientAddress, currentSequenceNumber);
            // Envia ACK para o cliente
            sendAck(clientAddress, clientPort, currentSequenceNumber);
        } else {
            // Envia NAK para o cliente devido a um checksum incorreto
            sendNak(clientAddress, clientPort, receivedChecksum);
        }

        // Replica a mensagem para todos os clientes
        echoMessageToAllClients(message, clientAddress, clientPort);
    }

    // Método para enviar um ACK para o cliente
    private void sendAck(InetAddress clientAddress, int clientPort, int sequenceNumber) throws IOException {
        String ackMessage = "ACK" + sequenceNumber;
        byte[] ackData = ackMessage.getBytes();
        DatagramPacket ackPacket = new DatagramPacket(ackData, ackData.length, clientAddress, clientPort);
        LOGGER.log(Level.INFO, "Sending ACK for sequence number: {0} to {1}:{2}", new Object[]{sequenceNumber, clientAddress, clientPort});
        serverSocket.send(ackPacket);
    }

    // Método para enviar um NAK para o cliente
    private void sendNak(InetAddress clientAddress, int clientPort, int wrongChecksum) throws IOException {
        String nakMessage = "NAK" + wrongChecksum;
        byte[] nakData = nakMessage.getBytes();
        DatagramPacket nakPacket = new DatagramPacket(nakData, nakData.length, clientAddress, clientPort);
        LOGGER.log(Level.INFO, "Sending NAK for wrong checksum: {0} to {1}:{2}", new Object[]{wrongChecksum, clientAddress, clientPort});
        serverSocket.send(nakPacket);
    }

    // Método para replicar a mensagem para todos os clientes
    private void echoMessageToAllClients(String message, InetAddress senderAddress, int senderPort) throws IOException {
        for (Map.Entry<InetAddress, Integer> entry : clientEndpoints.entrySet()) {
            if (!senderAddress.equals(entry.getKey()) || senderPort != entry.getValue()) {
                // Envia a mensagem para o cliente
                sendResponse(entry.getKey(), entry.getValue(), message);
            }
        }
    }

    // Método para enviar uma resposta para um cliente específico
    private void sendResponse(InetAddress address, int port, String response) throws IOException {
        byte[] sendData = response.getBytes();
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, address, port);
        serverSocket.send(sendPacket);
    }

    // Método para fechar o socket do servidor
    public void closeServerSocket() {
        if (serverSocket != null && !serverSocket.isClosed()) {
            serverSocket.close();
        }
    }

    // Método main para iniciar o servidor
    public static void main(String[] args) {
        // Verifica se o número correto de argumentos foi passado
        if (args.length < 1) {
            LOGGER.log(Level.SEVERE, "Usage: java Server <port_number>");
            return;
        }
        try {
            // Obtém o número da porta do argumento de linha de comando
            int port = Integer.parseInt(args[0]);
            // Inicializa e inicia o servidor
            Server server = new Server(port);
            server.startServer();
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Failed to initialize the server", ex);
        }
    }
}
