import java.io.Serializable;
import java.util.Arrays;

/**
 * Classe que representa os dados de um dispositivo
 * 
 * @author Martim Pereira fc58223
 * @author João Pereira fc58189
 * @author Daniel Nunes fc58257
 */
public final class DeviceData implements Serializable {

    private byte[] temp;
    private byte[] paramsTemp;
    private byte[] image;
    private byte[] paramsImage;

    /**
     * Construtor de um objeto DeviceData vazio
     */
    public DeviceData() {
        this.temp = null;
        this.image = null;
    }

    public DeviceData(byte[] temp, byte[] image) {
        this.temp = temp;
        this.image = image;
    }

    /**
     * Metodo que retorna a temperatura do dispositivo em bytes
     * 
     * @return Temperatura do dispositivo
     */
    public byte[] getTemp() {
        return temp;
    }

    /**
     * Metodo que retorna os parametros da temperatura do dispositivo em bytes
     * 
     * @return parametros da temperatura do dispositivo
     */
    public byte[] getTempParams() {
        return paramsTemp;
    }

    /**
     * Método que guarda a temperatura do dispositivo em bytes
     * 
     * @param temp Temperatura do dispositivo
     */
    public void setTemp(byte[] temp) {
        this.temp = temp;
    }

    /**
     * Método que guarda os parametros da temperatura do dispositivo em bytes
     * 
     * @param paramsTemp parametros da temperatura do dispositivo
     */
    public void setTempParams(byte[] paramsTemp) {
        this.paramsTemp = paramsTemp;
    }

    /**
     * Método que retorna a imagem do dispositivo
     * 
     * @return Imagem do dispositivo
     */
    public byte[] getImage() {
        return image;
    }

    /**
     * Metodo que retorna os parametros da imagem do dispositivo em bytes
     * 
     * @return parametros da imagem do dispositivo
     */
    public byte[] getImageParams() {
        return paramsImage;
    }

    /**
     * Método que guarda a imagem do dispositivo em bytes
     * 
     * @param image Imagem do dispositivo
     */
    public void setImage(byte[] image) {
        this.image = image;
    }

    /**
     * Método que guarda os parametros da imagem do dispositivo em bytes
     * 
     * @param paramsImage parametros da imagem do dispositivo
     */
    public void setImageParams(byte[] paramsImage) {
        this.paramsImage = paramsImage;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(temp);
        result = prime * result + Arrays.hashCode(image);
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
        DeviceData other = (DeviceData) obj;
        if (!Arrays.equals(temp, other.temp))
            return false;
        if (!Arrays.equals(image, other.image))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "[temp=" + Arrays.toString(temp) + ", image=" + Arrays.toString(image) + "]";
    }

}
