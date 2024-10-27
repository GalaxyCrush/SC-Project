import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Classe que trata da leitura e escrita do ficheiro de domínios.
 * 
 * @author Martim Pereira fc58223
 * @author João Pereira fc58189
 * @author Daniel Nunes fc58257
 */
public class DomainFileHandler {

    private String passCipher;

    private final String DOMAINS_PATH = "server/serverFiles/domains.txt";

    /**
     * Construtor da classe DomainFileHandler.
     * 
     * @param password password para cifrar o ficheiro de domínios.
     */
    public DomainFileHandler(String password) {
        this.passCipher = password;
    }

    /**
     * Método que carrega os domínios do ficheiro de domínios.
     * 
     * @param usersList lista de utilizadores.
     * @return lista de domínios.
     * @throws IOException              exceção de I/O.
     * @throws NoSuchAlgorithmException exceção de algoritmo inexistente.
     * @throws InvalidKeyException      exceção de chave inválida.
     */
    public List<Domain> loadDomainFile(Set<User> usersList)
            throws IOException, NoSuchAlgorithmException, InvalidKeyException {
        List<String> domainFile = Files.readAllLines(Paths.get(DOMAINS_PATH));

        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(this.passCipher.getBytes(), "HmacSHA256");
        mac.init(secretKey);

        List<Domain> domainsToLoad = new ArrayList<>();

        for (int i = 0; i < domainFile.size() - 1; i++) {
            String line = domainFile.get(i);
            mac.update(line.getBytes());
            String[] parts = line.split(";");
            String domainName = parts[0];
            String owner = parts[1];
            String devices = parts[2];
            String users = parts[3];

            Domain domain = new Domain(domainName,
                    usersList.stream().filter(u -> u.getUserId().equals(owner)).findFirst().get());
            loadDomainDeviceData(domain, devices);
            loadDomainUsers(domain, users, usersList);
            domainsToLoad.add(domain);
        }

        String hmacS = domainFile.get(domainFile.size() - 1);
        byte[] fileHmac = Utils.hexToBytes(hmacS);

        byte[] calculatedHmac = mac.doFinal();
        if (!Arrays.equals(calculatedHmac, fileHmac)) {
            System.out.println("Domains file integrity NOT verified");
            return null;
        }

        return domainsToLoad;

    }

    /**
     * Método que carrega os dispositivos de um domínio.
     * 
     * @param domain  domínio.
     * @param devices dispositivos.
     * @throws IOException exceção de I/O.
     */
    private void loadDomainDeviceData(Domain domain, String devices) throws IOException {
        String domainName = domain.getName();
        for (String device : devices.substring(1, devices.length() - 1).split(", ")) {

            domain.registerDevice(device);

            if (Files.exists(Paths.get(
                    "server/serverFiles/data/" + domainName + "/" + device.replace(':', '_') + "/temp.txt"))) {
                byte[] temp = Files.readAllBytes(Paths.get("server/serverFiles/data/" + domainName + "/"
                        + device.replace(':', '_') + "/temp.txt"));
                byte[] tempParams = Files.readAllBytes(Paths.get("server/serverFiles/data/" + domainName + "/"
                        + device.replace(':', '_') + "/tempParams.txt"));
                domain.registerTempToDevice(device, temp, tempParams);
            }

            if (Files.exists(Paths.get(
                    "server/serverFiles/data/" + domainName + "/" + device.replace(':', '_') + "/image.txt"))) {
                byte[] image = Files.readAllBytes(Paths.get("server/serverFiles/data/" + domainName + "/"
                        + device.replace(':', '_') + "/image.txt"));
                byte[] imageParams = Files.readAllBytes(Paths.get("server/serverFiles/data/" + domainName + "/"
                        + device.replace(':', '_') + "/imageParams.txt"));
                domain.registerImageToDevice(device, image, imageParams);
            }

        }

    }

    /**
     * Método que carrega os utilizadores de um domínio.
     * 
     * @param domain    domínio.
     * @param users     utilizadores.
     * @param usersList lista de utilizadores.
     * @throws IOException exceção de I/O.
     */
    private void loadDomainUsers(Domain domain, String users, Set<User> usersList) throws IOException {

        String domainName = domain.getName();
        for (String user : users.substring(1, users.length() - 1).split(", ")) {
            if (Files.exists(Paths.get(
                    "server/serverFiles/data/" + domainName + "/users/" + user.replace('.', '_') + ".txt"))) {
                byte[] domainKey = Files.readAllBytes(Paths.get(
                        "server/serverFiles/data/" + domainName + "/users/" + user.replace('.', '_') + ".txt"));
                domain.addUser(usersList.stream().filter(u -> u.getUserId().equals(user)).findFirst().get(),
                        domainKey);
            }

        }

    }

    /**
     * Método que faz backup dos domínios.
     * 
     * @param domainsList lista de domínios.
     * @throws FileNotFoundException    exceção de ficheiro não encontrado.
     * @throws NoSuchAlgorithmException exceção de algoritmo inexistente.
     * @throws InvalidKeyException      exceção de chave inválida.
     */
    public void backupDomainFile(Set<Domain> domainsList)
            throws FileNotFoundException, NoSuchAlgorithmException, InvalidKeyException {
        PrintWriter pw = new PrintWriter(DOMAINS_PATH);
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(this.passCipher.getBytes(), "HmacSHA256");
        mac.init(secretKey);

        for (Domain d : domainsList) {
            pw.println(d.toString());
            mac.update(d.toString().getBytes());

            backupDomainDeviceData(d);
            backupDomainUsers(d);

        }

        pw.print(Utils.bytesToHex(mac.doFinal()));
        pw.close();

    }

    /**
     * Método que faz backup dos dispositivos de um domínio.
     * 
     * @param domain domínio.
     */
    private void backupDomainDeviceData(Domain domain) {

        String domainName = domain.getName();

        for (Map.Entry<String, DeviceData> entry : domain.getDevices().entrySet()) {
            String device = entry.getKey();
            DeviceData value = entry.getValue();

            Utils.createDir("server/serverFiles/data/" + domainName + "/" + device.replace(':', '_'));

            if (value.getTemp() != null) {
                Utils.createBinaryFile(
                        "server/serverFiles/data/" + domainName + "/" + device.replace(':', '_') + "/temp.txt",
                        value.getTemp());
                Utils.createBinaryFile(
                        "server/serverFiles/data/" + domainName + "/" + device.replace(':', '_')
                                + "/tempParams.txt",
                        value.getTempParams());
            }

            if (value.getImage() != null) {
                Utils.createBinaryFile(
                        "server/serverFiles/data/" + domainName + "/" + device.replace(':', '_')
                                + "/image.txt",
                        value.getImage());
                Utils.createBinaryFile(
                        "server/serverFiles/data/" + domainName + "/" + device.replace(':', '_')
                                + "/imageParams.txt",
                        value.getImageParams());
            }

        }

    }

    /**
     * Método que faz backup dos utilizadores de um domínio.
     * 
     * @param domain domínio.
     */
    private void backupDomainUsers(Domain domain) {
        String domainName = domain.getName();
        Utils.createDir("server/serverFiles/data/" + domainName + "/users");
        for (Map.Entry<User, byte[]> entry : domain.getUsers().entrySet()) {
            User u = entry.getKey();
            byte[] key = entry.getValue();
            Utils.createBinaryFile(
                    "server/serverFiles/data/" + domainName + "/users/" + u.getUserId().replace('.', '_') + ".txt",
                    key);
        }
    }
}
