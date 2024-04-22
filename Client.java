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
    // Logger para registrar mensagens
    private static final Logger LOGGER = Logger.getLogger(Client.class.getName());

    // Tamanho da janela deslizante para controle de congestionamento
    private static final int WINDOW_SIZE = 5;

    // Socket UDP do cliente
    private final DatagramSocket socket;

    // Endereço do servidor
    private final InetAddress serverAddress;

    // Porta do servidor
    private final int serverPort;

    // Timer para controle de timeout
    private final Timer timer;

    // Número de sequência atual
    private int currentSequenceNumber = 0;

    // Último número de sequência confirmado
    private int lastConfirmedSequenceNumber = -1;

    // Fila de mensagens a serem enviadas
    private final Queue<String> messageQueue = new LinkedList<>();

    // Flag para indicar se o cliente está em execução
    private volatile boolean running = true;

    // Gerador de números aleatórios para simular erros
    private final Random random = new Random();

    // Construtor do cliente
    public Client(String address, int port) throws IOException {
        this.socket = new DatagramSocket(); //Datagram socket é o que permite que o cliente receba e envie mensagens
        this.serverAddress = InetAddress.getByName(address);
        this.serverPort = port;
        this.timer = new Timer();
    }

    // Método para enviar uma mensagem ao servidor
    public void send(String message, boolean simulateCorruption) throws IOException {
        if (!socket.isClosed()) {
            byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
            int checksum = CRC16CCITT.calculate(messageBytes);

            if (simulateCorruption && shouldSimulateError()) {
                // Simula corrupção modificando o checksum
                checksum = checksum ^ 0xFFFF; // Aplicando um XOR bit a bit com todos os 1s
            }

            // Constrói o pacote com o checksum e a mensagem
            String packetData = checksum + ":" + message;
            byte[] packetBytes = packetData.getBytes(StandardCharsets.UTF_8);
            DatagramPacket packet = new DatagramPacket(packetBytes, packetBytes.length, serverAddress, serverPort);
            socket.send(packet);
            LOGGER.log(Level.INFO, "Sent packet with checksum: {0}", checksum);
            // Inicia o timer para este pacote
            startTimer(packet, currentSequenceNumber++);
        } else {
            LOGGER.log(Level.INFO, "Socket is closed, cannot send messages.");
        }
    }

    // Método para enviar as próximas mensagens na fila
    private synchronized void sendNextMessages() throws IOException {
        // Enquanto a fila de mensagens não estiver vazia e o número de mensagens enviadas ainda estiver dentro do tamanho da janela deslizante
        while (!messageQueue.isEmpty() && currentSequenceNumber - lastConfirmedSequenceNumber <= WINDOW_SIZE) {
            String message = messageQueue.poll(); // Remove e obtém a primeira mensagem da fila

        }
    }

    // Método para iniciar o timer para um pacote
    private void startTimer(DatagramPacket packet, int sequenceNumber) {
        timer.schedule(new TimerTask() {
            public void run() {
                try {
                    // Verifica se houve timeout e reenvia o pacote se necessário
                    if (sequenceNumber > lastConfirmedSequenceNumber) {
                        LOGGER.log(Level.INFO, "Timeout! Retransmitting packet with sequence number: {0}", sequenceNumber);
                        socket.send(packet);
                    }
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, "Error during packet retransmission", e);
                }
            }
        }, 7000); // Tempo de timeout em milissegundos
    }

    // Método para receber ACKs do servidor
    public void receiveAckFromServer() {
        byte[] buffer = new byte[1024];
        DatagramPacket response = new DatagramPacket(buffer, buffer.length);
        while (running) {
            try {
                socket.receive(response);
                String ack = new String(response.getData(), 0, response.getLength()).trim();
                if (ack.startsWith("ACK")) {
                    int ackNum = Integer.parseInt(ack.substring(3));
                    // Atualiza o último número de sequência confirmado
                    lastConfirmedSequenceNumber = Math.max(lastConfirmedSequenceNumber, ackNum);
                    LOGGER.log(Level.INFO, "Received ACK: {0}", ackNum);
                    // Envia as próximas mensagens na fila
                    sendNextMessages();
                } else if (ack.startsWith("NAK")) {
                    // Lida com o NAK recebido (não implementado aqui)
                    int sequenceNumber = Integer.parseInt(ack.substring(3));
                    LOGGER.log(Level.INFO, "Received NAK for sequence number: {0}, resending...", sequenceNumber);
                    // Implemente a lógica de reenvio aqui, se necessário
                }
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "IOException while receiving ACK", e);
                running = false;
            }
        }
    }

    // Método para simular erro com uma certa probabilidade
    private boolean shouldSimulateError() {
        // Simula erro com probabilidade de 20%
        return random.nextDouble() < 0.2;
    }

    // Método run para execução da thread
    @Override
    public void run() {
        // Inicia uma nova thread para receber ACKs do servidor
        Thread ackReceiverThread = new Thread(this::receiveAckFromServer);
        ackReceiverThread.start();

        try {
            // Lê mensagens do usuário e envia ao servidor
            Scanner scanner = new Scanner(System.in);
            while (running) {
                String userInput = scanner.nextLine();
                if ("exit".equalsIgnoreCase(userInput)) {
                    running = false;
                    break;
                }
                try {
                    send(userInput, false);  // Altere o segundo parâmetro para true para simular erros
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        } finally {
            // Cancela o timer e fecha o socket ao finalizar
            timer.cancel();
            socket.close();
        }
    }

    // Método main para iniciar o cliente
    public static void main(String[] args) {
        // Verifica se o número correto de argumentos foi passado
        if (args.length != 2) {
            LOGGER.log(Level.SEVERE, "Usage: java Client <server_address> <server_port>");
            return;
        }
        try {
            // Obtém o endereço do servidor e a porta do argumento de linha de comando
            String serverAddress = args[0];
            int serverPort = Integer.parseInt(args[1]);
            // Inicializa e inicia o cliente
            Client client = new Client(serverAddress, serverPort);
            Thread clientThread = new Thread(client);
            clientThread.start();
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Failed to initialize the client", ex);
        }
    }
}
