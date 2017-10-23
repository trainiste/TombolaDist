package utils;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
public class Utils {
	public int clientTimeout = 5;
	public String rmiClientTimeout = "1000";
	public String defaultServerUrl = "rmi://localhost/";
	public String msgHandlerUrl = "/msg";
	public String ringUrl = "/ring";
	public String centralizedServerUrl = "master";
	public int defaultCentralizedServerPort = 8079;
	public int defaultServerPort = 8080;
	public Utils() {
	}
	public String createJson(String what, Object object[]) {
		JSONObject obj = new JSONObject();
		if (what == "clientHostnames") {
			JSONArray arr = new JSONArray();
			for (String el : (List<String>) object[0]) {
				arr.add(el);
			}
			obj.put("clientHostnames", arr);
		} else if (what.equals("election") || what.equals("leader")) {
			obj.put("leader", (Integer) object[0]);
		} else if (what.equals("token")) { // two cases: either type was leader or election. They have the same key "leader"
			obj.put("token", (Integer) object[0]);
		} else if (what.equals("drawnNumber") || what.equals("retransmitDrawnNumber")) {
				obj.put("drawnNumber", (Integer) object[0]);
		} else if (what.equals("winNumbers")) {  //two cases, one while forwarding and one while sending its own numbers
			JSONArray arr = new JSONArray();
			for (Integer el : (List<Integer>) object[0]) {
				arr.add(el);
			}
			obj.put("winNumbers", arr);
			obj.put("confirmWin", (boolean) object[1]);
		}
		StringWriter out = new StringWriter();
		try {
			obj.writeJSONString(out);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String jsonText = out.toString();
		return jsonText;
	}
	public Object[] readJson(String what, String payload) throws ParseException {
		Object obj[] = null;
        JSONParser parser = new JSONParser();
        Object tmp_obj = parser.parse(payload);
		JSONObject jsonObj = (JSONObject) tmp_obj;
		List<String> clientHostnames = null;
		List<Integer> winNumbers;
		if (what.equals("clientHostnames")) {
			clientHostnames = new ArrayList<String>();
            JSONArray msg = (JSONArray) jsonObj.get("clientHostnames");
			Iterator<String> iterator = msg.iterator();
			while (iterator.hasNext()) {
				clientHostnames.add(iterator.next());
			}
			obj = new Object[1];
			obj[0] = (Object) clientHostnames;
		} else if (what.equals("election") || what.equals("leader")) { // two cases: either type was leader or election. They have the same key "leader"
			obj = new Object[1];
			obj[0] = (int) ((long) jsonObj.get("leader"));
		} else if (what.equals("token")) { // two cases: either type was leader or election. They have the same key "leader"
			obj = new Object[1];
			obj[0] = (int) ((long) jsonObj.get("token"));
		} else if (what.equals("drawnNumber") || what.equals("retransmitDrawnNumber")) {
			obj = new Object[1];
			obj[0] = (int) ((long) jsonObj.get("drawnNumber"));
		} else if (what.equals("winNumbers")) { //two cases, one while forwarding and one while sending its own numbers
			winNumbers = new ArrayList<Integer>();
            JSONArray msg = (JSONArray) jsonObj.get("winNumbers");
			Iterator<Long> iterator = msg.iterator();
			while (iterator.hasNext()) {
				int tmp = (iterator.next()).intValue();
				winNumbers.add(tmp);
			}
			obj = new Object[2];
			obj[0] = (Object) winNumbers;
			obj[1] = (boolean) (jsonObj.get("confirmWin"));
		}
		return obj;
	}
	public enum Types {
	     RING_INIT,
	     ELECTION,
	     LEADER,
	     START_PLAYER_GAME,
	     TAKE_TOKEN,
	     DRAWN_NUMBER,
	     RETRANSMIT_DRAWN_NUMBER,
	     ASK_WIN,
	     CONFIRM_WIN,
	     END_GAME
	}
}
