public class CRC16CCITT {
    private static final int POLYNOMIAL = 0x1021;
    private static final int INITIAL_VALUE = 0xFFFF;

    public static int calculate(byte[] bytes) {
        int crc = INITIAL_VALUE;

        for (byte b : bytes) {
            crc ^= (b & 0xFF) << 8;

            for (int i = 0; i < 8; i++) {
                if ((crc & 0x8000) != 0) {
                    crc = (crc << 1) ^ POLYNOMIAL;
                } else {
                    crc <<= 1;
                }
            }
        }

        return crc & 0xFFFF;
    }
}
