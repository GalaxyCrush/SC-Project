import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

/**
 * Classe que lida com a encriptação e desencriptação do ficheiro de
 * utilizadores
 * 
 * @author Martim Pereira fc58223
 * @author João Pereira fc58189
 * @author Daniel Nunes fc58257
 */
public class UserFileHandler {

    private SecureRandom random = new SecureRandom();

    private final String ALGORITHM = "PBEWithHmacSHA256AndAES_128";

    private final String USERS_PATH = "server/serverFiles/users.txt";
    private final String SALT_PATH = "server/serverFiles/userParams/salt.txt";
    private final String ITERATIONS_PATH = "server/serverFiles/userParams/iterations.txt";

    private final String PARAMS_PATH = "server/serverFiles/userParams/params.txt";

    private SecretKey key;

    /**
     * Construtor de um UserFileHandler
     * 
     * @param password Password usada para criar chaves
     * @throws IOException
     * @throws InvalidKeySpecException
     * @throws NoSuchAlgorithmException
     */
    public UserFileHandler(String password) throws IOException, InvalidKeySpecException, NoSuchAlgorithmException {
        createDir("server/serverFiles/userParams");
        this.key = Utils.generateSecretKey(password, getSalt(), getIterations());
    }

    /**
     * Metodo que cria um diretorio do userParams
     * 
     * @param path nome do diretorio
     * @return Se foi criado com sucesso
     */
    public static boolean createDir(String path) {
        File serverFilesDir = new File(path);
        if (!serverFilesDir.exists()) {
            return serverFilesDir.mkdirs();
        }
        return true;

    }

    /**
     * Método que faz backup do ficheiro de utilizadores, encriptando-o
     * 
     * @param content Conteúdo do ficheiro de utilizadores
     * @throws IOException
     * @throws InvalidKeyException
     * @throws InvalidAlgorithmParameterException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     * @throws BadPaddingException
     * @throws IllegalBlockSizeException
     */
    public void backupUserFile(String content)
            throws IOException, InvalidKeyException, InvalidAlgorithmParameterException, NoSuchAlgorithmException,
            NoSuchPaddingException, BadPaddingException, IllegalBlockSizeException {
        Cipher c = Cipher.getInstance(ALGORITHM);
        c.init(Cipher.ENCRYPT_MODE, key);
        byte[] enc = c.doFinal(content.getBytes());
        byte[] params = c.getParameters().getEncoded();
        saveParams(params);

        writeToFile(USERS_PATH, Base64.getEncoder().encode(enc));
    }

    /**
     * Método que guarda os parametros de encriptação num ficheiro
     * 
     * @param params Parametros de encriptação
     */
    private void saveParams(byte[] params) {
        try {
            writeToFile(PARAMS_PATH, params);
        } catch (Exception e) {
            System.err.println("Error saving params");
        }
    }

    /**
     * Método que desencipta o ficheiro de utilizadores e carrega o seu conteúdo
     * 
     * @return Conteúdo do ficheiro de utilizadores desencriptado numa unica string
     * @throws IOException
     * @throws InvalidKeyException
     * @throws InvalidAlgorithmParameterException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     * @throws BadPaddingException
     * @throws IllegalBlockSizeException
     */
    public String loadUserFile() throws IOException, InvalidKeyException, InvalidAlgorithmParameterException,
            NoSuchAlgorithmException, NoSuchPaddingException, BadPaddingException, IllegalBlockSizeException {
        byte[] enc = Base64.getDecoder().decode(readFile(USERS_PATH));
        byte[] params = readFile(PARAMS_PATH);
        AlgorithmParameters p = AlgorithmParameters.getInstance(ALGORITHM);
        p.init(params);

        Cipher c = Cipher.getInstance(ALGORITHM);
        c.init(Cipher.DECRYPT_MODE, key, p);
        byte[] dec = c.doFinal(enc);
        return new String(dec, StandardCharsets.UTF_8);
    }

    /**
     * Método que gera um salt aleatório e guarda-o num ficheiro
     * 
     * @return Salt gerado
     * @throws IOException
     */
    private byte[] getSalt() throws IOException {
        File saltFile = new File(SALT_PATH);
        if (saltFile.exists()) {
            return readFile(SALT_PATH);
        }
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        writeToFile(SALT_PATH, salt);
        return salt;
    }

    /**
     * Método que gera um número aleatório de iterações e guarda-o num ficheiro
     * 
     * @return Número de iterações gerado
     * @throws IOException
     */
    private int getIterations() throws IOException {
        File itFile = new File(ITERATIONS_PATH);
        if (itFile.exists()) {
            try {
                BufferedReader br = new BufferedReader(new FileReader(itFile));
                String line = br.readLine();
                br.close();
                return Integer.parseInt(line);
            } catch (Exception e) {
                System.err.println("Error reading iterations");
            }
        }
        int iterations = random.nextInt(1000) + 10000;
        writeToFile(ITERATIONS_PATH, Integer.toString(iterations).getBytes());
        return iterations;

    }

    /**
     * Método que lê um ficheiro e devolve o seu conteúdo
     * 
     * @param filename O path do ficheiro
     * @return Conteúdo do ficheiro em bytes
     * @throws IOException
     */
    private byte[] readFile(String filename) throws IOException {
        File file = new File(filename);
        if (!file.exists()) {
            return null;
        }
        FileInputStream fis = new FileInputStream(file);
        byte[] b = new byte[(int) file.length()];
        fis.read(b);
        fis.close();
        return b;
    }

    /**
     * Método que escreve num ficheiro
     * 
     * @param filename O path do ficheiro
     * @param data     Dados a serem escritos
     * @throws IOException
     */
    private static void writeToFile(String filename, byte[] data) throws IOException {
        File file = new File(filename);
        FileOutputStream fos = new FileOutputStream(file);
        fos.write(data);
        fos.close();
    }
}
