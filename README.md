# Projeto de Segurança e Confiabilidade F2

## Descrição

Este projeto tem como objetivo a realização de um sistema distribuido de uma rede de sensores IoT. Há duas classes:

- `IoTDevice` que representa um dispositivo de sensorização, que tem como objetivo mandar dados sensoriais para o servidor.
- `IoTServer` que representa  servidor onde os dados são guardados.

Existe o conceito de Dominio que funciona como um conjunto fechado de Devices. Os utilizadores podem criar devices que podem ou não estar num dominio. Um dominio é feito por um User que denominamos de Owner, tendo este o poder de adicionar pessoas ao Dominio. Cada User tem permissoes de leitura relativamente aos dados de dispositivos presentes nos dominios aos quais tem acesso (tenha sido adicionado pelo Owner) e permissões de escrita para os dispositivos aos quais está conectado.
Nesta segunda fase adicionámos uma camada de segurança ao trabalho. O servidor agora não tem acesso aos dados e vai haver uma comunicação end to end entre cliente e servidor, onde o servidor só tem acesso a dados encriptados. Existe uma truststore na qual é guardado todos os certificados dos users do serviço, truststore esta presente para todos os users. Os clientes têm uma keystore com o seu par de chaves privado e certificado, e estes users necessitam de se autenticar previamente no servidor para poder usufruir do serviço. Autenticação que é feita a partir de 2FA a partir de um código que é mandado por mail e o check de criptografia assimétrica. Os dados são encriptados com uma chave de dominio feita a partir da chave do user com a password do dominio.

## Compilação

Foi feito um script para a compilação do trabalho que dá origem a dois jar, um para o IoTDevice outro para o IoTServer.

Este script vai:

1. Criar a pasta bin se não existir
2. Compilar os respetivos .class do projeto
3. Definir a main respetiva a cada um dos jar
4. Criar os .jar

Para correr o script basta fazer:

```bash
./compile.bat #(Windows)
```

```bash
./compile.sh #(Linux)
```

## Uso

1. Inicie o servidor com

```bash
java -jar IoTServer.jar <port> <password-cifra> <keystore> <password-keystore> <2FA-APIKey>
```

onde:

- **Port** que irá aceitar ligações de clientes (o parametro **port** é opcional sendo o porto default usado o 12345);
- **Password-cifra** e uma password que vai ser usaga para gerar a chave smétrca que cfra os ficheiros
- **Keystore** O caminho da keystore que contém o par de chaves do servidor.
- **Password-keystore** é a password da keystore.
- **2FA-APKey** é a chave dada para realziar a 2FA por mail.

1. Inicie o cliente com

```bash
java -jar IoTDevice.jar <serverAddress> <truststore> <keystore> <password/keystore> <dev-id> <user-id>
```

onde:

- serverAddress corresponde ao `<IP/hostname>[:Port]`(IP/hostname é obrigatório enquanto o porto pode ser omitido sendo usado por default o 12345);
- **Truststore** é o caminho para a truststore que contém os certificados de chave pública do servidor e dos utilizadores.
- **Keystore** é o caminho para a keystore que contém o par de chaves do `<user-id>`.
- **Password-keystore** é a password da keystore.
- o `<dev-id>` corresponde ao id do Device a inicializar;
- o `<user-id>` ao nome do User a inicializar.
_**ATENCÃO**_: - de modo a poder fazer a verificação do executável é necessário que exista um ficheiro localInfo.txt cujo conteúdo corresponda ao seguinte:
  - `PathDoIoTDevice` na primeira linha do ficheiro

## Notas

- Foi usado java 17 para realizar o trabalho
- Não encontrámos nenhuma limitação face ao enunciado

## Realizado por

- João Pereira fc58189
- Martim Pereira fc58223
- Daniel Nunes fc58257
