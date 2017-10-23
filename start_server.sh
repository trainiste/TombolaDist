java -Djava.rmi.server.hostname=$1 \
	-Djava.security.policy=file:./security.policy \
	-Djava.rmi.server.codebase=file:./bin -cp ./bin:./json-simple-1.1.jar  centralizedServer/CentralizedServer 
