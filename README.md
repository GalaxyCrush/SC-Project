# Projeto de Segurança e Confiabilidade F1

## Descrição

Este projeto tem como objetivo a realização de um sistema distribuido de uma rede de sensores IoT. Há duas classes:
- `IoTDevice` que representa um dispositivo de sensorização, que tem como objetivo mandar dados sensoriais para o servidor.
- `IoTServer` que representa  servidor onde os dados são guardados.

Existe o conceito de Dominio que funciona como um conjunto fechado de Devices. Os utilizadores podem criar devices que podem ou não estar num dominio. Um dominio é feito por um User que denominamos de Owner, tendo este o poder de adicionar pessoas ao Dominio. Cada User tem permissoes de leitura relativamente aos dados de dispositivos presentes nos dominios aos quais tem acesso (tenha sido adicionado pelo Owner) e permissoes de escrita para os dispositivos aos quais está conectado. 


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

 
_<u>**ATENCÃO**</u>_: - dependendo do script de compilação usado para gerar os .jar o tamanho varia, ou seja, um .jar gerado pelo compile.bat não terá o mesmo tamanho que um gerado pelo compile.sh

## Uso

1. Inicie o servidor com
```bash
java -jar IoTServer.jar <port>
```
onde:
- o porto que ira aceitar ligações de clientes (o parametro <port> é opcional sendo o porto default usado o 12345);

2. Inicie o cliente com
```bash
java -jar IoTDevice.jar <serverAddress> <dev-id> <user-id>
```
onde:
- serverAddress corresponde ao `<IP/hostname>[:Port]`(IP/hostname é obrigatório enquanto o porto pode ser omitido sendo usado por default o 12345);
- o `<dev-id>` corresponde ao id do Device a inicializar;
- o `<user-id>` ao nome do User a inicializar.

_<u>**ATENCÃO**</u>_: - de modo a poder fazer a verificação do tamanho e nome do executável é necessário que exista um ficheiro localInfo.txt cujo conteúdo corresponda ao seguinte:
 - `IoTDevice.jar:<tamanhoExecutavel>` (onde o tamanho do executável está em bytes) na primeira linha do ficheiro

## Notas

- Foi usado java 17 para realizar o trabalho
- Não encontrámos nenhuma limitação face ao enunciado

## Realizado por

#### Grupo SegC-004:
- João Pereira fc58189
- Martim Pereira fc58223
- Daniel Nunes fc58257