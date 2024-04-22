public class CRC16CCITT {
    // Polinômio utilizado no cálculo da checagem de redundancia ciclica
    private static final int POLYNOMIAL = 0x1021;

    // Valor inicial do CRC
    private static final int INITIAL_VALUE = 0xFFFF;

    // Método para calcular o CRC16-CCITT de um array de bytes
    public static int calculate(byte[] bytes) {
        // Inicializa o valor do CRC com o valor inicial
        int crc = INITIAL_VALUE;

        // Itera sobre cada byte no array de bytes
        for (byte b : bytes) {
            // Realiza um XOR entre o byte e o valor do CRC deslocado 8 bits para a esquerda
            crc ^= (b & 0xFF) << 8;

            // Itera sobre cada bit do byte
            for (int i = 0; i < 8; i++) {
                // Verifica se o bit mais significativo do CRC é 1
                if ((crc & 0x8000) != 0) {
                    // Se for 1, realiza um shift left no CRC e aplica o polinômio
                    crc = (crc << 1) ^ POLYNOMIAL;
                } else {
                    // Se for 0, realiza apenas um shift left no CRC
                    crc <<= 1;
                }
            }
        }

        // Retorna o valor do CRC resultante (apenas os 16 bits menos significativos)
        return crc & 0xFFFF;
    }
}
