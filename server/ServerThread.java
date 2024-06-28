
import java.net.Socket;

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

    private ServerThreadHandler handler;

    /**
     * Construtor da classe ServerThread
     * 
     * @param inSoc Socket do cliente
     * @param info  Informação partilhada entre os threads
     */
    public ServerThread(Socket inSoc, SharedInfoSingleton info) {
        this.cliSocket = inSoc;
        this.handler = new ServerThreadHandler(cliSocket, info);
        System.out.println("Thread active...");
    }

    /**
     * Método que corre a thread com o ciclo de comandos
     */
    public void run() {
        handler.processAuthentication();
        startCommandCycle();      
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
                Message msg = handler.readMessage();

                switch (msg.getCommand()) {
                    case "CREATE" :
                        msg = handler.createDomain(msg.getDomain());
                        handler.writeMessage(msg);
                        break;
                    case "ADD":
                        msg = handler.addUserToDomain(msg.getUser(), msg.getDomain());
                        handler.writeMessage(msg);
                        break;
                    case "RD":
                        msg = handler.registerDevice(msg.getDomain());
                        handler.writeMessage(msg);
                        break;
                    case "ET":
                        msg = handler.registerTemperature(msg.getTemp());
                        handler.writeMessage(msg);
                        break;
                    case "EI":
                        msg = handler.registerImage(msg.getData());
                        handler.writeMessage(msg);
                        break;
                    case "RT":
                        msg = handler.retriveDomainTemperatures(msg.getDomain());
                        handler.writeMessage(msg);
                        break;
                    case "RI":
                        msg = handler.retriveImage(msg.getUser() + ":" + msg.getDevId());
                        handler.writeMessage(msg);
                        break;
                    case "EXIT":
                        shutdown();
                        return;
                    default:
                        System.err.println("Comando inválido");
                        break;
                }
            } catch (Exception e) {
                System.err.println("Erro ao processar comando " + e.getMessage());
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