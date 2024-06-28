mkdir -p bin
mkdir -p bin/utils
mkdir -p bin/server
mkdir -p bin/device

javac -d bin/utils ./utils/*.java
javac -d bin/server -cp bin/utils ./server/*.java
javac -d bin/device -cp bin/utils ./device/*.java

echo "Main-Class: IoTServer" > bin/serverManifest.txt
echo "Main-Class: IoTDevice" > bin/deviceManifest.txt

cd bin
jar cfm ../IoTServer.jar serverManifest.txt -C utils . -C server .
jar cfm ../IoTDevice.jar deviceManifest.txt -C device .
jar uf ../IoTDevice.jar -C utils Message.class
jar uf ../IoTDevice.jar -C utils MessageCode.class
jar uf ../IoTDevice.jar -C utils Utils.class

cd ..
rm -r bin