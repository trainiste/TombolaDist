package rmiServer;
import java.rmi.*;
import msg.Msg;
public interface MsgHandlerInterface extends Remote {
    	   public void recvMsg(Msg msg) throws RemoteException;
 }
