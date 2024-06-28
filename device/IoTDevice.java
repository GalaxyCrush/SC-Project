import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

/**
 * Classe main do dispositivo 
 * @author Martim Pereira fc58223
 * @author João Pereira fc58189
 * @author Daniel Nunes fc58257
 */
public class IoTDevice {

    private static Scanner sc;
    private static ObjectInputStream in = null;
    private static ObjectOutputStream out = null;

    private static boolean closed;

    private static Socket clientSocket;

    /**
     * Método main do dispositivo
     * @param args argumentos passados na linha de comandos
     */
    public static void main(String[] args) {

        closed = false;
        sc = new Scanner(System.in);

        if (args.length == 0 || args.length > 3) {
            System.out.println("Wrong amount of paramenters!");
            System.exit(-1);
        }

        String serverAddress = args[0];
        String id = args[1];
        String username = args[2];

        String[] addr = serverAddress.split(":");
        String ipHostname = addr[0];
        int port = (addr.length > 1) ? Integer.parseInt(addr[1]) : 12345;
        clientSocket = null;

        try {
            clientSocket = new Socket(ipHostname, port);

        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.exit(-1);
        }

        // Dá suporte ao fecho por Ctr+C
        prepareCtrC();
        System.out.println("Insira a sua password:");
        Message msg = new Message();
        try {

            in = new ObjectInputStream(clientSocket.getInputStream());
            out = new ObjectOutputStream(clientSocket.getOutputStream());

            String password;
            boolean authenticated = false;

            do {
                if (sc.hasNextLine()) {
                    password = sc.nextLine();
                    msg.setUser(username);
                    msg.setPassword(password);
                    out.writeObject(msg);

                    msg = (Message) in.readObject();

                    System.out.println("Server response: " + msg.getCode() + "\n");

                    // Verifica se o utilizador é novo ou não
                    switch (msg.getCode()) {
                        case WRONG_PWD:
                            System.out.println("Wrong password. Try again:");
                            break;
                        case OK_NEW_USER:
                            authenticated = true;
                            break;
                        case OK_USER:
                            authenticated = true;
                            break;
                        default:
                            System.out.println("Something went wrong...");
                            System.exit(-1);
                    }
                }

            } while (!authenticated);

            boolean id_check = false;

            do {

                msg.clear();
                msg.setDevId(id);
                out.writeObject(msg);

                msg = (Message) in.readObject();
                System.out.println("Server response: " + msg.getCode() + "\n");

                switch (msg.getCode()) {
                    case NOK_DEVID:
                        System.out.println("User is already associated with that dev-id.\nTry again:");
                        if (sc.hasNextLine()) {
                            id = sc.nextLine();
                            System.out.println("novo dev_id -> " + id);
                        }
                        break;
                    case OK_DEVID:
                        id_check = true;
                        break;
                    default:
                        System.out.println("Something went wrong...");
                        System.exit(-1);
                }

            } while (!id_check);

            // verificacao executavel

            msg.clear();
            Path jarPath = Paths.get(IoTDevice.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            msg.setSize(Files.size(jarPath));
            msg.setFileName(jarPath.getFileName().toString());

            out.writeObject(msg);

            msg = (Message) in.readObject();

            switch (msg.getCode()) {
                case NOK_TESTED:
                    System.out.println("Exec file check failed. Connection closed.");
                    System.exit(-1);
                    break;
                case OK_TESTED:
                    System.out.println("Exec file check passed!");
                    break;
                default:
                    System.out.println("Something went wrong...");
                    System.exit(-1);
                    break;
            }

            printMenu();
            System.out.println("Command: ");
            do {
                msg.clear();

                if (sc.hasNextLine()) {

                    String[] input = sc.nextLine().split(" ");
                    String command = input[0].toUpperCase();

                    switch (command) {
                        // CREATE <dm> - tenta criar um dominio <dm>
                        case "CREATE":

                            if (input.length != 2) {
                                System.out.println("Wrong format for command CREATE");
                                System.out.println("Right format -> CREATE");
                            } else {

                                msg.setCommand(command);
                                msg.setDomain(input[1]);
                                out.writeObject(msg);

                                msg = (Message) in.readObject();
                                System.out.println("Response: " + msg.getCode().getDescription());

                            }

                            break;
                        // ADD <user1> <dm> - tenta adicionar utilizador <user1> ao dominio <dm>
                        case "ADD":
                            if (input.length != 3) {
                                System.out.println("Wrong format for command ADD");
                                System.out.println("Right format -> ADD <user1> <dm>");
                            } else {
                                msg.setCommand(command);
                                ;
                                msg.setUser(input[1]);
                                msg.setDomain(input[2]);
                                out.writeObject(msg);

                                msg = (Message) in.readObject();
                                System.out.println("Response: " + msg.getCode().getDescription());

                            }
                            break;
                        // RD <dm> - tenta registar o dispositivo atual no domínio <dm>
                        case "RD":
                            if (input.length != 2) {
                                System.out.println("Wrong format for command RD");
                                System.out.println("Right format -> RD <dm>");
                            } else {
                                msg.setCommand(command);
                                msg.setDomain(input[1]);
                                out.writeObject(msg);

                                msg = (Message) in.readObject();
                                System.out.println("Response: " + msg.getCode().getDescription());

                            }
                            break;
                        // ET <float> - tenta registar uma temperatura <float> no dispositivo atual.
                        case "ET":
                            if (input.length != 2) {
                                System.out.println("Wrong format for command ET");
                                System.out.println("Right format -> ET <float>");
                            } else {
                                msg.setCommand(command);
                                msg.setTemp(input[1]);
                                out.writeObject(msg);

                                msg = (Message) in.readObject();
                                System.out.println("Response: " + msg.getCode().getDescription());

                            }

                            // EI <filename.jpg> - tenta registar Imagem com o path <filename.jpg> no dispositivo atual.
                        case "EI":
                            if (input.length != 2) {
                                System.out.println("Wrong format for command EI");
                                System.out.println("Right format -> EI <filename.jpg>");
                            } else {

                                msg.setCommand(command);
                                msg.setData(Utils.getFileContents(input[1]));
                                out.writeObject(msg);

                                msg = (Message) in.readObject();
                                System.out.println("Response: " + msg.getCode().getDescription());

                            }

                            break;
                        // RT <dm> - tenta obter as últimas medições de temperatura de cada dispositivo do domínio <dm> do servidor
                        case "RT":
                            if (input.length != 2) {
                                System.out.println("Wrong format for command RT");
                                System.out.println("Right format -> RT <dm>");
                            } else {

                                msg.setCommand(command);
                                msg.setDomain(input[1]);
                                out.writeObject(msg);

                                msg = (Message) in.readObject();

                                if (msg.getCode() == MessageCode.OK) {

                                    if (Utils.createDir("device/devicesData")) {
                                        Utils.writeByteArrayToFile(msg.getData(),
                                                "device/devicesData/" + input[1] + "_temp.txt");

                                        System.out.println(
                                                "Response: " + msg.getCode().getDescription() + ", " + msg.getSize()
                                                        + " (long)." +
                                                        "File was saved in /device/devicesData with the name "
                                                        + input[1] + "_temp.txt");
                                    }

                                } else if (msg.getCode() == MessageCode.NO_PERM) {
                                    System.out.println("Response: " + msg.getCode().getDescription() + " de leitura");
                                } else if (msg.getCode() == MessageCode.NO_DATA) {
                                    System.out.println("Response: " + msg.getCode().getDescription()
                                            + " # dominio não tem dados de temperatura");
                                } else {
                                    System.out.println("Response: " + msg.getCode().getDescription());
                                }

                            }
                            break;
                        // RI <user-id>:<dev_id> - tenta receber a última Imagem registada pelo dispositivo <userid>:<dev_id> no servidor.
                        case "RI":
                            if (input.length != 2) {
                                System.out.println("Wrong format for command RI");
                                System.out.println("Right format -> RI <user-id>:<dev_id>");
                            } else {

                                String[] parts = input[1].split(":");

                                if (parts.length == 1) {
                                    System.out.println("Wrong format for command RI");
                                    System.out.println("Right format -> RI <user-id>:<dev_id>");
                                    break;
                                }
                                if (parts[1] == "") {
                                    System.out.println("Wrong format for command RI");
                                    System.out.println("Right format -> RI <user-id>:<dev_id>");
                                    break;
                                }

                                msg.setCommand(command);
                                msg.setUser(parts[0]);
                                msg.setDevId(parts[1]);
                                out.writeObject(msg);

                                msg = (Message) in.readObject();

                                if (msg.getCode() == MessageCode.OK) {
                                    if (Utils.createDir("device/devicesData")) {
                                        String filename = input[1].replace(":", "_");
                                        String path = "device/devicesData/" + filename + ".jpg";
                                        File received = new File(path);
                                        FileOutputStream fos = new FileOutputStream(received);
                                        fos.write(msg.getData(), 0, Integer.parseInt(Long.toString(msg.getSize())));
                                        System.out.println(
                                                "Response: " + msg.getCode().getDescription() + ", " + msg.getSize()
                                                        + " (long)." +
                                                        "File was saved in /device/devicesData with the name "
                                                        + filename + ".jpg");
                                        fos.close();
                                    }
                                } else if (msg.getCode() == MessageCode.NO_PERM) {
                                    System.out.println("Response: " + msg.getCode().getDescription() + " de leitura");
                                } else if (msg.getCode() == MessageCode.NO_DATA) {
                                    System.out.println("Response: " + msg.getCode().getDescription()
                                            + " # esse device id não publicou dados");
                                } else {
                                    System.out.println("Response: " + msg.getCode().getDescription());
                                }

                            }
                            break;
                        // HELP - imprime o menu
                        case "HELP":
                            printMenu();
                            break;
                        // quanlquer outro comando que não esteja presente no menu não tem qualquer efeito
                        default:
                            System.out.println("Invalid command. Try again!");
                            break;

                    }

                    System.out.println("Command: ");

                }

            } while (!closed);

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getMessage());
            System.exit(-1);
        }

        System.out.println("All good");

    }

    /**
     * Método que imprime o menu
     */
    private static void printMenu() {
        System.out.println("Menu:");
        System.out.println("- CREATE <dm> -> cria dominio");
        System.out.println("- ADD <user1> <dm> -> Adicionar utilizador <user1> ao domínio <dm>");
        System.out.println("- RD <dm> -> Registar o Dispositivo atual no domínio <dm>");
        System.out.println("- ET <float> -> Enviar valor <float> de Temperatura para o servidor.");
        System.out.println("- EI <filename.jpg> -> Enviar Imagem <filename.jpg> para o servidor.");
        System.out.println(
                "- RT <dm> -> Receber as últimas medições de Temperatura de cada dispositivo do domínio <dm>, desde que o utilizador tenha permissões.");
        System.out.println(
                "- RI <user-id>:<dev_id> # Receber o ficheiro Imagem do dispositivo <userid>:<dev_id> do servidor, desde que o utilizador tenha permissões.");
    }

    /**
     * Método que encarregue por detetar o fecho do cliente por Ctr+C
     */
    private static void prepareCtrC() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                Message msg = new Message();
                msg.setCommand("EXIT");
                out.writeObject(msg);
                closed = true;
                in.readObject();
            } catch (ClassNotFoundException | IOException e) {
                System.out.println("Client closed!");
            }

        }));
    }

        
}
