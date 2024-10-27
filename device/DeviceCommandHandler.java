import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

/**
 * Classe que representa um handler para o controlo de comandos de um
 * dispositivo
 * 
 * @author Martim Pereira fc58223
 * @author João Pereira fc58189
 * @author Daniel Nunes fc58257
 */
public class DeviceCommandHandler {

    private ObjectInputStream in;
    private ObjectOutputStream out;
    private String username;
    private String id;
    private KeyStore keystore;
    private KeyStore truststore;
    private String keystorePassword;

    /**
     * Construtor da classe
     * 
     * @author Martim Pereira fc58223
     * @author João Pereira fc58189
     * @author Daniel Nunes fc58257
     * 
     * @param in               ObjectInputStream
     * @param out              ObjectOutputStream
     * @param username         username
     * @param id               id do device
     * @param keystore         ficheiro keystore
     * @param truststore       ficheiro truststore
     * @param keystorePassword password do keystore
     */
    public DeviceCommandHandler(ObjectInputStream in, ObjectOutputStream out, String username, String id,
            KeyStore keystore,
            KeyStore truststore, String keystorePassword) {
        this.in = in;
        this.out = out;
        this.username = username;
        this.id = id;
        this.keystore = keystore;
        this.truststore = truststore;
        this.keystorePassword = keystorePassword;
    }

    /* --------------------------- REQUEST OPERATIONS --------------------------- */

    /**
     * Método que cria um domínio
     * 
     * @param domainName nome do domínio
     */
    public void createDomainRequest(String domainName) {
        Message msg = new Message();

        msg.setCommand("CREATE");
        msg.setDomain(domainName);
        Utils.writeMessage(msg, out);
        msg = Utils.readMessage(this.in);
        System.out.println("Response: " + msg.getCode().getDescription());

    }

    /**
     * Método que adiciona um utilizador a um domínio
     * 
     * @param user           O nome do utilizador
     * @param domain         O nome do domínio
     * @param domainPassword A password do domínio
     * @throws IOException
     * @throws KeyStoreException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     * @throws InvalidKeyException
     * @throws IllegalBlockSizeException
     */
    public void addUserToDomainRequest(String user, String domain, String domainPassword)
            throws IOException, KeyStoreException, NoSuchAlgorithmException, NoSuchPaddingException,
            InvalidKeyException, IllegalBlockSizeException {
        Message msg = new Message();
        msg.setCommand("ADD");
        msg.setUser(user);
        msg.setDomain(domain);

        Certificate userCert = truststore.getCertificate(user);

        if (userCert == null) {
            Message msg2 = new Message();
            msg2.setCommand("GET_CERTIFICATE");
            msg2.setUser(user);
            Utils.writeMessage(msg2, this.out);

            msg2 = Utils.readMessage(this.in);

            if (msg2.getCode() == MessageCode.OK) {
                userCert = msg2.getCertificate();
            } else {
                System.out.println("Response: " + msg2.getCode().getDescription());
                return;
            }
        }

        PublicKey userPubKey = userCert.getPublicKey();

        Object[] objs = generateWrappedDomainKey(userPubKey, domain, domainPassword);
        byte[] wrappedKey = (byte[]) objs[0];
        byte[] salt = (byte[]) objs[1];
        int iterations = (int) objs[2];

        msg.setData(wrappedKey);
        Utils.writeMessage(msg, this.out);

        msg = Utils.readMessage(this.in);
        System.out.println("Response: " + msg.getCode().getDescription());

        if (msg.getCode() == MessageCode.OK) {
            String params = domain + " " + Base64.getEncoder().encodeToString(salt) + " " + iterations + "\n";
            saveDomainKeyParams(params);
        }
    }

    /**
     * Método que gera a chave de domínio encripatada
     * 
     * @param userPubKey     chave pública do utilizador
     * @param domain         nome do domínio
     * @param domainPassword password do domínio
     * @return um array de objetos com a chave encriptada, o salt e as iterações
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     * @throws InvalidKeyException
     * @throws IllegalBlockSizeException
     * @throws IOException
     */
    private Object[] generateWrappedDomainKey(PublicKey userPubKey, String domain, String domainPassword)
            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException,
            IOException {

        byte[] salt = null;
        int iterations = 0;

        File paramsFile = new File("params_domain_key.txt");
        if (paramsFile.exists()) {

            synchronized (this) {
                List<String> lines = Files.readAllLines(Paths.get("params_domain_key.txt"));

                for (String line : lines) {
                    String[] parts = line.split(" ");
                    if (domain.equals(parts[0])) {
                        salt = Base64.getDecoder().decode(parts[1]);
                        iterations = Integer.parseInt(parts[2]);
                        break;
                    }
                }
            }
        }

        if (salt == null) {
            salt = Utils.generateSalt();
            iterations = Utils.generateIterations();
        }

        SecretKey domainKey = Utils.generateSecretKey(domainPassword, salt, iterations);

        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.WRAP_MODE, userPubKey);
        byte[] wrappedKey = cipher.wrap(domainKey);

        return new Object[] { wrappedKey, salt, iterations };

    }

    /**
     * Método que guarda os parametros da chave de domínio
     * 
     * @param params parametros da chave de domínio
     * @throws IOException
     */
    private void saveDomainKeyParams(String params) throws IOException {
        Path filePath = Paths.get("params_domain_key.txt");
        boolean exists = Files.exists(filePath);
        if (exists) {
            String content = new String(Files.readAllBytes(filePath));
            if (!content.contains(params)) {
                synchronized (this) {
                    Files.write(filePath, params.getBytes(), StandardOpenOption.APPEND);
                }
            }
        } else {
            synchronized (this) {
                Files.write(filePath, params.getBytes(), StandardOpenOption.CREATE);
            }
        }
    }

    /**
     * Método que regista um dispositivo num domínio se possivel
     * 
     * @param domain nome do domínio
     */
    public void registerDeviceRequest(String domain) {
        Message msg = new Message();
        msg.setCommand("RD");
        msg.setDomain(domain);
        Utils.writeMessage(msg, this.out);
        msg = Utils.readMessage(this.in);
        System.out.println("Response: " + msg.getCode().getDescription());
    }

    /**
     * Método que regista uma temperatura se possivel
     * 
     * @param temperature temperatura a ser registada
     * @throws ClassNotFoundException
     * @throws InvalidKeyException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     * @throws IOException
     * @throws UnrecoverableKeyException
     * @throws KeyStoreException
     */
    public void registerTemperatureRequest(float temperature)
            throws ClassNotFoundException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException,
            IllegalBlockSizeException, BadPaddingException, IOException, UnrecoverableKeyException, KeyStoreException {
        Message msg = new Message();
        msg = obtainDomains(username, id);
        List<String> domains = msg.getDomains();
        List<byte[]> userKeys = msg.getDataList();

        if (domains == null) {
            System.out.println("Device is not registered in any domain!");
            return;
        }

        byte[] temp = ByteBuffer.allocate(Float.BYTES).putFloat(temperature).array();

        List<byte[]> temps = new ArrayList<>();
        List<byte[]> paramsTemp = new ArrayList<>();

        PrivateKey privateKey = (PrivateKey) keystore.getKey(username, keystorePassword.toCharArray());

        for (byte[] domainKey : userKeys) {

            if (domainKey != null) {

                // decifrar chave com a nossa private key
                Cipher cipher = Cipher.getInstance("RSA");
                cipher.init(Cipher.UNWRAP_MODE, privateKey);
                SecretKey domainKeyDec = (SecretKey) cipher.unwrap(domainKey, "PBEWithHmacSHA256AndAES_128",
                        Cipher.SECRET_KEY);

                Cipher cipher2 = Cipher.getInstance("PBEWithHmacSHA256AndAES_128");
                cipher2.init(Cipher.ENCRYPT_MODE, domainKeyDec);

                temps.add(cipher2.doFinal(temp));
                byte[] params = cipher2.getParameters().getEncoded();
                paramsTemp.add(params);
            }
        }

        msg.setCommand("ET");
        msg.setDomains(domains);
        msg.setDataList(temps);
        msg.setParams(paramsTemp);
        Utils.writeMessage(msg, this.out);

        msg = Utils.readMessage(this.in);

        System.out.println("Response: " + msg.getCode().getDescription());
    }

    /**
     * Método que regista uma imagem relacionada a um device se possivel
     * 
     * @param filename nome do ficheiro da imagem
     * @throws ClassNotFoundException
     * @throws IOException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     * @throws InvalidKeyException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     * @throws UnrecoverableKeyException
     * @throws KeyStoreException
     */
    public void registerImageRequest(String filename) throws ClassNotFoundException, IOException,
            IllegalBlockSizeException, BadPaddingException, InvalidKeyException, NoSuchAlgorithmException,
            NoSuchPaddingException, UnrecoverableKeyException, KeyStoreException {
        byte[] data = Utils.getFileContents(filename);
        if (data == null) {
            System.out.println("File not found!");
            return;
        }

        Message msg = new Message();
        msg = obtainDomains(username, id);
        List<String> domains = msg.getDomains();
        List<byte[]> userKeys = msg.getDataList();

        if (domains == null) {
            System.out.println("Device is not registered in any domain!");
            return;
        }

        List<byte[]> images = new ArrayList<>();
        List<byte[]> paramsImages = new ArrayList<>();

        PrivateKey privateKey = (PrivateKey) keystore.getKey(username, keystorePassword.toCharArray());

        if (userKeys == null) {
            System.out.println("Device is not registered in any domain!");
            return;
        }

        for (byte[] domainKey : userKeys) {

            if (domainKey != null) {

                // decifrar chave com a nossa private key
                Cipher cipher = Cipher.getInstance("RSA");
                cipher.init(Cipher.UNWRAP_MODE, privateKey);
                SecretKey domainKeyDec = (SecretKey) cipher.unwrap(domainKey, "PBEWithHmacSHA256AndAES_128",
                        Cipher.SECRET_KEY);

                Cipher cipher2 = Cipher.getInstance("PBEWithHmacSHA256AndAES_128");
                cipher2.init(Cipher.ENCRYPT_MODE, domainKeyDec);

                images.add(cipher2.doFinal(data));
                byte[] params = cipher2.getParameters().getEncoded();
                paramsImages.add(params);
            }
        }

        msg.setCommand("EI");
        msg.setDomains(domains);
        msg.setDataList(images);
        msg.setParams(paramsImages);
        Utils.writeMessage(msg, this.out);

        msg = Utils.readMessage(this.in);
        System.out.println("Response: " + msg.getCode().getDescription());
    }

    /**
     * Método que retorna a temperatura de um domínio
     * 
     * @param domain nome do domínio
     * @throws UnrecoverableKeyException
     * @throws KeyStoreException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     * @throws InvalidKeyException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     */
    public void retriveTemperatureRequest(String domain)
            throws UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException, NoSuchPaddingException,
            InvalidKeyException, IllegalBlockSizeException, BadPaddingException {

        Message msg = new Message();

        msg.setCommand("RT");
        msg.setDomain(domain);
        Utils.writeMessage(msg, this.out);

        msg = Utils.readMessage(this.in);

        if (msg.getCode() == MessageCode.NO_PERM) {
            System.out.println("Response: " + msg.getCode().getDescription() + " de leitura");
        } else if (msg.getCode() == MessageCode.NO_DATA) {
            System.out.println("Response: " + msg.getCode().getDescription()
                    + " # dominio não tem dados de temperatura");
        } else if (msg.getCode() == MessageCode.OK) {

            byte[] encriptedDomainKey = msg.getDomainKey();

            List<byte[]> params = msg.getParams();

            PrivateKey privateKey = (PrivateKey) keystore.getKey(username, keystorePassword.toCharArray());

            // decifrar chave com a nossa private key
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.UNWRAP_MODE, privateKey);
            Key domainKeyDec = cipher.unwrap(encriptedDomainKey, "PBEWithHmacSHA256AndAES_128", Cipher.SECRET_KEY);

            if (Utils.createDir("device/devicesData")) {
                Utils.writeByteArrayToFile(msg.getData(),
                        "device/devicesData/" + domain + "_temp.txt", domainKeyDec, params);

                System.out.println(
                        "Response: " + msg.getCode().getDescription() + ", " + msg.getSize()
                                + " (long)." +
                                "File was saved in /device/devicesData with the name "
                                + domain + "_temp.txt");
            }
        } else {
            System.out.println("Response: " + msg.getCode().getDescription());
        }
    }

    /**
     * Método que retorna uma imagem de um dispositivo
     * 
     * @param devName nome do dispositivo
     * @throws NumberFormatException
     * @throws IOException
     * @throws UnrecoverableKeyException
     * @throws KeyStoreException
     * @throws NoSuchAlgorithmException
     * @throws UnrecoverableKeyException
     * @throws KeyStoreException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     * @throws InvalidKeyException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     * @throws InvalidAlgorithmParameterException
     */
    public void retriveImageRequest(String devName) throws NumberFormatException, IOException,
            UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException,
            KeyStoreException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
            IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException {

        String[] parts = devName.split(":");

        if (parts.length == 1) {
            System.out.println("Wrong format for command RI");
            System.out.println("Right format -> RI <user-id>:<dev_id>");
            return;
        }
        if (parts[1] == "") {
            System.out.println("Wrong format for command RI");
            System.out.println("Right format -> RI <user-id>:<dev_id>");
            return;
        }

        Message msg = new Message();
        msg.setCommand("RI");
        msg.setUser(parts[0]);
        msg.setDevId(parts[1]);
        Utils.writeMessage(msg, this.out);

        msg = Utils.readMessage(this.in);

        if (msg.getCode() == MessageCode.NO_PERM) {
            System.out.println("Response: " + msg.getCode().getDescription() + " de leitura");
        } else if (msg.getCode() == MessageCode.NO_DATA) {
            System.out.println("Response: " + msg.getCode().getDescription()
                    + " # dispositivo não tem dados de imagem");
        } else if (msg.getCode() == MessageCode.OK) {

            byte[] encriptedDomainKey = msg.getDomainKey();
            byte[] value = msg.getData();
            byte[] params = msg.getParam();

            PrivateKey privateKey = (PrivateKey) keystore.getKey(username, keystorePassword.toCharArray());

            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.UNWRAP_MODE, privateKey);
            Key domainKeyDec = cipher.unwrap(encriptedDomainKey, "PBEWithHmacSHA256AndAES_128", Cipher.SECRET_KEY);

            AlgorithmParameters p = AlgorithmParameters.getInstance("PBEWithHmacSHA256AndAES_128");
            p.init(params);

            cipher = Cipher.getInstance("PBEWithHmacSHA256AndAES_128");
            cipher.init(Cipher.DECRYPT_MODE, domainKeyDec, p);
            value = cipher.doFinal(value);

            if (Utils.createDir("device/devicesData")) {
                String filename = devName.replace(":", "_");
                String path = "device/devicesData/" + filename + ".jpg";
                File received = new File(path);
                FileOutputStream fos = new FileOutputStream(received);
                fos.write(value, 0, value.length);
                System.out.println(
                        "Response: " + msg.getCode().getDescription() + ", " + value.length
                                + " (long)." +
                                "File was saved in /device/devicesData with the name "
                                + filename + ".jpg");
                fos.close();
            }

        } else {
            System.out.println("Response: " + msg.getCode().getDescription());
        }

    }

    /**
     * Método que retorna os domínios associados ao dispositivo
     * 
     * @throws ClassNotFoundException
     * @throws IOException
     */
    public void myDomainsRequest() throws ClassNotFoundException, IOException {
        Message msg = new Message();
        msg = obtainDomains(username, id);
        if (msg.getCode() == MessageCode.OK) {
            System.out.println("Device belongs to the following domains:");
            for (String domain : msg.getDomains()) {
                System.out.println(" -> " + domain);
            }
        } else if (msg.getCode() == MessageCode.NO_DATA) {
            System.out.println("Device is not associated with any domain.");
        } else {
            System.out.println("Error while getting domains");
        }

    }

    // ------------------------------------------------------------------------------------------------------------------------------------------------------//
    // -------------------------------------------------------------------UtilityFunctions-------------------------------------------------------------------//
    // ------------------------------------------------------------------------------------------------------------------------------------------------------//

    /**
     * Método que imprime o menu
     */
    public void printMenu() {
        System.out.println("Menu:");
        System.out.println("- CREATE <dm> -> cria dominio");
        System.out.println(
                "- ADD ADD <user1> <dm> <password-dominio> -> Adicionar utilizador <user1> ao domínio <dm> que tem password <password-dominio>");
        System.out.println("- RD <dm> -> Registar o Dispositivo atual no dominio <dm>");
        System.out.println("- ET <float> -> Enviar valor <float> de Temperatura para o servidor.");
        System.out.println("- EI <filename.jpg> -> Enviar Imagem <filename.jpg> para o servidor.");
        System.out.println(
                "- RT <dm> -> Receber as ultimas medicoes de Temperatura de cada dispositivo do dominio <dm>, desde que o utilizador tenha permissoes.");
        System.out.println(
                "- RI <user-id>:<dev_id> # Receber o ficheiro Imagem do dispositivo <userid>:<dev_id> do servidor, desde que o utilizador tenha permissoes.");
        System.out.println("- MYDOMAINS # Imprime a lista de dominios que o dispositivo pertence.");
    }

    /**
     * Metodo que obtem a mensagem com o código dado pelo servidor
     * 
     * @param username O username do utilizador
     * @param id       O id do utilizador
     * @return A mensagem com a resposta do servidor
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private Message obtainDomains(String username, String id) throws IOException, ClassNotFoundException {
        Message msg = new Message();
        msg.setCommand("MYDOMAINS");
        msg.setUser(username);
        msg.setDevId(id);
        Utils.writeMessage(msg, this.out);

        msg = Utils.readMessage(this.in);

        return msg;
    }

    /**
     * Metodo que fecha o cliente
     */
    public void closeClient() {
        Message msg = new Message();
        msg.setCommand("EXIT");
        try {
            Utils.writeMessage(msg, this.out);
            in.readObject();
        } catch (Exception e) {
            System.out.println("Client closed!");
        }
    }

}
