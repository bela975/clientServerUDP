
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;

//https://youtu.be/gLfuZrrfKes?si=ubW5CsHxUUBmXBbr
public class Client {
    private DatagramSocket socket;
    private InetAddress serverAddress;
    private int serverPort;
    private Timer timer;
    private int baseSequenceNumber = 0;
    private int currentSequenceNumber = 0;
    private int nextSequenceNumber = 0;

    private static final int MAX_RETRIES = 3; // Limitar a tentativa de envio de pacotes de dados

    private static final int WINDOW_SIZE = 5;
    private int retryCount = 0; //Controla o número de tentativas feitas para enviar o pacote atual.

    private Queue<DatagramPacket> packetQueue = new LinkedList<>();

    private Queue<String> messageQueue = new LinkedList<>(); // Fila para armazenar mensagens a serem enviadas


    public Client(String address, int port) throws Exception {
        this.socket = new DatagramSocket();
        this.serverAddress = InetAddress.getByName(address);
        this.serverPort = port;
        this.timer = new Timer();
    }




    public void send(String message) throws Exception {
        messageQueue.add(message); // Add message to the queue
        sendNextMessages(); // Attempt to send messages from the queue


        // Converte a mensagem de string para array, faz o checksum, cria o buffer e insere: o número de sequência + checksum + carga útil
        byte[] messageBytes = message.getBytes();
        byte[] checksum = Checksum.calcularChecksum(message);
        ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES + checksum.length + messageBytes.length);
        buffer.putInt(currentSequenceNumber);
        buffer.put(checksum);
        buffer.put(messageBytes);

        // Cria o datagrama e registra o envio do pacote
        DatagramPacket packet = new DatagramPacket(buffer.array(), buffer.array().length, serverAddress, serverPort);
        socket.send(packet);
        System.out.println("Sent packet with sequence number: " + currentSequenceNumber);
        startTimer(packet); // Temporizador iniciado, caso precise retrasmitir o pacote


    //criando e configurando as janelas
     if (nextSequenceNumber < baseSequenceNumber + WINDOW_SIZE) {
        buffer.putInt(nextSequenceNumber);
        buffer.put(checksum);
        buffer.put(messageBytes);
        socket.send(packet);
        packetQueue.add(packet);
        System.out.println("Pacote enviado com número de sequência: " + nextSequenceNumber);
        if (baseSequenceNumber == nextSequenceNumber) {
            startTimer(packet);
        }
        nextSequenceNumber++;
    } else {
        System.out.println("Janela está cheia. Não é possível enviar o pacote agora.");
    }
    }

    private void sendNextMessages() throws Exception {



        while (nextSequenceNumber < baseSequenceNumber + WINDOW_SIZE && !messageQueue.isEmpty()) {
            String message = messageQueue.poll(); // Obtém a próxima mensagem da fila
            byte[] messageBytes = message.getBytes();
            byte[] checksum = Checksum.calcularChecksum(message);
            ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES + checksum.length + messageBytes.length);
            buffer.putInt(nextSequenceNumber);
            buffer.put(checksum);
            buffer.put(messageBytes);
            DatagramPacket packet = new DatagramPacket(buffer.array(), buffer.array().length, serverAddress, serverPort);
            socket.send(packet);
            packetQueue.add(packet);
            System.out.println("Pacote enviado com número de sequência: " + nextSequenceNumber);
            if (baseSequenceNumber == nextSequenceNumber) {
                startTimer(packet);
            }
            nextSequenceNumber++;
        }
    }

    private void startTimer(DatagramPacket packet) {
        timer.schedule(new TimerTask() { // Cria um novo timmer
            public void run() {
                try {
                    retryCount++; // Contador para controlar as tentativas de envios
                    System.out.println("Timeout! Retransmitindo o pacote: " + currentSequenceNumber );
                    send(new String(packet.getData()));
                } catch (Exception e) {
                    System.out.println("Erro durante a retransmissão do pacote: " + e.getMessage());
                }
            }
        }, 7000); // Delay de 7 segundos
    }

    // Método feito para a confirmação de recebimento dos pacotes (ACK)
    public void receiveAckFromServer() throws Exception {


        int currentSequenceNumber = 0;


        byte[] buffer = new byte[1024]; // Cria um buffer para receber os pacotes
        DatagramPacket response = new DatagramPacket(buffer, buffer.length); // Cria um datagrama para receber os pacotes
        socket.receive(response); // Receber o pacote

        // Constrói uma string a partir dos dados do pacote recebido e remove espaços extras.
        String ack = new String(response.getData(), 0, response.getLength()).trim();

        // Verifica se a string de ACK recebida começa com "ACK" seguido pelo número de sequência atual.
        if (ack.startsWith("ACK " + currentSequenceNumber)) {
            System.out.println(ack + " Recebido");
            timer.cancel(); // Cancela o timer, pois o pacote foi transmitido
            currentSequenceNumber++; // Incrementa o num de sequência para pegar o próximo pacote
            retryCount = 0; // zera a contagem, pois o pacote foi enviado
        } else {
            System.out.println("Received ACK for wrong sequence, expected: " + currentSequenceNumber + ", got: " + ack);
        }
    }

}


    public void main(String[] args) throws Exception {
        Client client = new Client("localhost", 12345);
        // Exemplo de envio de várias mensagens
        client.send("Mensagem 1");
        client.send("Mensagem 2");
        client.send("Mensagem 3");
        // Aguarde as respostas ACK/NACK do servidor
        client.receiveAckFromServer();

    }

