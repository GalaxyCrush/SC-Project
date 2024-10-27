import java.net.Socket;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Classe que representa um thread do servidor. Cada thread é responsável por
 * tratar um cliente.
 * 
 * @author Martim Pereira fc58223
 * @author João Pereira fc58189
 * @author Daniel Nunes fc58257
 */
public class ServerThread extends Thread {

    private volatile boolean shutdown = false;

    private Socket cliSocket = null;
    private ObjectInputStream in = null;
    private ObjectOutputStream out = null;

    private ServerThreadHandler handler;

    /**
     * Construtor da classe ServerThread
     * 
     * @param inSoc Socket do cliente
     * @param info  Informação partilhada entre os threads
     */
    public ServerThread(Socket inSoc, SharedInfoSingleton info) {
        this.cliSocket = inSoc;
        try {
            this.out = new ObjectOutputStream(inSoc.getOutputStream());
            this.in = new ObjectInputStream(inSoc.getInputStream());
            this.handler = new ServerThreadHandler(info, this.out, this.in);
            System.out.println("Thread active...");
        } catch (Exception e) {
            System.err.println("Error creating input/output streams");
            System.exit(-1);
        }
    }

    /**
     * Método que corre a thread com o ciclo de comandos
     */
    public void run() {

        if (handler.startAuthentication()) {
            startCommandCycle();
        }
        handler.close();
        try {

            if (cliSocket != null) {
                cliSocket.close();
            }
        } catch (Exception e) {
            System.err.println("Erro ao fechar o socket cliente");
            System.exit(-1);
        }
    }

    // ----------------------- Funcoes de Comando -------------------------//
    /**
     * Método que corre o ciclo de comandos
     */
    public void startCommandCycle() {
        while (!shutdown) {
            try {
                Message msg = Utils.readMessage(this.in);
                System.out.println("----------------------------------");
                System.out.println("Command: " + msg.getCommand());
                switch (msg.getCommand()) {
                    case "CREATE":
                        msg = handler.createDomain(msg.getDomain());
                        Utils.writeMessage(msg, this.out);
                        break;
                    case "ADD":
                        msg = handler.addUserToDomain(msg.getUser(), msg.getDomain(), msg.getData());
                        Utils.writeMessage(msg, this.out);
                        break;
                    case "RD":
                        msg = handler.registerDevice(msg.getDomain());
                        Utils.writeMessage(msg, this.out);
                        break;
                    case "ET":
                        msg = handler.registerTemperature(msg.getDomains(), msg.getDataList(), msg.getParams());
                        Utils.writeMessage(msg, this.out);
                        break;
                    case "EI":
                        msg = handler.registerImage(msg.getDomains(), msg.getDataList(), msg.getParams());
                        Utils.writeMessage(msg, this.out);
                        break;
                    case "RT":
                        msg = handler.retriveDomainTemperatures(msg.getDomain());
                        Utils.writeMessage(msg, this.out);
                        break;
                    case "RI":
                        msg = handler.retriveImage(msg.getUser() + ":" + msg.getDevId());
                        Utils.writeMessage(msg, this.out);
                        break;
                    case "MYDOMAINS":
                        msg = handler.retrieveUserDomains(msg.getUser(), msg.getDevId());
                        Utils.writeMessage(msg, this.out);
                        break;
                    case "GET_USER_KEYS":
                        msg = handler.retrieveDomainKey(msg.getDomains(), msg.getUser());
                        Utils.writeMessage(msg, this.out);
                        break;
                    case "GET_CERTIFICATE":
                        msg = handler.retrieveCertificate(msg.getUser());
                        Utils.writeMessage(msg, this.out);
                        break;
                    case "EXIT":
                        shutdown();
                        return;
                    default:
                        System.err.println("Comando inválido");
                        break;
                }
            } catch (Exception e) {
                System.err.println("Erro ao processar comando \n" + e.getMessage());
                shutdown();
            }
        }

    }

    /**
     * Método que sinaliza que a thread deve ser terminada
     */
    public void shutdown() {
        shutdown = true;
    }
}