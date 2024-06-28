import java.io.*;
import java.util.ArrayList;

/**
 * Classe que representa um Singleton que contem a informacao partilhada entre as diferentes threads 
 * 
 * @author Martim Pereira fc58223
 * @author João Pereira fc58189
 * @author Daniel Nunes fc58257
 */
public class SharedInfoSingleton {

    private final String DOMAINS_PATH = "server/serverFiles/domains.txt";
    private final String DEVICES_PATH = "server/serverFiles/devices.txt";
    private final String USERS_PATH = "server/serverFiles/users.txt";

    private static volatile SharedInfoSingleton domains = null;

    private ArrayList<Domain> domainsList;
    private ArrayList<Device> devicesList;
    private ArrayList<User> usersList;

    /**
     * Construtor privado para impedir inicializacao
     */
    private SharedInfoSingleton() {
        domainsList = new ArrayList<>();
        devicesList = new ArrayList<>();
        usersList = new ArrayList<>();
    }

    /**
     * Metodo que retorna a instancia do singleton
     * Caso ainda não exista uma instancia, cria-a
     * @return Instancia do singleton
     */
    public synchronized static SharedInfoSingleton getInstance() {
        if (domains == null) {
            synchronized (SharedInfoSingleton.class) {
                if (domains == null) {
                    domains = new SharedInfoSingleton();
                    domains.loadInfo();
                }
            }
        }
        return domains;
    }

    /**
     * Metodo que carrega a informacao dos ficheiros
     */
    public void loadInfo() {
        loadUsers();
        loadDomain();
        loadDevices();
    }

    private synchronized void loadUsers() {
        File file = new File(USERS_PATH);
        if (!file.exists()) {
            return;
        }
        try {
            BufferedReader bf = new BufferedReader(new FileReader(USERS_PATH));
            String line;
            while ((line = bf.readLine()) != null) {

                String[] parts = line.split(":");
                String user_id = parts[0].trim();
                String password = parts[1].trim();

                User u = new User(user_id, password);
                usersList.add(u);
            }
            bf.close();
        } catch (IOException e) {
            System.err.println("Error loading domains from file");
        }
        
    }

    /**
     * Metodo que faz o load dos dominios a partir das informações 
     * presentes no ficheiro dos dominios de modo a garantir persistência
     */
    private synchronized void loadDomain() {
        File file = new File(DOMAINS_PATH);
        if (!file.exists()) {
            return;
        }
        try {
            BufferedReader bf = new BufferedReader(new FileReader(DOMAINS_PATH));
            String line;
            while ((line = bf.readLine()) != null) {

                String[] parts = line.split(";");
                String name = parts[0].trim();
                String ownerName = parts[1].trim();
                String[] devices = parts[2].substring(1, parts[2].length() - 1).split(", ");
                String[] users = parts[3].substring(1, parts[3].length() - 1).split(", ");

                User owner = getUserByName(ownerName);

                Domain domain = new Domain(name, owner);
                for (String device : devices) {
                    if (device != "") {
                        Device d = new Device(device, false);
                        domain.registerDevice(d);
                        devicesList.add(d);
                    }
                }
                for (String user : users) {
                    if (user != "") {
                        User u = getUserByName(user);
                        domain.addUser(u);
                    }
                }
                domainsList.add(domain);
            }
            bf.close();
        } catch (IOException e) {
            System.err.println("Error loading domains from file");
        }
    }
    
    /**
     * Metodo que faz o load dos dispositivos a partir das informações 
     * presentes no ficheiro dos dispositivos  de modo a garantir persistência
     */
    private synchronized void loadDevices() {

        File file = new File(DEVICES_PATH);
        if (!file.exists()) {
            return;
        }
        try {
            BufferedReader bf = new BufferedReader(new FileReader(file));
            String line;
            while ((line = bf.readLine()) != null) {

                String[] parts = line.split("_");
                String devName = parts[0].trim();
                Float temp = parts[1].equals("null") ? null : Float.parseFloat(parts[1]);
                Device d = getDeviceByName(devName);
                if (d != null) {
                    d.setTemp(temp);
                } else {
                    devicesList.add(new Device(devName, false, temp));
                }

            }
            bf.close();
        } catch (IOException e) {
            System.err.println("Error loading devices from file");
        }
    }
    
    /**
     * Metodo que faz o backup da informacao antes de desligar o server de modo a garantir persistência
     */
    public void backupInfo() {
        backupUsers();
        backupDomains();
        backupDeviceInfo();

    }

    private synchronized void backupUsers() {

        File file = new File("server/serverFiles");
        if (!file.exists()) {
            Utils.createDir("server/serverFiles");
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(USERS_PATH))) {;
            for (User u : usersList) {
                String s = u.getUserId() + ":" + u.getPassword() + "\n";
                writer.write(s, 0, s.length());
            }
        } catch (IOException e) {
            System.err.println("Error creating backup of domains");
        }

    }
    
    /**
     * Metodo que faz o backup dos dominios para o ficheiro de dominios de modo a garantir persistência
     */
    private synchronized void backupDomains() {

        File file = new File("server/serverFiles");
        if (!file.exists()) {
            Utils.createDir("server/serverFiles");
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(DOMAINS_PATH))) {
            for (Domain d : domainsList) {
                String s = d.toString() + "\n";
                writer.write(s, 0, s.length());
            }
        } catch (IOException e) {
            System.err.println("Error creating backup of domains");
        }

    }

    /**
     * Metodo que faz o backup dos dispositivos para o ficheiro de dispositivos de modo a assegurar persistência
     */
    private synchronized void backupDeviceInfo() {

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(DEVICES_PATH));
            for (Device d : devicesList) {
                String s = d.toString() + "_" + (d.getTemp() == null ? "null" : Float.toString(d.getTemp())) + "\n";
                writer.write(s, 0, s.length());
            }
            writer.close();
        } catch (IOException e) {
            System.err.println("Error creating backup of devices");
        }

    }

    /**
     * Metodo que retorna a lista de dominios
     * @return Lista de dominios
     */
    public synchronized ArrayList<Domain> getDomains() {
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
    public synchronized boolean addDevice(Device device) {
        return devicesList.add(device);
    }

    /**
     * Metodo que retorna um dispositivo pelo seu nome
     * @param devName nome do dispositivo a procurar
     * @return Dispositivo com o nome procurado ou null caso nao exista
     */
    public synchronized Device getDeviceByName(String devName) {
        for (Device actDevice : devicesList) {
            if (actDevice.getDevName().equals(devName)) {
                return actDevice;
            }
        }
        return null;
    }

    public User getUserByName(String userid) {
        for (User user : usersList) {
            if (user.getUserId().equals(userid)) {
                return user;
            }
        }
        return null;
    }

    public synchronized boolean addUser(User user) {
        return usersList.add(user);
    }
}