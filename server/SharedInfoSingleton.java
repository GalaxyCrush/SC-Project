
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateFactory;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Classe que representa um Singleton que contem a informacao partilhada entre
 * as diferentes threads
 * 
 * @author Martim Pereira fc58223
 * @author João Pereira fc58189
 * @author Daniel Nunes fc58257
 */
public class SharedInfoSingleton {

    private final String DOMAINS_PATH = "server/serverFiles/domains.txt";
    private final String USERS_PATH = "server/serverFiles/users.txt";

    private static volatile SharedInfoSingleton sharedInfo = null;

    private Set<Domain> domainsList;
    private Set<String> devicesList; // devices ativos no momento
    private Set<User> usersList;

    private String twoFactorAuthKey;

    private UserFileHandler userFileHandler;
    private DomainFileHandler domainFileHandler;

    private KeyStore keystore;

    private String passCipher;

    /**
     * Construtor privado para impedir inicializacao
     */
    private SharedInfoSingleton(String passwString, String twoFactorAuthKey, String keystoreFile,
            String keystorePassword) {
        domainsList = new HashSet<>();
        devicesList = new HashSet<>();
        usersList = new HashSet<>();
        this.twoFactorAuthKey = twoFactorAuthKey;
        this.passCipher = passwString;

        try {

            this.keystore = KeyStore.getInstance("JCEKS");
            try (InputStream keystoreStream = new FileInputStream(keystoreFile)) {
                keystore.load(keystoreStream, keystorePassword.toCharArray());
            }
            this.userFileHandler = new UserFileHandler(passwString);
            this.domainFileHandler = new DomainFileHandler(passwString);

        } catch (Exception e) {
            System.err.println("Error loading persistance data");
        }
    }

    /**
     * Metodo que retorna a instancia do singleton
     * Caso ainda não exista uma instancia, cria-a
     * 
     * @return Instancia do singleton
     */
    public synchronized static SharedInfoSingleton getInstance(String passwString, String twoFactorAuthKey,
            String keystoreFile, String keystorePassword) {
        if (sharedInfo == null) {
            synchronized (SharedInfoSingleton.class) {
                if (sharedInfo == null) {
                    sharedInfo = new SharedInfoSingleton(passwString, twoFactorAuthKey, keystoreFile, keystorePassword);
                    if (sharedInfo.loadInfo()) {
                        System.out.println("Info loaded successfully");
                    } else {
                        return null;
                    }
                }
            }
        }
        return sharedInfo;
    }

    /**
     * Método que retorna a twoFactorAuthKey
     * 
     * @return String com a twoFactorAuthKey
     */
    public synchronized String getTwoFactorAuthKey() {
        return this.twoFactorAuthKey;
    }

    public synchronized String getPassCipher() {
        return this.passCipher;
    }

    /**
     * Metodo que carrega a informacao dos ficheiros
     */
    public boolean loadInfo() {
        loadUsers();
        return loadDomain();
    }

    /**
     * Método que lê a inforamção dos user a partir de um ficheiro, criando então os
     * users com o ceu certificado e adicionando-os à lista de users
     */
    private synchronized void loadUsers() {
        File file = new File(USERS_PATH);
        if (!file.exists()) {
            return;
        }
        try {

            String content = this.userFileHandler.loadUserFile();

            for (String line : content.split("\n")) {
                String[] parts = line.split(":");
                String userId = parts[0].trim();
                String certFileName = parts[1].trim();
                Certificate cert = loadCertificate("server/serverFiles/certificates/" + certFileName);
                User user = new User(userId, cert);
                usersList.add(user);
            }

        } catch (Exception e) {
            System.err.println("Error loading users from file");
        }
    }

    /**
     * Metodo que faz o load de um certificado a partir do certificado em memória
     * 
     * @param certPath O path do certificado
     * @return O certificado carregado
     */
    private synchronized Certificate loadCertificate(String certPath) {
        try {
            InputStream inStream = Files.newInputStream(Paths.get(certPath));
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            Certificate cert = cf.generateCertificate(inStream);
            inStream.close();
            return cert;
        } catch (Exception e) {
            System.err.println("Error loading certificate from file");
        }
        return null;
    }

    /**
     * Metodo que faz o load dos dominios a partir das informações
     * presentes no ficheiro dos dominios de modo a garantir persistência
     * 
     * @return true se o load foi bem sucedido false caso contrário
     */
    public synchronized boolean loadDomain() {
        File file = new File(DOMAINS_PATH);
        if (!file.exists()) {
            return true;
        }

        try {
            List<Domain> domainsToLoad = this.domainFileHandler.loadDomainFile(usersList);
            this.domainsList.addAll(domainsToLoad);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Metodo que faz o backup da informacao antes de desligar o server de modo a
     * garantir persistência
     */
    public void backupInfo() {
        backupUsers();
        backupDomains();
    }

    /**
     * Metodo que faz o backup dos utilizadores para o ficheiro de utilizadores
     */
    private synchronized void backupUsers() {

        File file = new File("server/serverFiles");
        if (!file.exists()) {
            Utils.createDir("server/serverFiles");
        }

        try {

            String content = "";

            for (User u : usersList) {
                String certFileName = backupCertificate(u);
                content += u.getUserId() + ":" + certFileName + "\n";
            }

            if (content.length() > 0) {
                this.userFileHandler.backupUserFile(content);
            }

        } catch (Exception e) {
            System.err.println("Error creating backup of domains");
        }

    }

    /**
     * Metodo que faz o backup de um certificado para o ficheiro de certificados
     * 
     * @param u O dono do certificado
     * @return O nome do ficheiro do certificado
     * @throws CertificateEncodingException
     * @throws IOException
     */
    private synchronized String backupCertificate(User u) throws CertificateEncodingException, IOException {
        File file = new File("server/serverFiles/certificates");
        if (!file.exists()) {
            Utils.createDir("server/serverFiles/certificates");
        }

        Certificate cert = u.getCertificate();
        byte[] b = cert.getEncoded();
        String certFileName = u.getUserId() + ".cer";

        Files.write(Paths.get("server/serverFiles/certificates/" + certFileName), b);

        return certFileName;
    }

    /**
     * Metodo que faz o backup dos dominios para o ficheiro de dominios de modo a
     * garantir persistência
     */
    private synchronized void backupDomains() {
        File file = new File("server/serverFiles/data");
        if (!file.exists()) {
            Utils.createDir("server/serverFiles/data");
        }

        if (domainsList.size() == 0) {
            return;
        }

        try {
            this.domainFileHandler.backupDomainFile(domainsList);
        } catch (Exception e) {
            System.err.println("Error creating backup of domains");
        }

    }

    /**
     * Metodo que retorna a lista de dominios
     * 
     * @return Lista de dominios
     */
    public synchronized Set<Domain> getDomains() {
        return domainsList;
    }

    /**
     * Metodo que adiciona um dominio à lista de dominios
     * 
     * @param domain Dominio a adicionar
     * @return true (as specified by Collection.add(E))
     */
    public synchronized boolean addDomain(Domain domain) {
        return domainsList.add(domain);
    }

    /**
     * Metodo que remove um dominio da lista de dominios
     * 
     * @param domain Dominio a remover
     * @return true se a lista continha o elemento a remover
     */
    public synchronized boolean removeDomain(Domain domain) {
        return domainsList.remove(domain);
    }

    /**
     * Metodo que retorna um dominio pelo seu nome
     * 
     * @param domainName Nome do dominio a procurar
     * @return Dominio com o nome procurado ou null caso nao exista
     */
    public synchronized Domain getDomain(String domainName) {
        for (Domain domain : domainsList) {
            if (domain.getName().equals(domainName)) {
                return domain;
            }
        }
        return null;
    }

    /**
     * Metodo que adiciona um dispositivo à lista de dispositivos
     * 
     * @param device Dispositivo a adicionar
     * @return true (as specified by Collection.add(E))
     */
    public synchronized boolean addDevice(String device) {
        return devicesList.add(device);
    }

    /**
     * Metodo que remove um dispositivo da lista de dispositivos
     * 
     * @param device Dispositivo a remover
     */
    public synchronized void removeDevice(String device) {
        this.devicesList.remove(device);
    }

    /**
     * Metodo que retorna um user pelo seu nome
     * 
     * @param userid Nome do user a procurar
     * @return User com o nome procurado ou null caso nao exista
     */
    public synchronized User getUserByName(String userid) {
        for (User user : usersList) {
            if (user.getUserId().equals(userid)) {
                return user;
            }
        }
        return null;
    }

    /**
     * Meodo que adiciona um user
     * 
     * @param user e-mail do User a ser adicionado
     * @return se foi criado com sucesso
     */
    public synchronized boolean addUser(User user) {
        return usersList.add(user);
    }

    /**
     * Metodo que retorna a lista de users
     * 
     * @return A lista de Users
     */
    public synchronized Set<User> getUsers() {
        return this.usersList;
    }

    /**
     * Metodo que verifica se um dispositivo existe
     * 
     * @param device Dispositivo a verificar
     * @return true se o dispositivo existe
     */
    public synchronized boolean hasDevice(String device) {
        return this.devicesList.contains(device);
    }
}