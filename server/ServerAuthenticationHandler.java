import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.Certificate;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.List;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.HttpURLConnection;

/**
 * Classe que trata da autenticação de um user
 * 
 * @author Martim Pereira fc58223
 * @author João Pereira fc58189
 * @author Daniel Nunes fc58257
 */
public class ServerAuthenticationHandler {

    private SharedInfoSingleton info;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private User user = null;
    private String device = null;

    /**
     * Construtor da classe AuthenticationHandler
     * 
     * @param info informação partilhada
     * @param in   stream de entrada
     * @param out  stream de saída
     */
    public ServerAuthenticationHandler(SharedInfoSingleton info, ObjectInputStream in, ObjectOutputStream out) {
        this.info = info;
        this.in = in;
        this.out = out;
    }

    /* ------------------------------ Autenticacao ------------------------------ */

    /**
     * Método que processa a autenticação de um cliente através de um
     * AuthenticationHandler
     * - Autenticação do utilizador e password
     * - Autenticação do device id
     * - Autenticação do executável
     * 
     * @return true se a autenticação foi bem sucedida
     */
    protected AbstractMap.SimpleEntry<User, String> processAuthentication() {
        try {
            if (twoFactorAuthentication()) {
                if (execAuthentication()) {
                    return new AbstractMap.SimpleEntry<User, String>(this.user, this.device);
                }
            }
            return null;
        } catch (Exception e) {
            System.err.println("Error processing authentication: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Método que processa a autenticação de dois fatores
     * 
     * @return true se a autenticação foi bem sucedida
     */
    protected boolean twoFactorAuthentication() throws IOException {
        System.out.println("Starting Two Factor Authentication");

        int authenticated = 0;
        do {
            out.reset();
            boolean passed = assimetricCryptoAuthentication();
            if (!passed) {
                return false;
            }
            authenticated = emailAuthentication();
        } while (authenticated == 0);
        if (authenticated == -1) {
            return false;
        }
        return true;

    }

    /**
     * Método que processa a autenticação assimétrica
     */
    protected boolean assimetricCryptoAuthentication() {
        System.out.println("Starting Assimetric Crypto Authentication");
        try {
            Message msg = Utils.readMessage(this.in);
            String username = msg.getUser();

            msg.clear();

            Long nonce = Utils.generateNonce();
            msg.setNonce(nonce);

            User u = this.info.getUserByName(username);
            if (u == null) {
                // user n existe
                msg.setCode(MessageCode.OK_NEW_USER);
            } else {
                // user existe
                msg.setCode(MessageCode.OK_USER);
            }

            Utils.writeMessage(msg, this.out);

            msg = Utils.readMessage(this.in);

            if (msg.getCode() == MessageCode.OK_NEW_USER) {
                // caso 4.2.1 a)
                Long receivedNonce = msg.getNonce();

                if (!receivedNonce.equals(nonce)) {
                    msg.setCode(MessageCode.ERROR);
                    Utils.writeMessage(msg, this.out);
                    return false;
                } else {
                    byte[] signatureBytes = msg.getSignature();
                    Certificate userCertificate = msg.getCertificate();

                    PublicKey clientPublicKey = userCertificate.getPublicKey();
                    Signature signature = Signature.getInstance("SHA256withRSA");
                    signature.initVerify(clientPublicKey);
                    signature.update(ByteBuffer.allocate(Long.BYTES).putLong(nonce).array());

                    if (signature.verify(signatureBytes)) {
                        msg.setCode(MessageCode.OK);
                        this.user = new User(username, userCertificate);
                        System.out.println("User: " + user.getUserId() + " created");
                        info.addUser(user);
                        Utils.writeMessage(msg, this.out);
                        return true;
                    } else {
                        msg.setCode(MessageCode.ERROR);
                        Utils.writeMessage(msg, this.out);
                        return false;
                    }
                }

            } else if (msg.getCode() == MessageCode.OK_USER) {

                // caso 4.2.1 b)
                byte[] signatureBytes = msg.getSignature();

                PublicKey userPublicKey = u.getCertificate().getPublicKey();

                Signature signature = Signature.getInstance("SHA256withRSA");
                signature.initVerify(userPublicKey);
                signature.update(ByteBuffer.allocate(Long.BYTES).putLong(nonce).array());

                msg.clear();

                if (signature.verify(signatureBytes)) {
                    this.user = u;
                    msg.setCode(MessageCode.OK);
                    Utils.writeMessage(msg, this.out);
                    return true;
                } else {
                    msg.setCode(MessageCode.ERROR);
                    Utils.writeMessage(msg, this.out);
                    return false;
                }

            } else {
                System.err.println("Error in assimetricCryptoAuthentication");
                msg.setCode(MessageCode.ERROR);
                Utils.writeMessage(msg, this.out);
                return false;
            }

        } catch (Exception e) {
            System.err.println("Exception in assimetricCryptoAuthentication:");
            Message msg = new Message();
            msg.setCode(MessageCode.ERROR);
            Utils.writeMessage(msg, this.out);
            return false;
        }

    }

    /**
     * Método que processa a autenticação por email
     * 
     * @param user_id id do utilizador
     * @return 1 se a autenticação foi bem sucedida
     *         0 se a autenticação falhou
     *         -1 se ocorreu um erro
     */
    protected int emailAuthentication() {
        String c2fa = Utils.generateC2FA();
        String url = String.format("https://lmpinto.eu.pythonanywhere.com/2FA?e=%s&c=%s&a=%s", this.user.getUserId(),
                c2fa,
                info.getTwoFactorAuthKey());

        try {
            sendGetRequest(url);
        } catch (Exception e) {
            System.out.println("Error sending GET request to 2FA server");
            return -1;
        }

        Message msg = Utils.readMessage(this.in);

        if (msg.getC2FA() == null) {
            System.out.println("Error reading C2FA from client");
            return -1;
        }

        if (msg.getC2FA().equals(c2fa)) {
            msg.clear();
            msg.setCode(MessageCode.OK);
            Utils.writeMessage(msg, this.out);
            return 1;
        } else {
            msg.clear();
            msg.setCode(MessageCode.ERROR);
            Utils.writeMessage(msg, this.out);

            msg = Utils.readMessage(this.in);

            if (msg.getCommand() == "EXIT") {
                System.out.println("User exited the program");
                return -1;
            } else {
                msg.clear();
            }

            return 0;

        }

    }

    /**
     * Método que envia um pedido GET
     * 
     * @param urlStr url para onde enviar o pedido
     * @throws Exception
     */
    private void sendGetRequest(String urlStr) throws Exception {
        int responseCode = 0;
        while (responseCode != HttpURLConnection.HTTP_OK) {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            // Request method
            conn.setRequestMethod("GET");

            // Get the response
            responseCode = conn.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String inputLine;
                StringBuffer response = new StringBuffer();

                while ((inputLine = br.readLine()) != null) {
                    response.append(inputLine);
                }
                br.close();

                System.out.println("Response: " + response.toString());
            } else {
                Thread.sleep(1000);
            }
        }
    }

    /**
     * Método que autentica o executável
     * 
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws NoSuchAlgorithmException
     * @return true se a autenticação foi bem sucedida false caso contrário
     */
    protected boolean execAuthentication() throws IOException, ClassNotFoundException, NoSuchAlgorithmException {

        Message msg = Utils.readMessage(this.in);
        String user_devId = this.user.getUserId() + ":" + msg.getDevId();

        boolean added = info.addDevice(user_devId);

        if (added) {
            this.device = user_devId;
            msg.setCode(MessageCode.OK_DEVID);
            Long nonce = Utils.generateNonce();
            msg.setNonce(nonce);
            Utils.writeMessage(msg, this.out);
            return checkExecHash(nonce);
        } else {
            msg.setCode(MessageCode.NOK_DEVID);
            return false;
        }

    }

    /**
     * Método que verifica a integridade do executável
     * 
     * @param nonce nonce gerado para a verificação
     * @return true se a integridade foi verificada com sucesso false caso contrário
     * @throws NoSuchAlgorithmException
     */
    private boolean checkExecHash(Long nonce) throws NoSuchAlgorithmException {

        boolean check = checkLocalInfoFileIntegrity();

        if (!check) {
            Message msg = new Message();
            msg.setCode(MessageCode.NOK_TESTED);
            Utils.writeMessage(msg, this.out);
            Utils.readMessage(this.in);
            return false;
        }

        Message msg = Utils.readMessage(this.in);

        byte[] receivedHash = msg.getData();
        msg.clear();

        try (BufferedReader br = new BufferedReader(new FileReader("localInfo.txt"))) {
            String path;
            if ((path = br.readLine()) != null) {
                File f = new File(path);
                byte[] fileBytes = Files.readAllBytes(f.toPath());
                ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES + fileBytes.length);
                buffer.putLong(nonce);
                buffer.put(fileBytes);

                byte[] concatBytes = buffer.array();

                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] calculatedHash = digest.digest(concatBytes);
                if (Arrays.equals(calculatedHash, receivedHash)) {
                    msg.setCode(MessageCode.OK_TESTED);
                    Utils.writeMessage(msg, this.out);
                    return true;
                } else {
                    msg.setCode(MessageCode.NOK_TESTED);
                    Utils.writeMessage(msg, this.out);
                    return false;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        msg.setCode(MessageCode.ERROR);
        Utils.writeMessage(msg, this.out);
        return false;
    }

    /**
     * Método que verifica a integridade do ficheiro localInfo.txt
     * 
     * @return true se a integridade foi verificada com sucesso false caso contrário
     */
    private boolean checkLocalInfoFileIntegrity() {
        try {
            List<String> lines = Files.readAllLines(Paths.get("localInfo.txt"));

            if (lines.size() == 2) { // hmac ja existe

                Mac mac = Mac.getInstance("HmacSHA256");
                SecretKeySpec secretKey = new SecretKeySpec(this.info.getPassCipher().getBytes(), "HmacSHA256");

                mac.init(secretKey);

                byte[] decodedHmac = Utils.hexToBytes(lines.get(1).trim());

                mac.update(lines.get(0).trim().getBytes());

                // Decode the HMAC from Base64
                byte[] calculatedHmac = mac.doFinal();
                if (!Arrays.equals(calculatedHmac, decodedHmac)) {
                    System.out.println("Exec info file integrity NOT verified");
                    return false;
                } else {
                    System.out.println("Exec info file integrity verified");
                    return true;
                }

            } else {
                System.out.println("Something went wrong while checking localInfo.txt integrity...");
                return false;
            }

        } catch (IOException | NoSuchAlgorithmException | InvalidKeyException e) {
            e.printStackTrace();
            return false;
        }

    }
}
