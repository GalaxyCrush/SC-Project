import java.io.Serializable;
import java.security.cert.Certificate;
import java.util.List;

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
    private String dev_id;

    private String c2fa;
    private Long nonce;

    private Certificate cert;
    private byte[] signature;

    private List<String> domains;
    
    private List<byte[]> dataList;
    private List<byte[]> params;

    private byte[] param;

    private byte[] domainKey;


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

        this.c2fa = null;
        this.nonce = null;

        this.cert = null;
        this.signature = null;

        this.command = null;

        this.domains = null;
        this.dataList = null;

        this.domainKey = null;

        this.params = null;

        this.param = null;
        
    }

    // ------------------------- Getters ------------------------- //
    public MessageCode getCode() {
        return this.code;
    }

    public List<byte[]> getParams() {
        return this.params;
    }

    public byte[] getParam() {
        return this.param;
    }

    public List<String> getDomains() {
        return this.domains;
    }

    public Certificate getCertificate() {
        return this.cert;
    }

    public byte[] getSignature() {
        return this.signature;
    }

    public Long getNonce() {
        return this.nonce;
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

    public String getDomain() {
        return this.domain;
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

    public String getC2FA() {
        return this.c2fa;
    }

    public List<byte[]> getDataList() {
        return this.dataList;
    }


    public byte[] getDomainKey() {
        return this.domainKey;
    }

    // ------------------------- Setters ------------------------- //
    public void setCode(MessageCode code) {
        this.code = code;
    }

    public void setParams(List<byte[]> params) {
        this.params = params;
    }

    public void setParam(byte[] param) {
        this.param = param;

    }

    public void setDataList(List<byte[]> dataList) {
        this.dataList = dataList;
    }

    public void setDomains(List<String> domains) {
        this.domains = domains;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public void setCertificate(Certificate cert) {
        this.cert = cert;
    }

    public void setSignature(byte[] signature) {
        this.signature = signature;
    }

    public void setNonce(Long nonce) {
        this.nonce = nonce;
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

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setData(byte[] data) {
        this.data = data;

    }

    public void setCommand(String command) {
        this.command = command;
    }

    public void setC2FA(String c2fa) {
        this.c2fa = c2fa;
    }

    public void setDomainKey(byte[] domainKey) {
        this.domainKey = domainKey;
    }

    public String toString() {
        return "Message [code=" + code + ", command=" + command + ", data=" + data + ", fileName=" + fileName + ", size="
                + size + ", domain=" + domain + ", user=" + user + ", dev_id=" + dev_id
                + ", c2fa=" + c2fa + ", nonce=" + nonce + ", cert=" + cert + ", signature="
                + signature + ", domains=" + domains + ", domainKey=" + domainKey + "]";
    }
}
