package main;
import rmiServer.*;
import rmiClient.*;
import java.rmi.*;
import java.rmi.server.*;
import player.Player;
public class Main {
	public static void main(String[] argv) {
		String centralizedServerHostname = "127.0.0.1";
		if(argv.length == 1) {
			centralizedServerHostname = argv[0];
		}
		try {
			Player newplayer = new Player(centralizedServerHostname);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
