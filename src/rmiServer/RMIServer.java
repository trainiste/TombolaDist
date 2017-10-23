package rmiServer;
import gui.Gui;
import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;
import msg.Msg;
import ring.Ring;
import rmiClient.RMIClient;
import table.Tabella;
import thread.MsgThread;
import utils.Utils;
public class RMIServer {
	String name;
	String url;
	Ring ring;
	RMIClient rmiClient; //server's client
	MsgHandler msgHandler;
	String msgHandlerUrl;
	String ringUrl;
	int pid;
	Utils utils;
	List<Msg> msgQueue;
	MsgThread readMsgThread;
	Tabella tabella;
	Gui gui;
	public RMIServer(int pid, RMIClient rmiClient, Tabella tabella, Gui gui) throws RemoteException {
		this.pid = pid;
		this.utils = new Utils();
		this.name = ((Integer)pid).toString(); //set the server's name
		this.url = utils.defaultServerUrl+name;
		this.rmiClient = rmiClient;
		this.ring = new Ring(pid, rmiClient);
		this.msgQueue = new ArrayList<Msg>();
		this.tabella = tabella;
		this.msgHandler = new MsgHandler(ring, msgQueue);
		this.gui = gui;
		msgHandlerUrl = utils.msgHandlerUrl;
		ringUrl = utils.ringUrl;
	}
	public void rmiServerMain() {
		readMsgThread = new MsgThread(ring, msgQueue, tabella, gui);
		readMsgThread.start(); //start read thread
		try {
			System.setSecurityManager(new RMISecurityManager());
			System.out.println("Server "+ url);
			Registry registry = LocateRegistry.createRegistry(utils.defaultServerPort+pid);
			registry.rebind(url+msgHandlerUrl, msgHandler); // expose msgHandler object
		} catch (Exception e) {
			System.out.println("Server "+ name +" failed: " + e);
		}
	}
	public Ring getRing() {
		return ring;
	}
	public void setGui(Gui gui) {
		this.gui = gui;
	}
	public Tabella getTabella() {
		return tabella;
	}
}
