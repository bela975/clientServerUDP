import java.security.MessageDigest;

public class Checksum {

    // Método para calcular o checksum MD5 de uma string
    public static byte[] calcularChecksum(String dados) throws Exception {
        // Obtém uma instância do MessageDigest para o algoritmo MD5
        MessageDigest md5 = MessageDigest.getInstance("MD5");

        // Atualiza o MessageDigest com os dados fornecidos
        md5.update(dados.getBytes());

        // Calcula o hash MD5 dos dados e retorna o resultado como um array de bytes
        return md5.digest();
    }
}
/*
 * O MD5 (Message Digest Algorithm 5) é um algoritmo de hash criptográfico amplamente utilizado
 * para gerar um resumo (ou hash) de uma mensagem ou conjunto de dados.
 * O hash MD5 produz uma sequência de 128 bits (ou 16 bytes) que é única para cada
 * entrada de dados, com uma probabilidade muito baixa de duas entradas diferentes produzirem o mesmo hash.
 */