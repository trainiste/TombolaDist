package thread;
import gui.Gui;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.json.simple.parser.ParseException;
import msg.Msg;
import ring.Ring;
import table.Tabella;
import utils.Utils;
import utils.Utils.Types;
public class MsgThread extends Thread {
	Ring ring;
	List<Msg> msgQueue;
	Utils utils;
	Tabella tabella;
	boolean readyToDie;
	Gui gui;
	boolean retransmit; //avoid infinite retransmit
	public MsgThread(Ring ring,	List<Msg> msgQueue, Tabella tabella, Gui gui) {
		this.ring = ring;
		this.msgQueue = msgQueue;
		this.utils = new Utils();
		this.tabella = tabella;
		this.readyToDie = false;
		this.retransmit = true;
		this.gui = gui;
	}
	@Override
	public void run() {
		Msg tmp_msg;
		while(true) {
			if(!msgQueue.isEmpty()) {
				tmp_msg = msgQueue.remove(0);
				handleMsg(tmp_msg);
			} else { //no Msgs, just sleep
				try {
					synchronized (msgQueue) {
					    msgQueue.wait();
					}
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	public void handleMsg(Msg msg){
		System.out.println(msg.getType()+": "+msg.getPayload());
		//discard messages from nodes now out of the ring
		if(ring.ringSet && ring.getServerPids().indexOf(msg.getSender()) < 0) {
			return;
		}
		//check if I have to die
		if(readyToDie && msg.getType() != Types.ASK_WIN) {
			ring.sendEndGame(Types.END_GAME);
			System.exit(0);
		}
		if(ring.ringSet) {
			rebuildRing(msg.getSender());
		}
		if(tabella.getWin()) {
			tabella.setWin(false);
			ring.forwardWin(Types.ASK_WIN, Arrays.asList(tabella.getTabellaNumbers()), true);
			System.out.println("I win");
		}
		ring.saveMsg(msg); //save the new msg properly delivered
		Integer tmp_pid = -1;
		switch(msg.getType()) {
		case START_PLAYER_GAME:
			List<String> serverHostnames = null;
			try {
				serverHostnames = (List<String>) utils.readJson("clientHostnames", msg.getPayload())[0];
				ring.initializeServerHostnames(serverHostnames);
			} catch (ParseException e) {
				System.out.println("Error while parsing JSON Object: "+msg.getPayload());
			}
			break;
		case ELECTION:
			// new election
			if(ring.getLeader() == ring.getPid()) { //if I am the leader
				System.out.println("I am still alive!");
				//discard the msg and force a new election with me as leader
				ring.setRingLeader(); // I am the leader, let world now
				boolean send_omission = false;
				if(tabella.getDrawnNumbers().indexOf(tabella.getMyLastDrawnNumber()) < 0) { //if send omission not already handled
					send_omission = true;
				}
				if(tabella.getMyLastDrawnNumber() != -1 && send_omission) { //it was not the beginning
					ring.sendDrawnNumber(Types.RETRANSMIT_DRAWN_NUMBER, tabella.getMyLastDrawnNumber());//send the last number again
				}
				return;
			}
			if(checkIsMyTurn(msg.getSender())) { //leader has crashed but it's my turn
				ring.setLeader(ring.getPid()); //set myself as leader
				retransmit = false;
				ring.setRingLeader(); // I am the leader, let world now
				return;
			}
			try {
				tmp_pid = (Integer) utils.readJson("election", msg.getPayload())[0];
			} catch (ParseException e) {
				System.out.println("Error while parsing JSON Object: "+ msg.getPayload());
			}
			if (tmp_pid == ring.getPid()) { // my message came back, so I am the leader
				ring.setLeader(tmp_pid);
				retransmit = false;
				ring.setRingLeader(); // I am the leader, let world now
				return;
			} else if (tmp_pid < ring.getPid()) {
				if (ring.getPendingElection()) { // if I am already in Election state
					return; // just discard the packet
				} else { // I want to be the leader
					tmp_pid = ring.getPid();
				}
			}
			ring.setPendingElection(true);
			ring.ringForwardLeader(Types.ELECTION, tmp_pid);
			break;
		case LEADER:
			try {
				tmp_pid = (Integer) utils.readJson("election", msg.getPayload())[0];
			} catch (ParseException e) {
				System.out.println("Error while parsing JSON Object: "+msg.getPayload());
			}
			ring.setLeader(tmp_pid);
			//if it is a different pid, forward the msg
			if(tmp_pid != ring.getPid()) {
				ring.ringForwardLeader(Types.LEADER, tmp_pid);
			} else {
				retransmit = false;
				gui.setInfoText("Your turn. Please draw a number");
				//enable drawing
				gui.enable_drawButton();
			}
			break;
		case TAKE_TOKEN:
			retransmit = false;
			ring.setLeader(ring.getPid()); //someone gave me the token
			ring.ringForwardLeader(Types.LEADER, ring.getPid());
			break;
		case DRAWN_NUMBER:
			try {
				int n = (Integer) utils.readJson("drawnNumber", msg.getPayload())[0];
				List tmp_list = tabella.getDrawnNumbers();
				boolean leader_crash = false;
				if(tabella.getDrawnNumbers().indexOf(n) > -1) {
					leader_crash = true;
				}
				tabella.addNumber(n); //add number in queue and forward it
				gui.fillLastNumberView(n);
				gui.fillDrawnNumbersView();
				if (n != tabella.getMyLastDrawnNumber()) { // it does not come from me
					if(leader_crash) { //number already in queue. Possibly leader has crashed, now it's me
						//System.out.println("CIAOOOOO");
						gui.setInfoText("Your turn. Please draw a number");
						//enable drawing
						gui.enable_drawButton();
						ring.setLeader(ring.getPid());
						retransmit = false;
						ring.setRingLeader();
					} else {
						if (tabella.checkWin()) {
							tabella.setWin(true);
						}
						ring.sendDrawnNumber(Types.DRAWN_NUMBER, n);
					}
				} else {
					if(ring.ringLength == 1 ) { //just myself in the ring
						if(tabella.checkWin()) {
							ring.forwardWin(Types.ASK_WIN, Arrays.asList(tabella.getTabellaNumbers()), true);
						}
					}
					TimeUnit.SECONDS.sleep(2);
					gui.disable_drawButton();
					gui.setInfoText("");
					retransmit = true;
					ring.releaseToken();
				}
			} catch (Exception e) {
				e.printStackTrace();
			};
			break;
		case RETRANSMIT_DRAWN_NUMBER: //there was a send omission somewhere
			try {
				int n = (Integer) utils.readJson("retransmitDrawnNumber", msg.getPayload())[0];
				List tmp_list = tabella.getDrawnNumbers();
				boolean still_send_omission = false;
				boolean leader_crash = false;
				if(tabella.getDrawnNumbers().indexOf(n) < 0) { //if send omission not already handled
					still_send_omission = true;
				}
				if(tabella.getDrawnNumbers().indexOf(n) > -1) {
					leader_crash = true;
				}
				tabella.addNumber(n); // add number in queue and forward it
				gui.fillLastNumberView(n);
				gui.fillDrawnNumbersView();
				if (ring.getPid() == ring.getLeader() && still_send_omission) { // it was from me

						if (tabella.getMyLastDrawnNumber() != -1) { // if it was not the beginning
							TimeUnit.SECONDS.sleep(3);
							gui.disable_drawButton();
							gui.setInfoText("");
							retransmit = true;
							ring.releaseToken();
						}
				} else {
					if(tabella.checkWin()) {
						tabella.setWin(true);
					}
					if(retransmit) {
						ring.sendDrawnNumber(Types.RETRANSMIT_DRAWN_NUMBER, n);
					}
					}
			} catch (Exception e) {
				e.printStackTrace();
			}
			break;
		case ASK_WIN: //compare values of the win array with mine
			boolean ok_win = true;
			List<Integer> win_numbers  = null;
			try {
				Object obj[] = utils.readJson("winNumbers", msg.getPayload());

				win_numbers = (List<Integer>)obj[0];
				System.out.println(win_numbers.toString());
				boolean sameLists = win_numbers.equals(Arrays.asList(tabella.getTabellaNumbers()));
				ok_win = (boolean) obj[1];
				if(sameLists && ok_win && ring.ringLength > 1) { //if my msg has just returned to myself, and I am not alone
					readyToDie = true;
				} else if(sameLists) { //it was me requiring the win but I made a mistake, restore the table and discard the packet
					tabella.restoreStatus();
					gui.restoreStatus();
					return;
				}
				if(ok_win) { //if others are agree about win, let check me too
					ok_win = checkWin(win_numbers, tabella.getDrawnNumbers());
				}
				ring.forwardWin(Types.ASK_WIN, win_numbers, ok_win);
				if(sameLists && readyToDie) { //ready to die was set at the previous msg. I waited for someone else that had won
					ring.sendEndGame(Types.END_GAME);
					gui.setInfoText("You win");
					TimeUnit.SECONDS.sleep(3);
					System.exit(0);
				}
			} catch (ParseException | InterruptedException e) {
				System.out.println("Error while parsing JSON Object: "+ msg.getPayload());
			}
			break;
		case END_GAME:
			ring.sendEndGame(Types.END_GAME);
			gui.setInfoText("You lose");
			try {
				TimeUnit.SECONDS.sleep(3);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			System.exit(0);
			break;
		default:
			break;
		}
	}
	public void rebuildRing(int sender) {
		synchronized (ring.getServerPids()) {
		List<String> tmp_serverHostnames = new ArrayList<String>(ring.getServerHostnames()); //array of clients to serve
		List<Integer> tmp_serverPids = new ArrayList<Integer>(ring.getServerPids()); //array of clients to serve
		int tmp_ringLength = ring.getRingLength();
		boolean end = false;
		int index = tmp_serverPids.indexOf(sender);
		int value = -1;
		int inc = 1;
		System.out.println("Sender: "+sender+" with index: "+index);
		System.out.println(tmp_serverPids.toString());
		while(!end) {
			index = (index+inc)%tmp_ringLength;
			value = tmp_serverPids.get(index);
			System.out.println("value: "+value+" with index: "+index);
			if(value == ring.getPid() || value == sender) { //no crash, it is myself or I have already deleted crashed nodes
				break;
			}
			tmp_serverPids.remove(index);
			tmp_serverHostnames.remove(index);
			tmp_ringLength--;
			inc = 0;
		}
			ring.setRingLength(tmp_ringLength);
			ring.setServerHostnames(tmp_serverHostnames);
			ring.setServerPids(tmp_serverPids);
			System.out.println(tmp_serverPids.toString());
		}
	}
	public boolean checkIsMyTurn(int sender) {
		boolean res = false;
		int old_leader = ring.getLeader();
		List<Integer> tmp_serverPids = new ArrayList<Integer>(ring.getServerPids());
		if(ring.maxRingPid == 0 || ((sender+1)%ring.maxRingPid == ring.getPid() && sender == ring.getLeader())) { //old leader was the sender
			res = false; //if leader has sent it is not crashed
		} else if(ring.getPid() > old_leader){
			if(old_leader > sender && old_leader < ring.getPid()){
				res = true;
			}
		} else if(sender == tmp_serverPids.get(tmp_serverPids.size()-1) && old_leader > sender){
			res = true;
		}
		return res;
	}
	public boolean checkWin(List<Integer> win_numbers, List<Integer> drawn_numbers) {
		boolean res = true;
		for(int i = 0; i < win_numbers.size(); i++ ) { //for each number check its position in
			if(drawn_numbers.indexOf(win_numbers.get(i)) < 0) {
				res = false;
				break;
			}
		}
		return res;
	}
}
