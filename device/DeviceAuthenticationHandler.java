import java.io.*;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * Classe que representa um handler para a autenticação de dispositivos
 * 
 * @author Martim Pereira fc58223
 * @author João Pereira fc58189
 * @author Daniel Nunes fc58257
 */
public class DeviceAuthenticationHandler {

    private Scanner sc = new Scanner(System.in);

    private ObjectInputStream in;
    private ObjectOutputStream out;
    private KeyStore keystore;
    private String keystorePassword;

    /**
     * Construtor da classe DeviceAuthenticationHandler
     * 
     * @param in               Stream de input
     * @param out              Stream de output
     * @param keystore         Keystore do dispositivo
     * @param keystorePassword Password da keystore
     */
    public DeviceAuthenticationHandler(ObjectInputStream in, ObjectOutputStream out, KeyStore keystore,
            String keystorePassword) {
        this.out = out;
        this.in = in;
        this.keystore = keystore;
        this.keystorePassword = keystorePassword;
    }

    /**
     * Metodo que inicia a autenticação 2FA do dispositivo
     * 
     * @return true se a autenticação foi bem sucedida, false caso contrário
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws KeyStoreException
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     * @throws UnrecoverableKeyException
     * @throws SignatureException
     */
    public boolean startTwoFactorAuthentication(String username)
            throws IOException, ClassNotFoundException, KeyStoreException,
            NoSuchAlgorithmException, InvalidKeyException, UnrecoverableKeyException, SignatureException {
        boolean authenticated = false;

        Message msg = new Message();

        do {
            out.reset();
            msg.clear();
            // ------------------------------------------------------- //
            // --------primeiro metodo de autenticacao - 4.2.1-------- //
            // ------------------------------------------------------- //

            msg.setUser(username);
            out.writeObject(msg);
            msg = (Message) in.readObject();
            Long receivedNonce = msg.getNonce();
            System.out.println("Received nonce: " + receivedNonce + "\n");

            Certificate certificate = keystore.getCertificate(username);
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign((PrivateKey) keystore.getKey(username, keystorePassword.toCharArray()));

            byte[] nonceBytes = ByteBuffer.allocate(Long.BYTES).putLong(receivedNonce).array();

            signature.update(nonceBytes);
            byte[] signatureBytes = signature.sign();

            // Enviar o nonce, a assinatura e o certificado para o servidor
            msg.setSignature(signatureBytes);

            if (msg.getCode() == MessageCode.OK_NEW_USER) {
                msg.setNonce(receivedNonce);
                msg.setCertificate(certificate);
            }

            out.writeObject(msg);

            msg = (Message) in.readObject();

            System.out.println("Server response: " + msg.getCode() + "\n");

            if (msg.getCode() != MessageCode.OK) {
                System.out.println("Authentication failed.");
                return false;
            }
            // ------------------------------------------------------- //
            // --------segundo metodo de autenticacao - 4.2.2--------- //
            // ------------------------------------------------------- //

            System.out.println("Introduza o código enviado pelo servidor:");

            try {
                String code = sc.nextLine();
                msg.clear();
                msg.setC2FA(code);
                out.writeObject(msg);
            } catch (NoSuchElementException e) {
                return false;
            }

            msg = (Message) in.readObject();

            if (msg.getCode() == MessageCode.OK) {
                System.out.println("Authenticated!");
                authenticated = true;
            } else {
                msg.clear();

                System.out.println("Authentication failed. Do you want to try again? (Y/N)");
                if (sc.hasNextLine() && sc.nextLine().toUpperCase().equals("N")) {
                    msg.setCommand("EXIT");
                    out.writeObject(msg);
                    return false;
                } else {
                    msg.setCommand("AGAIN");
                    out.writeObject(msg);
                }
            }
        } while (!authenticated);
        return true;
    }

    /**
     * Metodo que inicia a autenticação do executável
     * 
     * @return true se a autenticação foi bem sucedida, false caso contrário
     * @throws IOException
     * @throws URISyntaxException
     * @throws NoSuchAlgorithmException
     * @throws ClassNotFoundException
     */
    public boolean startExecutableAuthentication(String id)
            throws IOException, URISyntaxException, NoSuchAlgorithmException, ClassNotFoundException {

        Message msg = new Message();

        msg.setDevId(id);
        Utils.writeMessage(msg, this.out);

        msg = Utils.readMessage(this.in);

        if (msg.getCode() == MessageCode.NOK_DEVID) {
            return false;
        }

        Long receivedNonce = msg.getNonce();

        msg.clear();
        Path jarPath = Paths.get(IoTDevice.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        System.out.println("Received nonce: " + receivedNonce + "\n");
        byte[] fileBytes = Files.readAllBytes(jarPath);
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES + fileBytes.length);
        buffer.putLong(receivedNonce);
        buffer.put(fileBytes);
        byte[] concatBytes = buffer.array();

        // hash SHA-256
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashedBytes = digest.digest(concatBytes);

        msg.setData(hashedBytes);
        out.writeObject(msg);

        msg.clear();

        msg = Utils.readMessage(this.in);

        if (msg.getCode() == MessageCode.OK_TESTED) {
            System.out.println("Exec check was successful!");
        } else {
            System.out.println("Exec check failed!");
            return false;
        }
        return true;

    }

}
