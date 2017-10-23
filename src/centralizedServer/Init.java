package centralizedServer;
import java.rmi.*;
import java.rmi.server.*;
import java.util.ArrayList;
import java.util.List;
public class Init extends UnicastRemoteObject implements InitInterface {
	int counter = -1;
	static List<String> clientHostnames = new ArrayList<String>();
	public Init() throws RemoteException {
	}
	public int initPlayer() throws RemoteException {
		try {
			String clientHostname = java.rmi.server.RemoteServer.getClientHost();
			clientHostnames.add(clientHostname);
		} catch (ServerNotActiveException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		counter++;
		return counter;
	}
	public int getCounter() throws RemoteException {
		return counter+1; //length is not starting from 0
	}
	public List<String> getClientHostnames() throws RemoteException {
		return clientHostnames; //length is not starting from 0
	}
}
