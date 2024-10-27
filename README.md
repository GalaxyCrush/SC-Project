# Safety and Reliability Project

## Description

This project aims to realise a distributed system for an IoT sensor network. There are two classes:

- `IoTDevice` which represents a sensing device, whose purpose is to send sensed data to the server.
- `IoTServer` which represents the server where the data is stored.

There is the concept of a Domain which functions as a closed set of Devices. Users can create devices that may or may not be in a domain. A domain is created by a User who we call the Owner, who has the power to add people to the Domain. Each User has read permissions for the data of devices in the domains to which they have access (having been added by the Owner) and write permissions for the devices to which they are connected.
In this second phase we've added a layer of security to the work. The server now has no access to the data and there will be end-to-end communication between client and server, where the server only has access to encrypted data. There is a truststore in which all the service users' certificates are stored, a truststore that is present for all users. Clients have a keystore with their private key pair and certificate, and these users need to authenticate themselves on the server before they can use the service. Authentication is done via 2FA using a code that is sent by email and the asymmetric encryption check. The data is encrypted with a domain key made from the user's key and the domain password.

## Compilation

A script has been made to compile the work that gives rise to two jars, one for the IoTDevice and the other for the IoTServer.
This script will:

1. Create the bin folder if it doesn't exist
2. Compile the respective .class of the project
3. Define the respective main for each jar
4. Create the .jar
  
To run the script just do:

```bash
./compile.bat # (Windows)
```

```bash
./compile.sh # (Linux)
```

## Usage

1. start the server with

```bash
java -jar IoTServer.jar <port> <password-cipher> <keystore> <password-keystore> <2FA-APIKey>
```

where:

- **Port** which will accept connections from clients (the **port** parameter is optional and the default port used is 12345);
- **Password-cipher** and a password that will be used to generate the synthetic key that encrypts the files.
- **Keystore** The path to the keystore containing the server's key pair.
- **Password-keystore** is the keystore password.
- **2FA-APKey** is the key given to realise 2FA by mail.

1. start the client with

```bash
java -jar IoTDevice.jar <serverAddress> <truststore> <keystore> <password/keystore> <dev-id> <user-id>
```

where:

- serverAddress corresponds to `<IP/hostname>[:Port]` (IP/hostname is mandatory while the port can be omitted with 12345 being used by default);
- Truststore** is the path to the truststore containing the server's and users' public key certificates.
- **Keystore** is the path to the keystore containing the `<user-id>` key pair.
- **Password-keystore** is the password for the keystore.
- the `<dev-id>` corresponds to the id of the Device to be initialised;
- `<user-id>` is the name of the User to be initialised.
_**ATTENTION**_: - In order to check the executable, there must be a localInfo.txt file whose contents correspond to the following:
- `PathDoIoTDevice` in the first line of the file

## Notes

- We used java 17 to carry out the work
- We didn't find any limitations to the statement
- Final grade 20/20

## Realised by

- João Pereira fc58189
- Martim Pereira fc58223
- Daniel Nunes fc58257
