package rmiServer;
import java.rmi.*;
import java.rmi.server.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.json.simple.parser.ParseException;
import ring.Ring;
import utils.Utils;
import utils.Utils.Types;
import msg.Msg;
public class MsgHandler extends UnicastRemoteObject implements MsgHandlerInterface {
	Ring ring;
	Utils utils;
	List<Msg> msgQueue;
	public MsgHandler(Ring ring, List<Msg> msgQueue) throws RemoteException {
		this.ring = ring;
		this.msgQueue = msgQueue;
		this.utils = new Utils();
	}
	public void recvMsg(Msg msg){
		//simply add a Msg to the queue
		msgQueue.add(msg);
		synchronized (msgQueue) {
		    msgQueue.notify();
		}
	}
}
