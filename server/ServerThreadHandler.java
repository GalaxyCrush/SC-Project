import java.io.*;
import java.net.Socket;
import java.util.HashMap;

/**
 * Classe responsável pelo comportamento das ServerThreads 
 * 
 * @author Martim Pereira fc58223
 * @author João Pereira fc58189
 * @author Daniel Nunes fc58257
 */
public class ServerThreadHandler {

    private SharedInfoSingleton info;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    private User user = null;
    private Device device = null;
    
    /**
     * Construtor de um ServerThreadHandler
     */
    public ServerThreadHandler(Socket sock, SharedInfoSingleton info) {
        this.info = info;
        try {
            this.out = new ObjectOutputStream(sock.getOutputStream());
            this.in = new ObjectInputStream(sock.getInputStream());
        }
        catch (Exception e) {
            System.err.println("Error crating IO streams");
        }
    }
    
    /**
     * Método que processa a autenticação de um cliente
     *  - Autenticação do utilizador e password
     *  - Autenticação do device id
     *  - Autenticação do executável
     */
    protected boolean processAuthentication() {
        try {
            userAuthentication();
            devIdAuthentication();
            execAuthentication();
            return true;
        } catch (Exception e) {
            System.err.println("Error processing authentication: " + e.getMessage());
        }
        return false;
    }

    /**
     *  Método que autentica o utilizador e a password
     * 
     * @throws IOException
     * @throws ClassNotFoundException
     */
    protected void userAuthentication() throws IOException, ClassNotFoundException {
        Message msg;
        do {
            msg = (Message) in.readObject();

            this.user = info.getUserByName(msg.getUser());

            if (this.user == null) {
                // nao havia user registado com user:password
                this.user = new User(msg.getUser(), msg.getPassword());
                msg.setCode(MessageCode.OK_NEW_USER);
                info.addUser(this.user);
            } else {
                // user já existia
                if (this.user.getPassword().equals(msg.getPassword())) {
                    msg.setCode(MessageCode.OK_USER);
                } else {
                    msg.setCode(MessageCode.WRONG_PWD);
                }
            }
            this.out.writeObject(msg);

        } while (msg.getCode() == MessageCode.WRONG_PWD);
        this.user = new User(msg.getUser(), msg.getPassword());
    }

    /**
     * Método que autentica o device id
     * 
     * @throws IOException
     * @throws ClassNotFoundException
     */
    protected void devIdAuthentication() throws IOException, ClassNotFoundException {
        Message msg;
        do {
            msg = (Message) in.readObject();
            String user_DevId = this.user.getUserId() + ":" + msg.getDevId();
            this.device = info.getDeviceByName(user_DevId);
            if (this.device != null) {
                if (this.device.isOn()) {
                    msg.setCode(MessageCode.NOK_DEVID);
                } else {
                    msg.setCode(MessageCode.OK_DEVID);
                    this.device.turnOn();
                }
            } else {
                this.device = new Device(user_DevId, true);
                info.addDevice(this.device);
                msg.setCode(MessageCode.OK_DEVID);
            }
            this.out.writeObject(msg);

        } while (msg.getCode() == MessageCode.NOK_DEVID);
        System.out.println("Device " + this.device.getDevName() + " created!");
    }

    /**
     * Método que autentica o executável
     * 
     * @throws IOException
     * @throws ClassNotFoundException
     */
    protected void execAuthentication() throws IOException, ClassNotFoundException {

        Message msg = (Message) in.readObject();

        String size = String.valueOf(msg.getSize());
        String exec = msg.getFileName();

        msg.setCode(Utils.checkExec(exec, size));

        out.writeObject(msg);
    }
    
    // ---------------------------------------


    /**
     * Método que recebe uma mensagem
     * 
     * @return Message enviada pela socket
     */
    protected Message readMessage() {
        try {
            return ((Message) this.in.readObject());
        } catch (ClassNotFoundException | IOException e) {
            System.err.println("Error reading Message");
        }
        return null;
    }

    /**
     * Método que envia uma mensagem
     * 
     * @param msg Message a enviar
     */
    protected void writeMessage(Message msg) {
        try {
            this.out.writeObject(msg);
        } catch (IOException e) {
            System.err.println("Error writing Message");
        }
    }

    
    /**
     * Método encarregue pela criação de um dominio
     * @param domainName nomde do dominio a criar
     * @return Message com o resultado da operação
     *         - OK se o dominio foi criado com sucesso
     *         - NOK se o dominio já existir
     */    
    protected Message createDomain(String domainName) {
        Domain domain = info.getDomain(domainName);
        Message msg = new Message();
        if (domain != null) {
            msg.setCode(MessageCode.NOK);
        } else {
            Domain newDomain = new Domain(domainName, this.user);
            info.addDomain(newDomain);
            msg.setCode(MessageCode.OK);
        }
        return msg;
    }

    /**
     * Método encarregue pela adição de um utilizador a um dominio
     * 
     * @param userid nome do utilizador a adicionar
     * @param domainName nome do dominio ao qual o user será adicionado
     * @return Message com o resultado da operação
     *         - OK se o utilizador foi adicionado com sucesso
     *         - NO_DM se o dominio não existir
     *         - NO_USER se o utilizador não existir
     *         - NO_PERM se o utilizador não tiver permissões (não é o owner do dominio)
     */
    protected Message addUserToDomain(String userid, String domainName) {
        Domain domain = info.getDomain(domainName);
        Message msg = new Message();
        if (domain == null) {
            msg.setCode(MessageCode.NO_DM);
        } else {
            User user = null;
            synchronized (this) {
                File file = new File("server/serverFiles/users.txt");
                try {
                    BufferedReader bf = new BufferedReader(new FileReader(file));
                    String line;
                    while ((line = bf.readLine()) != null) {
                        String[] parts = line.split(":");
                        String name = parts[0].trim();
                        if (name.equals(userid)) {
                            user = info.getUserByName(userid);
                            break;
                        }
                    }
                    bf.close();
                } catch (IOException e) {
                    System.err.println("Error reading from users.txt");
                }
            }
            if (user == null) {
                msg.setCode(MessageCode.NO_USER);
            } else if (!domain.isOwner(this.user)) {
                msg.setCode(MessageCode.NO_PERM);
            } else {
                domain.addUser(user);
                msg.setCode(MessageCode.OK);
            }
        }
        return msg;
    }

    /**
     * Método encarregue pela adição de um dispositivo a um dominio
     * 
     * @param devId nome do dispositivo a registar no dominio
     * @param domainName nome do dominio onde o dispositivo será adicionado
     * @return Message com o resultado da operação
     *         - OK se o dispositivo foi adicionado com sucesso
     *         - NO_DM se o dominio não existir
     *         - NO_PERM se o utilizador não tiver permissões (não é o owner do dominio)
     */
    protected Message registerDevice(String domainName) {
        Domain domain = info.getDomain(domainName);
        Message msg = new Message();
        if (domain == null) {
            msg.setCode(MessageCode.NO_DM);
        } else if (!domain.hasUser(this.user.getUserId())) {
            msg.setCode(MessageCode.NO_PERM);
        } else {
            domain.getDevices().add(this.device);
            msg.setCode(MessageCode.OK);
        }
        return msg;
    }

    /**
     * Método encarregue pelo registo de uma temperatura num dispositivo
     * 
     * @param temp temperatura a registar
     * @return Message com o resultado da operação
     *         - OK se a temperatura foi registada com sucesso
     *         - NOK se a temperatura não for válida (string não é um número)
     */    
    protected Message registerTemperature(String temp) {
        Message msg = new Message();
        try {
            Float t = Float.parseFloat(temp);
            this.device.setTemp(t);
            msg.setCode(MessageCode.OK);
                        
        } catch (NumberFormatException e) {
            msg.setCode(MessageCode.NOK);
        }
        return msg;
    }

    /**
     * Método encarregue pelo registo de uma imagem num dispositivo
     * 
     * @param data byte array com o contéudo da imagem a registar
     * @return Message com o resultado da operação
     *         - OK se a imagem foi registada com sucesso
     *         - NOK se a imagem não for válida (data == null) ou se ocorrer um erro ao escrever a imagem
     */
    protected Message registerImage(byte[] data) {
        boolean dir = Utils.createDir("server/serverImages");
        if (!dir) {
            Message msg = new Message();
            msg.setCode(MessageCode.NOK);
            return msg;
        }
        
        Message msg = new Message();
        if (data == null) {
            msg.setCode(MessageCode.NOK);
        } else {
            try {
                synchronized (this) {
                    String name = device.getDevName().replace(':', '_');
                    File f = new File("server/serverImages/" + name + ".jpg");
                    FileOutputStream fos = new FileOutputStream(f);
                    fos.write(data, 0, data.length);
                    fos.close();
                }
                msg.setCode(MessageCode.OK);
            } catch (IOException e) {
                msg.setCode(MessageCode.NOK);
            }
        }
        return msg;
    }

    /**
     * Método encarregue por retornar as temperaturas do dspositivos de um dominio
     * 
     * @param domainName nome do dominio a procurar
     * @return Message com o resultado da operação
     *         - OK se as temperaturas foram retornadas com sucesso
     *         - NO_DM se o dominio não existir
     *         - NO_PERM se o utilizador não tiver permissões (não é o owner do dominio)
     *         - NO_ID se o dispositivo não existir
     */
    protected Message retriveDomainTemperatures(String domainName) {
        Domain d = info.getDomain(domainName);
        Message msg = new Message();
        if (d != null) {
            if (d.hasUser(this.user.getUserId())) {
                HashMap<String,Float> temps = new HashMap<>();
                for (Device device : d.getDevices()) {
                    if (device.getTemp() != null) {
                        temps.put(device.getDevName(), device.getTemp());
                    }  
                }
                if (temps.size() == 0) {
                    msg.setCode(MessageCode.NO_DATA);                    
                } else {
                    byte[] data = Utils.hashMapToByteArray(temps);
                    msg.setData(data);
                    msg.setSize(Long.valueOf(data.length));
                    msg.setCode(MessageCode.OK);
                }
            } else {
                msg.setCode(MessageCode.NO_PERM);
            }
        } else {
            msg.setCode(MessageCode.NO_DM);
        }
        return msg;
    }

    /**
     * Método encarregue por retornar a imagem de um dispositivo
     * 
     * @param user_devId nome do dispositivo a procurar
     * @return Message com o resultado da operação
     *         - OK se a imagem foi retornada com sucesso
     *         - NO_ID se o dispositivo não existir
     *         - NO_PERM se o utilizador não tiver permissões (não é o owner do dominio)
     *         - NO_DATA se a imagem não existir
     */    
    protected Message retriveImage(String user_devId) {

        Message msg = new Message();

        if (this.info.getDeviceByName(user_devId) == null) {
            msg.setCode(MessageCode.NO_ID);                      
        } else {
            for (Domain domain : info.getDomains()) {
                if (domain.hasDevice(user_devId) && domain.hasUser(this.user.getUserId())) {
                    String targetName = user_devId.replace(":", "_");
                    File f = new File("server/serverImages/" + targetName + ".jpg");
                    if (f.exists()) {
                        msg.setCode(MessageCode.OK);                        
                        byte[] data = Utils.getFileContents("server/serverImages/" + targetName + ".jpg");
                        msg.setData(data);
                        msg.setSize(Long.valueOf(data.length));
                    } else {
                        msg.setCode(MessageCode.NO_DATA);
                    }
                    return msg;
                }
            }
            msg.setCode(MessageCode.NO_PERM); 
        }

        return msg;
        
    }

    /**
     * Método encarregue por fechar a conexão com o cliente
     */
    protected void close() {
        this.device.turnOff();
        Message msg = new Message();
        writeMessage(msg);
        try {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
        } catch (Exception e) {
            System.err.println("Error closing IO streams");
        }
    }

}