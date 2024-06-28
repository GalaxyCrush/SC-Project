
/**
 * Enumerado com os códigos de mensagens possíveis.
 * 
 * @author Martim Pereira fc58223
 * @author João Pereira fc58189
 * @author Daniel Nunes fc58257
 */
public enum MessageCode {
    WRONG_PWD("Wrong Password"),
    OK_NEW_USER("User Created"),
    OK_USER("User accepted"),
    NOK_DEVID("Invalid dev-id"),
    OK_DEVID("Valid dev-id"),
    NOK_TESTED("NOK_TESTED"),
    OK_TESTED("OK_TESTED"),
    OK("OK"),
    NOK("NOK"),
    NO_PERM("NO_PERM # sem permissões"),
    NO_DM("NO_DM # esse dominio não existe"),
    NO_USER("NO_USER # esse user não existe"),
    NO_DATA("NO_DATA"),
    NO_ID("NOID # esse device id não existe"),
    ERROR("Error");

    private final String description;
    
    /**
     * Método que cria um novo código de mensagem com uma descrição
     * 
     * @param description descrição do código de mensagem
     */
    MessageCode(String description) {
        this.description = description;
    }

    /**
     * Metodo que retorna a descrição do código de mensagem
     * @return A descrição da mensagem
     */
    public String getDescription() {
        return description;
    }
}
