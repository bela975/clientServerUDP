import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class Server {
    private DatagramSocket socket;
    private int expectedSequenceNumber = 0; // Para o controle do próximo número de sequência

    // Construtor só inicializa o SocketUDP
    public Server(int port) throws Exception {
        this.socket = new DatagramSocket(port);
        System.out.println("Porta a qual o servidor está operando: " + port);
    }

    public void listen() throws Exception {
        byte[] receiveBuffer = new byte[1024]; //// Cria um array para receber os dados

        // Cria um datagrama para receber os dados, espera a chegada dos pacotes e precessa-os
        while (true) {
            DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
            socket.receive(receivePacket);
            processPacket(receivePacket);
        }
    }

    private void processPacket(DatagramPacket packet) throws Exception {
        ByteBuffer wrapped = ByteBuffer.wrap(packet.getData()); // Encapsula os dados (facilita a leitura em binário)

        int sequenceNumber = wrapped.getInt(); // Lê o número de sequência

        // Lê o checksum
        byte[] receivedChecksum = new byte[16];
        wrapped.get(receivedChecksum);

        // Calcula o tamanho da mensagem (subtrai o número de sequência e o checksum)
        byte[] message = new byte[packet.getLength() - Integer.BYTES - 16];
        wrapped.get(message);

        // Agora, realmente, processa e calcula e checksum
        byte[] calculatedChecksum = Checksum.calcularChecksum(new String(message));

        // Verificação do checksum e do número de sequência (Arrays.equals se retorna true, ambos forem iguais)
        boolean correctChecksum = Arrays.equals(receivedChecksum, calculatedChecksum);
        boolean correctSequence = sequenceNumber == expectedSequenceNumber;


        String response;

        if (correctChecksum && correctSequence) {
            response = "ACK " + sequenceNumber;
        } else {
            response = "NACK " + sequenceNumber;
        }

        if (response.startsWith("ACK")) {
            expectedSequenceNumber++;
            System.out.println("Pacote recebido com sucesso! Número de sequência: " + sequenceNumber + "): " + new String(message));
        } else {
            System.out.println("Pacote recebido incorretamente! Número de sequência: " + sequenceNumber + ")");
        }

        // Envia a mensagem (ACK ou NACK) para a porta e endereço de origem
        sendResponse(packet.getAddress(), packet.getPort(), response);
    }

    // Enviar a resposta para o cliente
    private void sendResponse(InetAddress address, int port, String response) throws Exception {
        byte[] sendData = response.getBytes(); // String para byte
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, address, port);
        socket.send(sendPacket);
    }

    public static void main(String[] args) throws Exception {
        int port = 12345;
        Server server = new Server(port);
        server.listen();
    }
}
