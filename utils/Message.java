import java.io.Serializable;

/**
 * Classe que representa uma mensagem
 * 
 * @author Martim Pereira fc58223
 * @author João Pereira fc58189
 * @author Daniel Nunes fc58257
 */
public class Message implements Serializable{

    private MessageCode code;
    private String command;
    
    private byte[] data;
    private String fileName;
    private Long size;
    
    private String domain;
    private String user;
    private String password;
    private String dev_id;

    private String temp;

    /**
     * Construtor de uma mensagem vazia
     */
    public Message() {
    }
    
    /**
     * Construtor de uma mensagem apenas com um código
     * @param code código da mensagem
     */
    public Message(MessageCode code) {
        setCode(code);
    }

    /**
     * Método que limpa o contéudo de uma mensagem
     * 
     * @ensures this.code == null && this.data == null && this.fileName == null && this.size == null
     *       && this.domain == null && this.user == null && this.dev_id == null && this.temp == null
     */
    public void clear() {
        this.code = null;
    
        this.data = null;
        this.fileName = null;
        this.size = null;
        
        this.domain = null;
        this.user = null;
        this.dev_id = null;
        this.temp = null;
        
    }

    // ------------------------- Getters ------------------------- //
    public MessageCode getCode() {
        return this.code;
    }
    
    public Long getSize() {
        return this.size;
    }

    public String getDevId() {
        return this.dev_id;
    }

    public String getUser() {
        return this.user;
    }

    public String getPassword() {
        return this.password;
    }

    public String getDomain() {
        return this.domain;
    }

    public String getTemp() {
        return this.temp;
    }

    public String getFileName() {
        return this.fileName;
    }

    public byte[] getData() {
        return this.data;
    }
    
    public String getCommand() {
        return this.command;
    }

    // ------------------------- Setters ------------------------- //
    public void setCode(MessageCode code) {
        this.code = code;
    }
    public void setSize(Long size) {
        this.size = size;
    }

    public void setDevId(String dev_id) {
        this.dev_id = dev_id;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public void setTemp(String temp) {
        this.temp = temp;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setData(byte[] data) {
        this.data = data;

    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setCommand(String command) {
        this.command = command;
    }
    
    
}
