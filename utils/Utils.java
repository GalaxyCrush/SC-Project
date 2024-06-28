import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Classe que contem metodos static auxiliares
 * 
 * @author Martim Pereira fc58223
 * @author João Pereira fc58189
 * @author Daniel Nunes fc58257
 * 
 */
public final class Utils {

    /**
     * Constructor vazio para impedir inicializacao
     */
    private Utils() {
    }
    
    /**
     * Metodo que vai preparar as pastas necessarias para o servidor funcionar 
     */
    public static void prepareServer() {
        boolean created = createDir("server/serverFiles");
        boolean created2 = createDir("server/serverImages");
        if (!created || !created2) {
            System.out.println("Failed to setup server!");
            System.exit(-1);
        }
    }
    
    /**
     * Método que cria um diretório com o path fornecido, caso este não exista.
     * 
     * @param path O path do diretório a ser criado
     * @return true se o diretório foi criado com sucesso, false caso contrário
     */
    public static boolean createDir(String path) {
        File serverFilesDir = new File(path);
        if (!serverFilesDir.exists()) {
            return serverFilesDir.mkdirs();
        }
        return true;

    }
    
    /**
     * Método que cria um ficheiro com o path fornecido e o conteúdo fornecido.
     * 
     * @param path O path do ficheiro a ser criado
     * @param contents O conteúdo do ficheiro a ser criado
     */
    private static void createFile(String path, String contents) {
        File f = new File(path);
        try {
            FileWriter fw = new FileWriter(f);
            fw.write(contents);
            fw.close();
        } catch (IOException e) {
            System.out.println("Failed to setup server!");
            e.printStackTrace();
        }

    }

    /**
     * Metodo qye vai fazer a verificacao do conteudo do ficheiro local_info no caso o nome do exec assim como o seu tamanho.
     * 
     * @param exec_name O nome do executavel
     * @param exec_size O tamanho do executavel
     * @return OK_TESTED se o executavel ja foi testado, NOK_TESTED se o executavel nao foi testado
     */
    public synchronized static MessageCode checkExec(String exec_name, String exec_size) {

        try (BufferedReader br = new BufferedReader(new FileReader("localInfo.txt"))) {
            String line;
            if ((line = br.readLine()) != null) {
                String[] parts = line.split(":");
                String local_name = parts[0].trim();
                String local_size = parts[1].trim();

                System.out.println(local_name.equals(exec_name));


                if (local_name.equals(exec_name) && local_size.equals(exec_size)) {
                    return MessageCode.OK_TESTED;
                } else {
                    return MessageCode.NOK_TESTED;
                }
            }

            return MessageCode.ERROR;

        } catch (IOException e) {
            e.printStackTrace();
        }
        return MessageCode.ERROR;
    }

    /**
     * Método que verifica se o username e a password fornecidos são válidos no ficheiro users.txt
     * 
     * @param targetPassword password a verificar
     * @param username nome de utilizador a verificar
     * @return OK_USER se o utilizador já existe,
     *         OK_NEW_USER se o utilizador não existe e foi criado,
     *         WRONG_PWD se a password está errada,
     *         ERROR se ocorrer um erro
     */
    public synchronized static MessageCode checkPassword(String targetPassword, String username) {
        File f = new File("server/serverFiles/users.txt");
        if (!f.exists()) {
            createFile("server/serverFiles/users.txt", username + ":" + targetPassword + "\n");
            return MessageCode.OK_NEW_USER;
        }
        try (BufferedReader br = new BufferedReader(new FileReader("server/serverFiles/users.txt"))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(":");
                String user_id = parts[0].trim();
                String password = parts[1].trim();
                if (user_id.equals(username)) {
                    System.out.println(password);
                    System.out.println(targetPassword);
                    if (password.equals(targetPassword)) {
                        return MessageCode.OK_USER;
                    } else {
                        return MessageCode.WRONG_PWD;
                    }
                }
            }
            FileWriter writer = new FileWriter("server/serverFiles/users.txt", true);
            String user_pass = username + ":" + targetPassword + "\n";
            writer.write(user_pass, 0, user_pass.length());
            writer.close();
            return MessageCode.OK_NEW_USER;
        } catch (IOException e) {
            System.err.println("Error in checkPassword: " + e.getMessage());
        }
        return MessageCode.ERROR;
    }

    /**
     * Método que lê o conteúdo de um ficheiro e devolve um array de bytes com esse conteúdo.
     * 
     * @param fileName ficheiro a ser lido
     * @return array de bytes com o conteúdo do ficheiro ou null caso ocorra um erro
     */
    public static synchronized byte[] getFileContents(String fileName) {

        try (FileInputStream fis = new FileInputStream(fileName)) {
            long fileSize = fis.available();
            byte[] data = new byte[(int) fileSize];
            fis.read(data);
            return data;

        } catch (IOException e) {
            System.err.println("Error in getFileContents: " + e.getMessage());
            return null;
        }
    }

    /**
     * Método que converte um HashMap num array de bytes e envia o HashMap pela socket.
     * 
     * @param hashmap HashMap a ser convertido
     * @return array de bytes com o HashMap
     */
    public static byte[] hashMapToByteArray(HashMap<String, Float> hashMap) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(hashMap);
            return bos.toByteArray();
        } catch (IOException e) {
            System.err.println("Error in hashMapToByteArray: " + e.getMessage());
            return new byte[0];
        }
    }
    
    /**
     * Método que escreve um array de bytes provenientes de um HashMap num ficheiro.
     * 
     * @param byteArray HashMap a escrever em formato de array de bytes
     * @param fileName nome do ficheiro onde escrever o HashMap
     */
    public synchronized static void writeByteArrayToFile(byte[] byteArray, String fileName) {
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(byteArray));
             Writer writer = new OutputStreamWriter(new FileOutputStream(fileName), StandardCharsets.UTF_8);
             BufferedWriter bufferedWriter = new BufferedWriter(writer)) {

            Object object = ois.readObject();
            if (object instanceof HashMap) {
                HashMap<?, ?> hashMap = (HashMap<?, ?>) object;
                for (Map.Entry<?, ?> entry : hashMap.entrySet()) {
                    if (entry.getKey() instanceof String && entry.getValue() instanceof Float) {
                        String key = (String) entry.getKey();
                        Float value = (Float) entry.getValue();
                        String s = key + " - " + value + "\n";
                        bufferedWriter.write(s, 0, s.length());
                    }
                }
            }
            else {
                System.out.println("Object is not a HashMap");
            }

            System.out.println("File written successfully: " + fileName);
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error in writeByteArrayToFile: " + e.getMessage());
        }
    }

}
