package rmiClient;
import java.net.MalformedURLException;
import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import msg.Msg;
import utils.Utils;
import utils.Utils.Types;
public class RMIClient {
	String centralizedServerUrl;
	String serverUrl;
	String centralizedServerHostname;
	String serverHostname;
	centralizedServer.InitInterface init;
	Utils utils;
	//Registry registry;
	public RMIClient(String centralizedServerHostname, String centralizedServerUrl, String serverUrl) throws RemoteException {
		this.utils = new Utils();
		this.centralizedServerHostname = centralizedServerHostname;
		this.centralizedServerUrl = centralizedServerUrl;
		this.serverUrl = serverUrl; //last server used
		this.serverHostname = centralizedServerHostname; //last Hostname used
	}
	public int initPlayer(String centralizedServerUrl) {
		try {
			System.setSecurityManager(new RMISecurityManager());
			System.setProperty("sun.rmi.transport.tcp.responseTimeout", utils.rmiClientTimeout);
			Registry registry = LocateRegistry.getRegistry(centralizedServerHostname, utils.defaultCentralizedServerPort);
			init = (centralizedServer.InitInterface)registry.lookup(centralizedServerUrl);
			int pid = init.initPlayer();
			System.out.println("Pid is :" + pid);
			return pid;
		} catch (Exception e) {
			System.out.println("initPlayer exception: " + e);
			return -1;
		}
	}
	public void sendRingMsg(String _serverHostname, int _serverPort, String _serverUrl, Msg msg) throws MalformedURLException, RemoteException, NotBoundException {
		if (_serverHostname != null){
			setServerHostname(_serverHostname);
		}
		if (_serverUrl != null){
			setServerUrl(_serverUrl);
		}
		rmiServer.MsgHandlerInterface msgHandler;
		Registry registry = LocateRegistry.getRegistry(_serverHostname, _serverPort);
		System.setSecurityManager(new RMISecurityManager());
		System.setProperty("sun.rmi.transport.tcp.responseTimeout", utils.rmiClientTimeout);
		msgHandler = (rmiServer.MsgHandlerInterface) registry.lookup(serverUrl);
		msgHandler.recvMsg(msg);
	}
	// Only for centralizedServer's client
	public void startPlayerGame(String serverHostname, int serverPort, String serverUrl, List<String> clientHostnames) throws RemoteException, NotBoundException, InterruptedException {
		Object objects[] = new Object[1];
		objects[0] = (Object)clientHostnames;
		rmiServer.MsgHandlerInterface msgHandler;
		System.setSecurityManager(new RMISecurityManager());
		System.setProperty("sun.rmi.transport.tcp.responseTimeout", utils.rmiClientTimeout);
		System.out.println(serverHostname);
		Registry registry = LocateRegistry.getRegistry(serverHostname, serverPort);
		msgHandler = (rmiServer.MsgHandlerInterface) registry.lookup(serverUrl);
		String payload = utils.createJson("clientHostnames", objects);
		Msg msg = new Msg(Types.START_PLAYER_GAME, 0, -1, payload);
		msgHandler.recvMsg(msg);
	}
	/* utils */
	public void setServerUrl(String serverUrl) {
		this.serverUrl = serverUrl;
	}
	public void setServerHostname(String serverHostname) {
		this.serverHostname = serverHostname;
	}
}
