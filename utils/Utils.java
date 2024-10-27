import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.AlgorithmParameters;
import java.security.Key;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Classe que contem metodos static auxiliares
 * 
 * @author Martim Pereira fc58223
 * @author João Pereira fc58189
 * @author Daniel Nunes fc58257
 * 
 */
public final class Utils {

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Constructor vazio para impedir inicializacao
     */
    private Utils() {
    }

    /**
     * Método que cria um diretório com o path fornecido, caso este não exista.
     * 
     * @param path O path do diretório a ser criado
     * @return true se o diretório foi criado com sucesso, false caso contrário
     */
    public static boolean createDir(String path) {
        File serverFilesDir = new File(path);
        if (!serverFilesDir.exists()) {
            return serverFilesDir.mkdirs();
        }
        return true;

    }

    /**
     * Método que cria um ficheiro com o path fornecido e o conteúdo fornecido.
     * 
     * @param path     O path do ficheiro a ser criado
     * @param contents O conteúdo do ficheiro a ser criado
     */
    public static void createFile(String path, String contents) {
        File f = new File(path);
        try {
            FileWriter fw = new FileWriter(f);
            fw.write(contents);
            fw.close();
        } catch (IOException e) {
            System.err.println("Failed to create file!");
        }

    }

    /**
     * Método que escreve um array de bytes num ficheiro.
     * 
     * @param path O path do ficheiro a ser escrito
     * @param data O array de bytes a ser escrito
     */
    public static void createBinaryFile(String path, byte[] data) {
        File file = new File(path);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(data);
        } catch (Exception e) {
            System.err.println("Error in createBinaryFile: " + e.getMessage());
        }
    }

    /**
     * Método que lê o conteúdo de um ficheiro e devolve um array de bytes com esse
     * conteúdo.
     * 
     * @param fileName ficheiro a ser lido
     * @return array de bytes com o conteúdo do ficheiro ou null caso ocorra um erro
     */
    public static synchronized byte[] getFileContents(String fileName) {

        try (FileInputStream fis = new FileInputStream(fileName)) {
            long fileSize = fis.available();
            byte[] data = new byte[(int) fileSize];
            fis.read(data);
            return data;
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Método que converte um HashMap num array de bytes e envia o HashMap pela
     * socket.
     * 
     * @param hashmap HashMap a ser convertido
     * @return array de bytes com o HashMap
     */
    public static byte[] hashMapToByteArray(HashMap<String, byte[]> hashMap) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(hashMap);
            return bos.toByteArray();
        } catch (IOException e) {
            System.err.println("Error in hashMapToByteArray: " + e.getMessage());
            return new byte[0];
        }
    }

    /**
     * Método que escreve um array de bytes provenientes de um HashMap num ficheiro.
     * 
     * @param byteArray HashMap a escrever em formato de array de bytes
     * @param fileName  nome do ficheiro onde escrever o HashMap
     */
    public synchronized static void writeByteArrayToFile(byte[] byteArray, String fileName, Key domainKey,
            List<byte[]> params) {
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(byteArray));
                Writer writer = new OutputStreamWriter(new FileOutputStream(fileName), StandardCharsets.UTF_8);
                BufferedWriter bufferedWriter = new BufferedWriter(writer)) {

            Object object = ois.readObject();
            if (object instanceof HashMap) {
                HashMap<?, ?> hashMap = (HashMap<?, ?>) object;
                int counter = 0;
                for (Map.Entry<?, ?> entry : hashMap.entrySet()) {
                    if (entry.getKey() instanceof String && entry.getValue() instanceof byte[]) {
                        String key = (String) entry.getKey();
                        byte[] value = (byte[]) entry.getValue();

                        AlgorithmParameters p = AlgorithmParameters.getInstance("PBEWithHmacSHA256AndAES_128");
                        p.init(params.get(counter));
                        counter++;

                        Cipher cipher = Cipher.getInstance("PBEWithHmacSHA256AndAES_128");
                        cipher.init(Cipher.DECRYPT_MODE, domainKey, p);
                        value = cipher.doFinal(value);

                        String s = key + " - " + Float.intBitsToFloat(ByteBuffer.wrap(value).getInt()) + "\n";
                        bufferedWriter.write(s, 0, s.length());
                    }
                }
            } else {
                System.out.println("Object is not a HashMap");
            }

            System.out.println("File written successfully: " + fileName);
        } catch (Exception e) {
            System.err.println("Error in writeByteArrayToFile: " + e.getMessage());
        }
    }

    /**
     * Método que gera um Nonce aleatório
     * 
     * @return Nonce aleatório
     */
    public static Long generateNonce() {
        return RANDOM.nextLong();
    }

    /**
     * Método que gera um código aleatório de autenticação de 5 dígitos.
     * 
     * @return código de autenticação de 5 dígitos
     */
    public static String generateC2FA() {
        int c2faNumber = RANDOM.nextInt(100000);
        return String.format("%05d", c2faNumber);
    }

    /**
     * Método que gera um salt aleatorio para a o parametro de criação de uma
     * SecretKey
     * 
     * @return salt aleatorio
     */
    public static byte[] generateSalt() {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return salt;
    }

    /**
     * Método que gera um numero de interações aleatório
     * 
     * @return o número de interações
     * @ensures valor maior que 1000
     */
    public static int generateIterations() {
        return RANDOM.nextInt(1000) + 1000;
    }

    /**
     * Método que gera uma SecretKey a partir de uma password, um salt e um numero
     * de iterações
     * 
     * @param password   password a ser usada para gerar a SecretKey
     * @param salt       salt a ser usada para gerar a SecretKey
     * @param iterations numero de iterações a serem usadas para gerar a SecretKey
     * @return SecretKey gerada
     */
    public static SecretKey generateSecretKey(String password, byte[] salt, int iterations) {
        try {
            PBEKeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt, iterations);
            SecretKeyFactory kf = SecretKeyFactory.getInstance("PBEWithHmacSHA256AndAES_128");
            SecretKey key = kf.generateSecret(keySpec);
            return key;
        } catch (Exception e) {
            System.err.println("Error in generateSecretKey: " + e.getMessage());
            return null;
        }

    }

    /**
     * Método que converte um array de bytes numa string hexadecimal
     * 
     * @param bytes array de bytes a ser convertido
     * @return string hexadecimal
     */
    public static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            hexString.append(String.format("%02x", b));
        }
        return hexString.toString();
    }

    /**
     * Método que converte uma string hexadecimal num array de bytes
     * 
     * @param hexString string hexadecimal a ser convertida
     * @return array de bytes correspondentes à string hexadecimal
     */
    public static byte[] hexToBytes(String hexString) {
        int len = hexString.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)
                    + Character.digit(hexString.charAt(i + 1), 16));
        }
        return data;
    }

    /**
     * Método que recebe uma mensagem
     * 
     * @return Message enviada pela socket
     */
    public static Message readMessage(ObjectInputStream in) {
        try {
            return ((Message) in.readObject());
        } catch (ClassNotFoundException | IOException e) {
            System.err.println("Error reading Message");
        }
        return null;
    }

    /**
     * Método que envia uma mensagem
     * 
     * @param msg Message a enviar
     */
    public static void writeMessage(Message msg, ObjectOutputStream out) {
        try {
            out.writeObject(msg);
        } catch (IOException e) {
            System.err.println("Error writing Message");
        }
    }

}
