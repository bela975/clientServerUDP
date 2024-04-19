import java.security.MessageDigest;

public class Checksum {

    public static byte[] calcularChecksum(String dados) throws Exception {
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        md5.update(dados.getBytes());
        return md5.digest();
    }
}