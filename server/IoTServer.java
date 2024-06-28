import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
     * @param args argumentos da linha de comando 
     */
    public static void main(String[] args) {

        
        List<ServerThread> activeThreads = Collections.synchronizedList(new ArrayList<>());
        
        SharedInfoSingleton info = SharedInfoSingleton.getInstance();
        scheduler.scheduleAtFixedRate(info::backupInfo, 10, 30, TimeUnit.SECONDS);
        
        Utils.prepareServer();

        int port;

        if (args.length > 1) {
            System.out.println("Wrong amount of paramenters!");
            System.exit(-1);
        }

        port = args.length == 0 ? 12345 : Integer.parseInt(args[0]);


        try {
            ServerSocket serverSocket = new ServerSocket(port);
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

        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.exit(-1);

        }

    }

}
