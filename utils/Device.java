
public class Device {

    private final String name;
    private Float lastTemp;
    private boolean isOn;

    /**
     * Construtor para o objeto Device
     * 
     * @param name nome do dispositivo (user_id:dev_id)
     * @param isOn boolean que representa se o dispositivo está ativo
     */
    public Device(String name, boolean isOn) {
        this.name = name;
        this.lastTemp = null;
        this.isOn = isOn;
    }
    /**
     * Constructor para o objeto Device
     * 
     * @param name nome do dispositivo (user_id:dev_id)
     * @param isOn boolean que representa se o dispositivo está ativo
     * @param lastTemp float que representa a última temperatura registada
     */
    public Device(String name, boolean isOn, Float lastTemp) {
        this.name = name;
        this.lastTemp = lastTemp;
        this.isOn = isOn;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((lastTemp == null) ? 0 : lastTemp.hashCode());
        result = prime * result + (isOn ? 1231 : 1237);
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
        Device other = (Device) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (lastTemp == null) {
            if (other.lastTemp != null)
                return false;
        } else if (!lastTemp.equals(other.lastTemp))
            return false;
        if (isOn != other.isOn)
            return false;
        return true;
    }

    /**
     * Método que devolve o nome do dispositivo
     * 
     * @return String com o nome do dispositivo
     */
    public String getDevName() {
        return this.name;
    }

    /**
     * Método que devolve a última temperatura registada
     * 
     * @return Float com a última temperatura registada
     */
    public Float getTemp() {
        return this.lastTemp;
    }

    /**
     * Método que define a última temperatura registada
     * 
     * @param temp Float com a temperatura registada mais recentemente
     */
    public void setTemp(Float temp) {
        this.lastTemp = temp;
    }

    /**
     * Método que liga o dispositivo
     * 
     * @ensures this.isOn == true
     */
    public void turnOn() {
        this.isOn = true;
    }

    /**
     * Método que desliga o dispositivo
     * 
     * @ensures this.isOn == false
     
     */
    public void turnOff() {
        this.isOn = false;
    }

    /**
     * Método que verifica se o dispositivo está ligado
     * 
     * @return boolean que representa se o dispositivo está ligado
     */    
    public boolean isOn() {
        return this.isOn;
    }

    @Override
    public String toString() {
        return name;
    }
}
