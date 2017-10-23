package msg;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import utils.Utils.Types;
public class Msg implements Serializable {
	Types type;
	int clock;
	int sender;
	String payload;
	public Msg(Types election, int clock, int sender, String payload) {
		this.type = election;
		this.clock = clock;
		this.sender = sender;
		this.payload = payload;
	}
	public Types getType() {
		return type;
	}
	public int getClock() {
		return clock;
	}
	public int getSender() {
		return sender;
	}
	public String getPayload() {
		return payload;
	}
	public void setSender(int sender) {
		this.sender = sender;
	}
	public void setPayload(String _payload) {
		this.payload = _payload;
	}
}
