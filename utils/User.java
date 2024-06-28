/**
 * Classe que representa um utilizador
 * 
 * @author Martim Pereira fc58223
 * @author João Pereira fc58189
 * @author Daniel Nunes fc58257
 */
public class User {

    private String user_id;
    private String password;

    public User(String user_id, String password) {
        this.user_id = user_id;
        this.password = password;
    }

    @Override
    /**
     * Metodo que redefine o método Hascode para a classe User
     */
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((user_id == null) ? 0 : user_id.hashCode());
        return result;
    }

    @Override
    /**
     * Metodo que redefine o método equals para a classe User
     */
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        User other = (User) obj;
        if (user_id == null) {
            if (other.user_id != null)
                return false;
        } else if (!user_id.equals(other.user_id))
            return false;
        return true;
    }

    /**
     * Metodo que devolve o id do utilizador
     * @return String com o nome do utilizador
     */
    public String getUserId() {
        return user_id;
    }

    public String getPassword() {
        return password;
    }

    @Override
    /**
     * Metodo que devolve o id do utilizador em string
     * @return O id do utilizador
     */
    public String toString() {
        return user_id;
    }

    
}
