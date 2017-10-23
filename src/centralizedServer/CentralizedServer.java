package centralizedServer;
import utils.*;
import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import rmiClient.RMIClient;
public class CentralizedServer {
	//Server centralizedServer;
	static RMIClient centralizedClient;
	static Utils utils;
	static int timeout;
	static Init init;
	String name;
	String url;
	static List<String> clientHostnames; //array of clients to serve
	//array of addresses
	public CentralizedServer() {
		this.utils = new Utils();
		this.name = utils.centralizedServerUrl; //set the server's name
		this.url = utils.defaultServerUrl+name;
		//this.centralizedServer = new Server(utils.centralizedServerUrl);
		this.timeout = utils.clientTimeout;
		try {
			this.init = new Init();
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public void CentralizedServerMain() {
		try {
			System.setSecurityManager(new RMISecurityManager());
			System.out.println(url);
			Registry registry = LocateRegistry.createRegistry(utils.defaultCentralizedServerPort);
			registry.rebind(url, init);
		} catch (Exception e) {
			System.out.println("Server "+ name +" failed: " + e);
		}
	}
	public static void main (String[] argv) throws RemoteException {
		CentralizedServer cs = new CentralizedServer();
		cs.CentralizedServerMain();
		String tempUrl = utils.defaultServerUrl;
		try {
			TimeUnit.SECONDS.sleep(timeout);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
			System.out.println("Let's start");
			clientHostnames = new ArrayList<String>(init.getClientHostnames());
			centralizedClient = new RMIClient(null, null, "0".toString()); // Only serverUrl defined (centralizedServer is useless)
			int tempCounter = init.getCounter();
			System.out.println("Found "+clientHostnames.size()+" hosts");
			int tmp_port = utils.defaultServerPort;
			for (int i = 0; i < tempCounter; i++){
				String serverUrl = tempUrl + ((Integer)i).toString() + utils.msgHandlerUrl;
				tmp_port += i;
				try {
					centralizedClient.startPlayerGame(clientHostnames.get(i), utils.defaultServerPort+i, serverUrl, clientHostnames);
				} catch (Exception e) {
					System.out.println(e);
					System.out.println("Impossible to reach "+clientHostnames.get(i)+":"+((Integer) tmp_port));
					continue;
				}	
			}
	}
}
