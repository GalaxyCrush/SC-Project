#!/bin/bash

rm -f server.jceks
rm -f martim.jceks
rm -f daniel.jceks
rm -f joao.jceks
rm -f truststore.jceks
rm -f martim.cer
rm -f daniel.cer
rm -f joao.cer
rm -f server.cer


read -p "Enter a password for all keystores: " password

echo "Creating server keystore..."
keytool -genkey -alias myserver -keyalg RSA -keysize 2048 -keystore server.jceks -storetype JCEKS -storepass "$password" -keypass "$password" -noprompt -dname "CN=myserver, OU=OrgUnit, O=Org, L=City, S=State, C=US"

echo "Creating clients keystore..."
keytool -genkey -alias fc58223@alunos.fc.ul.pt -keyalg RSA -keysize 2048 -keystore martim.jceks -storetype JCEKS -storepass "$password" -keypass "$password" -noprompt -dname "CN=myclient, OU=OrgUnit, O=Org, L=City, S=State, C=US"
keytool -genkey -alias fc58257@alunos.fc.ul.pt -keyalg RSA -keysize 2048 -keystore daniel.jceks -storetype JCEKS -storepass "$password" -keypass "$password" -noprompt -dname "CN=myclient, OU=OrgUnit, O=Org, L=City, S=State, C=US"
keytool -genkey -alias fc58189@alunos.fc.ul.pt -keyalg RSA -keysize 2048 -keystore joao.jceks -storetype JCEKS -storepass "$password" -keypass "$password" -noprompt -dname "CN=myclient, OU=OrgUnit, O=Org, L=City, S=State, C=US"

echo "Exporting certificates..."
keytool -export -alias myserver -file server.cer -keystore server.jceks -storetype JCEKS -storepass "$password"
keytool -export -alias fc58223@alunos.fc.ul.pt -file martim.cer -keystore martim.jceks -storetype JCEKS -storepass "$password"
keytool -export -alias fc58257@alunos.fc.ul.pt -file daniel.cer -keystore daniel.jceks -storetype JCEKS -storepass "$password"
keytool -export -alias fc58189@alunos.fc.ul.pt -file joao.cer -keystore joao.jceks -storetype JCEKS -storepass "$password"

echo "Creating truststore and importing certificates..."
keytool -import -alias myserver -file server.cer -keystore truststore.jceks -storetype JCEKS -storepass "$password" -noprompt
keytool -import -alias fc58223@alunos.fc.ul.pt -file martim.cer -keystore truststore.jceks -storetype JCEKS -storepass "$password" -noprompt
keytool -import -alias fc58257@alunos.fc.ul.pt -file daniel.cer -keystore truststore.jceks -storetype JCEKS -storepass "$password" -noprompt
keytool -import -alias fc58189@alunos.fc.ul.pt -file joao.cer -keystore truststore.jceks -storetype JCEKS -storepass "$password" -noprompt

echo "Done."
