package ring;
import java.rmi.*;
import java.rmi.server.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import msg.Msg;
import rmiClient.RMIClient;
import utils.Utils;
import utils.Utils.Types;
public class Ring extends UnicastRemoteObject{
	private static final long serialVersionUID = 1L;
	public int maxRingPid = 0;
	public int ringLength = 0;
	int pid;
	int ringClock;
	Utils utils;
	RMIClient rmiClient;
	String centralizedServerUrl;
	List<String> serverHostnames; //array of clients to serve
	public List<Integer> serverPids; //array of clients to serve
	boolean expiredTimeout;
	public Msg lastMsg[]; //keep in memory the last message and the previous one
	public boolean ringSet; //is the ring set?
	int leaderPid;
	boolean pendingElection; //flag for election
	public Ring(int pid, RMIClient rmiClient) throws RemoteException {
		this.utils = new Utils();
		this.pid = pid;
		this.ringClock = 0; //our convention for accepting the first message
		this.rmiClient = rmiClient;
		this.lastMsg = new Msg[2];
		this.lastMsg[0] = this.lastMsg[1] = null;
		this.centralizedServerUrl = utils.centralizedServerUrl;
		this.ringSet = false;
		this.leaderPid = -1;
		this.serverPids = new ArrayList<Integer>();
		this.pendingElection = false;
	}
	public int getPid() {
		return pid;
	}
	public int getRingLength() {
		return ringLength;
	}
	public void setRingLength(int ringLength){
		this.ringLength = ringLength;
	}
	public List<Integer> getServerPids() {
		return serverPids;
	}
	public void setServerPids(List<Integer> serverPids) {
		this.serverPids = serverPids;
	}
	public void setServerHostnames(List<String> serverHostnames) {
		this.serverHostnames = serverHostnames;
	}
	public List<String> getServerHostnames() {
		return serverHostnames;
	}
	public void initializeServerHostnames(List<String> serverHostnames) {
		this.serverHostnames = serverHostnames;
		setMaxRingPid(this.serverHostnames.size());
		for(int i = 0; i < this.serverHostnames.size(); i++ ) {
			this.serverPids.add(i); //at the beginning we are sure that every process is not crashed. So we add its id to the list
		}
		initializeRing();
	}
	public void setMaxRingPid(int ringLength){
		this.ringLength = ringLength;
		this.maxRingPid = ringLength-1;
	}
	public void initializeRing(){
		try {
			TimeUnit.SECONDS.sleep(1);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Msg newMsg = new Msg(Types.RING_INIT, 1, pid, "msg coming from: " + pid + " with clock " + 1 ); //create new Msg
		saveMsg(newMsg);
		String tempServerUrl = utils.defaultServerUrl;
		Integer tmp_pid = (pid+1) % ringLength; //index of pid inside serverPids. At the beginning it is next
		int tmp_port = utils.defaultServerPort + tmp_pid;
		String serverUrl = tempServerUrl + (tmp_pid).toString();
		serverUrl += utils.msgHandlerUrl;
		try {
			rmiClient.sendRingMsg(serverHostnames.get(tmp_pid), tmp_port, serverUrl, newMsg);
		} catch (Exception e) {
			crashHandler(tmp_pid);
			for (int i = 1; i < ringLength; i++) { // start from 1 for avoiding msg to itself
				tmp_pid = serverPids.get(i);
				tmp_port = utils.defaultServerPort + tmp_pid;
				serverUrl = tempServerUrl + (tmp_pid).toString();
				serverUrl += utils.msgHandlerUrl;
				newMsg.setPayload("msg coming from: " + pid );
				try {
					rmiClient.sendRingMsg(serverHostnames.get(i), tmp_port, serverUrl, newMsg);
					break; //go out
				} catch (Exception e1) {
					crashHandler(i);
					i--;
					continue;
				}
			}
		}
		this.ringSet = true;
		election(); //call a new election
	}
	public void crashHandler(int index) {
		System.out.println("Node "+serverPids.get(index) + " has crashed!");
		serverHostnames.remove(index); //delete element from hostnames since it crashed
		serverPids.remove(index);
		//now we are sure that a process' IP inside serverHostanames in position
		//X has its own pid inside serverPids in position x
		this.ringLength--;
		System.out.println("New length is: "+this.ringLength);
	}
	//election
	public void election() {
		Object objects[] = new Object[1];
		objects[0] = (Object)this.pid;
		pendingElection = true;
		String payload = utils.createJson("leader", objects);
		Msg newMsg = new Msg(Types.ELECTION, ringClock+1, pid, payload);
		sendMsg(newMsg);
	}
	public void setLeader(Integer pid) {
		this.leaderPid = pid;
		pendingElection = false;
		System.out.println("New Leader: "+pid);
	}
	public int getLeader() {
		return this.leaderPid;
	}
	public boolean getPendingElection() {
		return pendingElection;
	}
	public void setPendingElection(boolean value) {
		this.pendingElection = value;
	}
	public void setRingLeader(){
		Object objects[] = new Object[1];
		objects[0] = (Object)this.pid;
		String payload = utils.createJson("leader", objects);
		Msg newMsg = new Msg(Types.LEADER, ringClock+1, pid, payload);
		sendMsg(newMsg);
	}
	public void ringForwardLeader(Types t, int pid){
		Object objects[] = new Object[1];
		objects[0] = (Object)pid;
		String payload = utils.createJson("leader", objects);
		Msg newMsg = new Msg(t, ringClock+1, pid, payload);
		sendMsg(newMsg);
	}
	public void sendDrawnNumber(Types t, int number){
		Object objects[] = new Object[1];
		objects[0] = (Object)number;
		String payload = "";
		if(t == Types.DRAWN_NUMBER) {
			payload = utils.createJson("drawnNumber", objects);
		} else {
			payload = utils.createJson("retransmitDrawnNumber", objects);
		}
		Msg newMsg = new Msg(t, ringClock+1, pid, payload);
		sendMsg(newMsg);
	}
	public void forwardWin(Types t, List<Integer> numbers, boolean ok_win){
		Object objects[] = new Object[2];
		objects[0] = (Object)numbers;
		objects[1] = (Object)ok_win;
		String payload = utils.createJson("winNumbers", objects);
		Msg newMsg = new Msg(t, ringClock+1, pid, payload);
		sendMsg(newMsg);
	}
	public void releaseToken(){
		Object objects[] = new Object[1];
		objects[0] = (Object)pid;
		String payload = utils.createJson("token", objects); //add myself as sender
		Msg newMsg = new Msg(Types.TAKE_TOKEN, ringClock+1, pid, payload);
		sendMsg(newMsg);
	}
	public void sendEndGame(Types t){
		Msg newMsg = new Msg(t, ringClock+1, pid, "");
		sendMsg(newMsg);
	}
	public void sendMsg(Msg msg){
		synchronized(serverPids) {
			ringClock++;
		String tempServerUrl = utils.defaultServerUrl;
		saveMsg(msg);
		Integer tmp_index = (serverPids.indexOf(pid)+1)%ringLength;
		Integer tmp_pid = (Integer) (serverPids.get((tmp_index) % ringLength));
		int tmp_port = utils.defaultServerPort + tmp_pid;
		//set the Msg's sender
		msg.setSender(pid);
		String serverUrl = tempServerUrl + (tmp_pid).toString();
		serverUrl += utils.msgHandlerUrl;
		System.out.println("Send msg: "+tmp_index+", "+ serverUrl+ ", " +tmp_port);
		try {
			rmiClient.sendRingMsg(serverHostnames.get(tmp_index), tmp_port, serverUrl, msg);
		} catch (Exception e) {
			System.out.println("Index to remove: " + tmp_index);
			crashHandler(tmp_index);
			for (int i = 1; i <= serverPids.size(); i++) { // start from 1 for avoiding msg to itself
				tmp_index = (serverPids.indexOf(pid)+i)%ringLength;
				tmp_pid = (Integer) (serverPids.get((tmp_index)));
				System.out.println("Let me try with: "+tmp_pid+" con indice: "+ tmp_index);
				try {
				TimeUnit.SECONDS.sleep(2);
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
				tmp_port = utils.defaultServerPort + tmp_pid;
				serverUrl = tempServerUrl + (tmp_pid).toString();
				serverUrl += utils.msgHandlerUrl;
				try {
					rmiClient.sendRingMsg(serverHostnames.get(tmp_index), tmp_port, serverUrl, msg);
					break; //go out
				} catch (Exception e1) {
					crashHandler(tmp_index);
					i--;
					continue;
				}
			}
		}
		}
	}
	//utils
	public boolean isTimeoutExpired() {
		if(lastMsg[0].equals(lastMsg[1])) {
			return true;
		}
		return false;
	}
	public void setTimeoutExpired(boolean what) {
		if(what) {
			saveMsg(lastMsg[1]); //in order to make timeout expired, we manually set lastMsg equal
		}
		expiredTimeout = what;
	}
	public void saveMsg(Msg newMsg) {
		if(lastMsg[0] == null && lastMsg[1] == null) { //at the beginning both are empty
			lastMsg[0] = lastMsg[1] = newMsg;
		}
		lastMsg[0] = lastMsg[1];
		lastMsg[1] = newMsg;
		expiredTimeout = false;
		setClock(newMsg.getClock());
	}
	public Msg remoteMsg(Msg msg) throws RemoteException {
		return new Msg(msg.getType(), msg.getClock(), pid, msg.getPayload());
	}
	public int getClock() {
		return this.ringClock;
	}
	public void setClock(int clock) {
		this.ringClock = clock;
	}
}
