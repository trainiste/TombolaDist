package player;
import gui.Gui;
import java.rmi.RemoteException;
import java.util.concurrent.TimeUnit;
import msg.Msg;
import ring.Ring;
import rmiClient.*;
import rmiServer.*;
import table.Tabella;
import utils.Utils;
import utils.Utils.Types;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
public class Player {
	Integer pid = 0;
	RMIServer rmiServer;
	RMIClient rmiClient;
	Utils utils;
	String centralizedServerHostname;
	String centralizedServerUrl;
	Ring ring; //reference to rmiServer's ring
	Msg lastMsg;
	Gui gui;
	Tabella tabella;
	public Player(String centralizedServerHostname) throws RemoteException {
		this.utils = new Utils();
		this.centralizedServerHostname = centralizedServerHostname;
		this.centralizedServerUrl = utils.defaultServerUrl+utils.centralizedServerUrl;
		tabella = new Tabella();
		rmiClient = new RMIClient(this.centralizedServerHostname, centralizedServerUrl, null); //third parameter is the player's server name
		pid = (Integer) rmiClient.initPlayer(this.centralizedServerUrl);
		rmiServer = new RMIServer(pid, rmiClient, tabella, null);
        gui = new Gui(rmiServer);
        gui.setTitle("Client " + pid);
		ring = rmiServer.getRing();
		rmiServer.rmiServerMain(); //bind server and start it
		SwingUtilities.invokeLater(new Runnable() {
		      public void run() {
          gui.setVisible(true);
      }
		});
		//create fake Msg for avoiding timeout expiration after the first msg
		Msg newMsg = new Msg(Types.END_GAME, 0, 0,  "");
		this.ring.saveMsg(newMsg);
		while(true) {
			try {
				TimeUnit.SECONDS.sleep(5);
				if(ring.isTimeoutExpired() && ring.ringSet) {
					gui.disable_drawButton();
					ring.election(); //call a new election
					//} //TODO the else case
				} else {
					ring.setTimeoutExpired(true);
				}

			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
