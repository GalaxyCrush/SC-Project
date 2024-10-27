import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.cert.Certificate;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

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
    private String device = null;

    private ServerAuthenticationHandler authHandler;

    /**
     * Construtor de um ServerThreadHandler
     */
    public ServerThreadHandler(SharedInfoSingleton info, ObjectOutputStream out, ObjectInputStream in) {
        this.info = info;
        try {
            this.out = out;
            this.in = in;
            this.authHandler = new ServerAuthenticationHandler(info, in, out);
        } catch (Exception e) {
            System.err.println("Error crating IO streams");
        }
    }

    /**
     * Método que processa a autenticação de um cliente através de um
     * AuthenticationHandler
     * - Autenticação do utilizador e password
     * - Autenticação do device id
     * - Autenticação do executável
     * 
     * @return true se a autenticação foi bem sucedida
     */
    protected boolean startAuthentication() {
        AbstractMap.SimpleEntry<User, String> pair = authHandler.processAuthentication();

        if (pair == null) {
            return false;
        }

        this.user = pair.getKey();
        this.device = pair.getValue();
        return true;

    }

    /* ----------------------------- Funcionalidades ---------------------------- */

    /**
     * Método encarregue pela criação de um dominio
     * 
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
     * @param userid     nome do utilizador a adicionar
     * @param domainName nome do dominio ao qual o user será adicionado
     * @return Message com o resultado da operação
     *         - OK se o utilizador foi adicionado com sucesso
     *         - NO_DM se o dominio não existir
     *         - NO_USER se o utilizador não existir
     *         - NO_PERM se o utilizador não tiver permissões (não é o owner do
     *         dominio)
     */
    protected Message addUserToDomain(String userid, String domainName, byte[] domainKey) {
        Domain domain = info.getDomain(domainName);
        Message msg = new Message();
        if (domain == null) {
            msg.setCode(MessageCode.NO_DM);
        } else {
            User user = info.getUserByName(userid);

            if (user == null) {
                msg.setCode(MessageCode.NO_USER);
            } else if (!domain.isOwner(this.user)) {
                msg.setCode(MessageCode.NO_PERM);
            } else {
                domain.addUser(user, domainKey);
                msg.setCode(MessageCode.OK);
            }
        }
        return msg;
    }

    /**
     * Método encarrege pelo retorno de um certificado pertencente a um User
     * 
     * @param user nome do utilizador a procurar
     * @return Message com o resultado da operação
     */
    public Message retrieveCertificate(String user) {
        Message msg = new Message();

        User target = info.getUserByName(user);

        if (target == null) {
            msg.setCode(MessageCode.NO_USER);
            return msg;
        }

        Certificate cert = target.getCertificate();
        if (cert != null) {
            msg.setCertificate(cert);
            msg.setCode(MessageCode.OK);
        } else {
            msg.setCode(MessageCode.NO_USER);
        }

        return msg;
    }

    /**
     * Método encarregue pela adição de um dispositivo a um dominio
     * 
     * @param devId      nome do dispositivo a registar no dominio
     * @param domainName nome do dominio onde o dispositivo será adicionado
     * @return Message com o resultado da operação
     *         - OK se o dispositivo foi adicionado com sucesso
     *         - NO_DM se o dominio não existir
     *         - NO_PERM se o utilizador não tiver permissões (não é o owner do
     *         dominio)
     */
    protected Message registerDevice(String domainName) {
        Domain domain = info.getDomain(domainName);
        Message msg = new Message();
        if (domain == null) {
            msg.setCode(MessageCode.NO_DM);
        } else if (!domain.hasUser(this.user.getUserId())) {
            msg.setCode(MessageCode.NO_PERM);
        } else {
            domain.registerDevice(device);
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
    protected Message registerTemperature(List<String> domains, List<byte[]> temps, List<byte[]> params) {
        Message msg = new Message();

        for (int i = 0; i < domains.size(); i++) {
            Domain d = info.getDomain(domains.get(i));
            byte[] t = temps.get(i);
            byte[] p = params.get(i);
            d.registerTempToDevice(this.device, t, p);
        }

        msg.setCode(MessageCode.OK);
        return msg;
    }

    /**
     * Método que retorna a chave de um dominio de um utilizador
     * 
     * @param domains  lista de dominios a procurar
     * @param username nome do utilizador que queremos a key
     * @return Message com o resultado da operação
     */
    public Message retrieveDomainKey(List<String> domains, String username) {
        Message msg = new Message();

        List<byte[]> keys = new ArrayList<>();

        for (String domain : domains) {

            Domain d = info.getDomain(domain);
            byte[] domainKey = d.getKeyByUserId(username);
            keys.add(domainKey);

        }

        msg.setDataList(keys);
        return msg;

    }

    /**
     * Método encarregue pelo registo de uma imagem num dispositivo
     * 
     * @param data byte array com o contéudo da imagem a registar
     * @return Message com o resultado da operação
     *         - OK se a imagem foi registada com sucesso
     *         - NOK se a imagem não for válida (data == null) ou se ocorrer um erro
     *         ao escrever a imagem
     */
    protected Message registerImage(List<String> domains, List<byte[]> images, List<byte[]> params) {
        Message msg = new Message();

        for (int i = 0; i < domains.size(); i++) {
            Domain d = info.getDomain(domains.get(i));
            byte[] img = images.get(i);
            byte[] p = params.get(i);
            d.registerImageToDevice(this.device, img, p);
        }

        msg.setCode(MessageCode.OK);
        return msg;
    }

    /**
     * Método encarregue por retornar as temperaturas do dispositivos de um dominio
     * 
     * @param domainName nome do dominio a procurar
     * @return Message com o resultado da operação
     *         - OK se as temperaturas foram retornadas com sucesso
     *         - NO_DM se o dominio não existir
     *         - NO_PERM se o utilizador não tiver permissões (não é o owner do
     *         dominio)
     *         - NO_ID se o dispositivo não existir
     */
    protected Message retriveDomainTemperatures(String domainName) {
        Domain d = info.getDomain(domainName);
        Message msg = new Message();
        if (d != null) {
            if (d.hasUser(this.user.getUserId())) {
                HashMap<String, byte[]> temps = new HashMap<>();
                List<byte[]> params = new ArrayList<>();
                for (Entry<String, DeviceData> entry : d.getDevices().entrySet()) {
                    String device = entry.getKey();
                    DeviceData data = entry.getValue();
                    if (data.getTemp() != null) {
                        temps.put(device, data.getTemp());
                        params.add(data.getTempParams());
                    }
                }
                if (temps.size() == 0) {
                    msg.setCode(MessageCode.NO_DATA);
                } else {
                    byte[] data = Utils.hashMapToByteArray(temps);

                    // ja foi guardada cifrada pela user pubKey
                    byte[] domainKey = d.getKeyByUserId(this.user.getUserId());

                    msg.setDomainKey(domainKey);
                    msg.setData(data);
                    msg.setParams(params);
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
     *         - NO_PERM se o utilizador não tiver permissões (não é o owner do
     *         dominio)
     *         - NO_DATA se a imagem não existir
     */
    protected Message retriveImage(String user_devId) {

        Message msg = new Message();

        if (!this.info.hasDevice(user_devId)) {
            msg.setCode(MessageCode.NO_ID);
        } else {
            for (Domain domain : info.getDomains()) {
                if (domain.hasDevice(user_devId) && domain.hasUser(this.user.getUserId())) {
                    DeviceData data = domain.getDevices().get(user_devId);
                    if (data.getImage() != null) {
                        msg.setData(data.getImage());
                        msg.setParam(data.getImageParams());
                        msg.setDomainKey(domain.getKeyByUserId(this.user.getUserId()));
                        msg.setCode(MessageCode.OK);
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
     * Método que retorna os dominios em que um user se encontra
     * 
     * @param username nome do utilizador
     * @param devId    nome do dispositivo
     * @return Message com o resultado da operação
     */
    protected Message retrieveUserDomains(String username, String devId) {

        Message msg = new Message();
        List<String> domains = new ArrayList<>();
        List<byte[]> keys = new ArrayList<>();
        for (Domain domain : info.getDomains()) {
            if (domain.hasDevice(username + ":" + devId)) {
                domains.add(domain.getName());
                keys.add(domain.getKeyByUserId(username));
            }
        }

        if (domains.size() == 0) {
            msg.setCode(MessageCode.NO_DATA);
        } else {
            msg.setDomains(domains);
            msg.setDataList(keys);
            msg.setCode(MessageCode.OK);
        }
        return msg;
    }

    /**
     * Método encarregue por fechar a conexão com o cliente
     */
    protected void close() {
        this.info.removeDevice(this.device);
        Message msg = new Message();
        Utils.writeMessage(msg, this.out);
        try {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
        } catch (Exception e) {
            System.exit(-1);
        }
    }
}