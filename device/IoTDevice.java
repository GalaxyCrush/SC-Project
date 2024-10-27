import java.io.FileInputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.SocketException;
import java.security.KeyStore;
import java.util.Scanner;
import javax.net.SocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 * Classe main do dispositivo
 * 
 * @author Martim Pereira fc58223
 * @author João Pereira fc58189
 * @author Daniel Nunes fc58257
 */
public class IoTDevice {

    private static Scanner sc;

    private static boolean active;

    private static SSLSocket clientSocket;

    private static DeviceCommandHandler handler;
    private static DeviceAuthenticationHandler authHandler;

    /**
     * Método main do dispositivo
     * 
     * @param args argumentos passados na linha de comandos
     */
    public static void main(String[] args) {

        active = true;
        sc = new Scanner(System.in);

        if (args.length != 6) {
            System.out.println("Wrong amount of parameters!");
            System.exit(-1);
        }

        String serverAddress = args[0];
        String truststoreFile = args[1];
        String keystoreFile = args[2];
        String keystorePassword = args[3];
        String id = args[4];
        String username = args[5];

        String[] addr = serverAddress.split(":");
        String ipHostname = addr[0];
        int port = (addr.length > 1) ? Integer.parseInt(addr[1]) : 12345;
        clientSocket = null;

        try {
            beginConnection(keystorePassword, keystoreFile, truststoreFile, ipHostname, port, username, id);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
            System.exit(-1);
        }

        // Dá suporte ao fecho por Ctr+C
        prepareCtrC();

        try {
            if (!authHandler.startTwoFactorAuthentication(username)) {
                handler.closeClient();
                clientSocket.close();
                System.exit(-1);
            }

            if (!authHandler.startExecutableAuthentication(id)) {
                handler.closeClient();
                clientSocket.close();
                System.exit(-1);
            }

            handler.printMenu();

            System.out.println("Command: ");
            do {

                if (sc.hasNextLine()) {

                    String[] input = sc.nextLine().split(" ");
                    String command = input[0].toUpperCase();

                    switch (command) {
                        // CREATE <dm> - tenta criar um dominio <dm>
                        case "CREATE":
                            if (input.length != 2) {
                                System.out.println("Wrong format for command CREATE");
                                System.out.println("Right format -> CREATE <dm>");
                            } else {
                                handler.createDomainRequest(input[1]);
                            }
                            break;
                        // ADD <user1> <dm> - tenta adicionar utilizador <user1> ao dominio <dm>
                        case "ADD":
                            if (input.length != 4) {
                                System.out.println("Wrong format for command ADD");
                                System.out.println("Right format -> ADD <user1> <dm> <password-dominio>");
                            } else {
                                handler.addUserToDomainRequest(input[1], input[2], input[3]);
                            }
                            break;
                        // RD <dm> - tenta registar o dispositivo atual no domínio <dm>
                        case "RD":
                            if (input.length != 2) {
                                System.out.println("Wrong format for command RD");
                                System.out.println("Right format -> RD <dm>");
                            } else {
                                handler.registerDeviceRequest(input[1]);

                            }
                            break;
                        // ET <float> - tenta registar uma temperatura <float> no dispositivo atual.
                        case "ET":
                            if (input.length != 2) {
                                System.out.println("Wrong format for command ET");
                                System.out.println("Right format -> ET <float>");
                            } else {
                                try {
                                    float temp = Float.parseFloat(input[1]);
                                    handler.registerTemperatureRequest(temp);
                                } catch (NumberFormatException e) {
                                    System.out.println("Invalid temperature!");
                                }
                            }

                            break;
                        // EI <filename.jpg> - tenta registar Imagem com o path <filename.jpg> no
                        // dispositivo atual.
                        case "EI":
                            if (input.length != 2) {
                                System.out.println("Wrong format for command EI");
                                System.out.println("Right format -> EI <filename.jpg>");
                            } else {
                                handler.registerImageRequest(input[1]);
                            }

                            break;
                        // RT <dm> - tenta obter as últimas medições de temperatura de cada dispositivo
                        // do domínio <dm> do servidor
                        case "RT":
                            if (input.length != 2) {
                                System.out.println("Wrong format for command RT");
                                System.out.println("Right format -> RT <dm>");
                            } else {
                                handler.retriveTemperatureRequest(input[1]);
                            }
                            break;
                        // RI <user-id>:<dev_id> - tenta receber a última Imagem registada pelo
                        // dispositivo <userid>:<dev_id> no servidor.
                        case "RI":
                            if (input.length != 2) {
                                System.out.println("Wrong format for command RI");
                                System.out.println("Right format -> RI <user-id>:<dev_id>");
                            } else {
                                handler.retriveImageRequest(input[1]);
                            }
                            break;
                        // MYDOMAINS - tenta obter a lista de domínios em que o dispositivo atual está
                        case "MYDOMAINS":
                            if (input.length != 1) {
                                System.out.println("Wrong format for command MYDOMAINS");
                                System.out.println("Right format -> MYDOMAINS");
                            } else {
                                handler.myDomainsRequest();
                            }
                            break;
                        // HELP - imprime o menu
                        case "HELP":
                            handler.printMenu();
                            break;
                        // quanlquer outro comando que não esteja presente no menu não tem qualquer
                        // efeito
                        default:
                            System.out.println("Invalid command. Try again!");
                            break;

                    }
                    System.out.println("Command: ");
                }
            } while (active);

        } catch (SocketException e) {
            System.out.println("Server closed connection");
            System.exit(-1);
        } catch (Exception e) {
            System.err.println("Exception in IoTDevice main");
            System.exit(-1);
        }

    }

    /**
     * Método que encarregue por detetar o fecho do cliente por Ctr+C
     */
    private static void prepareCtrC() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            handler.closeClient();
            active = false;
        }));
    }

    /**
     * Método que inicia a conexão com o servidor
     * 
     * @param keystorePassword password do keystore
     * @param keystoreFile     ficheiro keystore
     * @param truststoreFile   ficheiro truststore
     * @param ipHostname       ip do servidor
     * @param port             porta do servidor
     * @param username         username do user
     * @param id               id do dispositivo
     * @throws Exception
     */
    private static void beginConnection(String keystorePassword, String keystoreFile, String truststoreFile,
            String ipHostname, int port, String username, String id) throws Exception {
        System.setProperty("javax.net.ssl.keyStore", keystoreFile);
        System.setProperty("javax.net.ssl.keyStorePassword", keystorePassword);
        System.setProperty("javax.net.ssl.keyStoreType", "JCEKS");

        System.setProperty("javax.net.ssl.trustStore", truststoreFile);
        System.setProperty("javax.net.ssl.trustStorePassword", keystorePassword);
        System.setProperty("javax.net.ssl.trustStoreType", "JCEKS");

        SocketFactory sf = SSLSocketFactory.getDefault();
        clientSocket = (SSLSocket) sf.createSocket(ipHostname, port);
        ObjectInputStream ois = new ObjectInputStream(clientSocket.getInputStream());
        ObjectOutputStream oos = new ObjectOutputStream(clientSocket.getOutputStream());

        KeyStore keystore = KeyStore.getInstance("JCEKS");
        try (InputStream keystoreStream = new FileInputStream(keystoreFile)) {
            keystore.load(keystoreStream, keystorePassword.toCharArray());
        }

        KeyStore truststore = KeyStore.getInstance("JCEKS");
        try (InputStream keystoreStream = new FileInputStream(truststoreFile)) {
            truststore.load(keystoreStream, keystorePassword.toCharArray());
        }

        handler = new DeviceCommandHandler(ois, oos, username, id, keystore, truststore, keystorePassword);
        authHandler = new DeviceAuthenticationHandler(ois, oos, keystore, keystorePassword);
    }
}
