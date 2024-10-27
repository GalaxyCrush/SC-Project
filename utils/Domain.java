
import java.io.Serializable;
import java.util.HashMap;
import java.util.Set;

/**
 * Classe que representa um dominio
 * 
 * @author Martim Pereira fc58223
 * @author João Pereira fc58189
 * @author Daniel Nunes fc58257
 */
public class Domain implements Serializable {

    private final String name;
    private User owner;
    private HashMap<User, byte[]> users;
    private HashMap<String, DeviceData> devices;

    /**
     * Construtor de um dominio
     * 
     * @param name  ome do dominio
     * @param owner User que é dono/criador do dominio
     */
    public Domain(String name, User owner) {
        this.name = name;
        this.owner = owner;
        this.users = new HashMap<>();
        this.devices = new HashMap<>();
    }

    /**
     * Metodo que retorna a chave de um utilizador
     * 
     * @param username Nome do utilizador
     * @return Chave do utilizador
     */
    public byte[] getKeyByUserId(String username) {
        for (User u : users.keySet()) {
            if (u.getUserId().equals(username)) {
                return users.get(u);
            }
        }
        return null;
    }

    /**
     * Metodo que retorna a lista de dispositivos
     * 
     * @return Lista de dispositivos
     */
    public HashMap<String, DeviceData> getDevices() {
        return devices;
    }

    /**
     * Metodo que retorna o dono do dominio
     * 
     * @return Dono do dominio
     */
    public User getOwner() {
        return owner;
    }

    /**
     * Metodo que retorna a lista de utilizadores
     * 
     * @return Lista de utilizadores
     */
    public HashMap<User, byte[]> getUsers() {
        return users;
    }

    /**
     * @param user User que vai ser adicionado ao dominio
     * @ensures this.users.contains(user)
     */
    public void addUser(User user, byte[] domainKey) {
        this.users.put(user, domainKey);
    }

    /**
     * Metodo que adiciona um dispositivo ao dominio
     * 
     * @param Device O device a ser adicionado
     */
    public void registerDevice(String device) {
        this.devices.put(device, new DeviceData());
    }

    /**
     * Metodo que regista uma temperatura num dispositivo
     * 
     * @param device Nome do dispositivo
     * @param temp   Temperatura a ser registada
     * @param params Parametros da temperatura
     */
    public void registerTempToDevice(String device, byte[] temp, byte[] params) {
        this.devices.get(device).setTemp(temp);
        this.devices.get(device).setTempParams(params);
    }

    /**
     * Metodo que regista uma imagem num dispositivo
     * 
     * @param device Nome do dispositivo
     * @param image  Imagem a ser registada
     * @param params Parametros da imagem
     */
    public void registerImageToDevice(String device, byte[] image, byte[] params) {
        this.devices.get(device).setImage(image);
        this.devices.get(device).setImageParams(params);
    }

    /**
     * Metodo que retorna o nome do dominio
     * 
     * @return Nome do dominio
     */
    public String getName() {
        return name;
    }

    /**
     * Metodo que verifica se um user é dono do dominio
     * 
     * @param user User a ser verificado
     */
    public boolean isOwner(User user) {
        return this.owner.getUserId().equals(user.getUserId());
    }

    /**
     * Metodo que verifica se um user é membro do dominio
     * 
     * @param String user a ser verificado
     * @return True se o user é membro do dominio e False caso contrário
     */
    public boolean hasUser(String user) {
        Set<User> users = this.users.keySet();
        for (User u : users) {
            if (u.getUserId().equals(user)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Metodo que verifica se um dispositivo está na lista de dispositivos
     * 
     * @param String name nome do dispositivo a ser verificado
     * @return True se o dispositivo está na lista e False caso contrário
     */
    public boolean hasDevice(String name) {
        Set<String> devices = this.devices.keySet();
        for (String d : devices) {
            if (d.equals(name)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return name + ";" + owner + ";" + devices.keySet().toString() + ";" + users.keySet().toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((owner == null) ? 0 : owner.hashCode());
        result = prime * result + ((users == null) ? 0 : users.hashCode());
        result = prime * result + ((devices == null) ? 0 : devices.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Domain other = (Domain) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (owner == null) {
            if (other.owner != null)
                return false;
        } else if (!owner.equals(other.owner))
            return false;
        if (users == null) {
            if (other.users != null)
                return false;
        } else if (!users.equals(other.users))
            return false;
        if (devices == null) {
            if (other.devices != null)
                return false;
        } else if (!devices.equals(other.devices))
            return false;
        return true;
    }

}
