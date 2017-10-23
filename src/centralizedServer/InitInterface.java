package centralizedServer;
import java.rmi.*;
public interface InitInterface extends Remote {
    	   public int initPlayer() throws RemoteException;
    	   public int getCounter() throws RemoteException;
 }
