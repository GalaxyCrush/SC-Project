import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

/**
 * Classe principal do servidor
 * 
 * @author Martim Pereira fc58223
 * @author João Pereira fc58189
 * @author Daniel Nunes fc58257
 */
public class IoTServer {

    private final static ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    /**
     * Main do servidor
     * 
     * @param args argumentos da linha de comando
     */
    public static void main(String[] args) {

        List<ServerThread> activeThreads = Collections.synchronizedList(new ArrayList<>());

        // scheduler.scheduleAtFixedRate(info::backupInfo, 10, 30, TimeUnit.SECONDS);

        int port;
        String passwordCipher;
        String keystoreFile;
        String keystorePassword;
        String twoFactorAuthKey;

        if (args.length == 0 || args.length > 5) {
            System.out.println("Usage: IoTServer <port> <password-cifra> <keystore> <password-keystore> <2FA-APIKey>");
            System.exit(-1);
        }

        try {
            port = Integer.parseInt(args[0]);
            passwordCipher = args[1];
            keystoreFile = args[2];
            keystorePassword = args[3];
            twoFactorAuthKey = args[4];
        } catch (NumberFormatException e) {
            port = 12345;
            passwordCipher = args[0];
            keystoreFile = args[1];
            keystorePassword = args[2];
            twoFactorAuthKey = args[3];
        }

        boolean b = prepareServer(passwordCipher);
        if (!b) {
            System.exit(-1);
        }

        SharedInfoSingleton info = SharedInfoSingleton.getInstance(passwordCipher, twoFactorAuthKey, keystoreFile,
                keystorePassword);

        if (info == null) {
            System.exit(-1);
        }

        try {

            SSLServerSocket serverSocket = beginConnection(keystoreFile, keystorePassword, port);

            System.out.println("Server running...");

            // Adiciona um hook para fechar os sockets e guardar a informação
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                for (ServerThread thread : activeThreads) {
                    thread.shutdown();
                }
                info.backupInfo();

                scheduler.shutdown();
                try {
                    if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                        scheduler.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    scheduler.shutdownNow();
                }

                try {
                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }));

            while (true) {
                Socket clientSocket = null;
                try {
                    clientSocket = serverSocket.accept();
                    ServerThread newServerThread = new ServerThread(clientSocket, info);
                    activeThreads.add(newServerThread);
                    newServerThread.start();

                } catch (IOException e) {
                    System.err.println(e.getMessage());
                    System.exit(-1);
                }

            }

        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
            System.exit(-1);

        }

    }

    /**
     * Metodo que vai iniciar a conexao com o servidor
     * 
     * @param keystoreFile     ficheiro keystore
     * @param keystorePassword password do keystore
     * @param port             porta do servidor
     * @return socket do servidor
     * @throws IOException
     */
    private static SSLServerSocket beginConnection(String keystoreFile, String keystorePassword, int port)
            throws IOException {

        System.setProperty("javax.net.ssl.keyStore", keystoreFile);
        System.setProperty("javax.net.ssl.keyStorePassword", keystorePassword);
        System.setProperty("javax.net.ssl.keyStoreType", "JCEKS");

        ServerSocketFactory ssf = SSLServerSocketFactory.getDefault();
        SSLServerSocket ss = (SSLServerSocket) ssf.createServerSocket(port);

        return ss;

    }

    /**
     * Metodo que prepara o servidor para correr verificando a sua integridade
     * 
     * @param passString password
     * @return true se preparou o servidor, false caso contrario
     */
    public static boolean prepareServer(String passString) {
        boolean created = Utils.createDir("server/serverFiles");
        if (!created) {
            System.out.println("Failed to setup server!");
            return false;
        }

        try {
            List<String> lines = Files.readAllLines(Paths.get("localInfo.txt"));

            if (lines.size() == 1) {

                try (BufferedWriter writer = new BufferedWriter(new FileWriter("localInfo.txt"))) {
                    Mac mac = Mac.getInstance("HmacSHA256");
                    SecretKeySpec secretKey = new SecretKeySpec(passString.getBytes(), "HmacSHA256");
                    mac.init(secretKey);
                    mac.update(lines.get(0).trim().getBytes());

                    byte[] hmac = mac.doFinal();
                    String hmacHex = Utils.bytesToHex(hmac);

                    writer.write(lines.get(0).trim() + "\n" + hmacHex);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

}
