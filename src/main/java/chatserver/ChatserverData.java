package chatserver;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.InetSocketAddress;
import java.util.concurrent.Executor;

import util.Config;
import java.util.Set;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;



public class ChatserverData {
	
	private HashMap<InetSocketAddress,String> IPUsernameMap;
	private HashMap<String,String> UserPasswordMap;
	private HashMap<String,Boolean> UserLoginStatusMap;
	private int NumberOfUsers;
	private Queue<Message> PendingMessages; // NOT USED
	private boolean CloseFlag;
	private List<TCPConnection> threads;
	private HashMap<String,String> LastPublicMessages;
	//private HashMap<String,InetSocketAddress> RegisterMap;
	private HashMap<String,String> RegisterMapString;
	
	public ChatserverData(){
		Config users = new Config("user");
		this.UserPasswordMap = createUserPasswordMap(users);
		this.UserLoginStatusMap = initiateLoginStatuses(users);
		this.NumberOfUsers = users.listKeys().size();
		this.IPUsernameMap = new HashMap();
		this.PendingMessages = new LinkedList<Message>();
		this.CloseFlag = false;
		this.threads = new ArrayList<TCPConnection>();
		this.LastPublicMessages = initializeLastPublicMessages();
		//this.RegisterMap = new HashMap<>();
		this.RegisterMapString = new HashMap<>();
	}
	
	public synchronized String registerUser(String request, String username){
		//RegisterMap.put(username, address);
		String registerdata = request.substring(10,request.length());
		/*String[] temp = registerdata.split(":");
		String ip = temp[0];
		int port = Integer.parseInt(temp[1]);
		RegisterMap.put(username, new InetSocketAddress(ip,port));*/
		RegisterMapString.put(username, registerdata);
		return "Successfully registered address for "+username+".";
	}
	public synchronized String lookupAddress(String request){
		String username = request.substring(8,request.length());
		return ((RegisterMapString.get(username) == null ? "Wrong username or user not reachable.":RegisterMapString.get(username)));

	}
	
	private HashMap<String,String> initializeLastPublicMessages(){
		HashMap<String,String> map = new HashMap<>();
		Iterator it = UserLoginStatusMap.entrySet().iterator();
	    while (it.hasNext()){
	        Map.Entry pair = (Map.Entry)it.next();
	        map.put((String)pair.getKey(),"No message received!");
	    }
		return map;
	}
	public synchronized void addThread(TCPConnection t){
		threads.add(t);
	}
	public synchronized int numberOfActiveUsers(){
		return IPUsernameMap.keySet().size();
	}
	
	public synchronized void setCloseFlag(){
		CloseFlag = true;
	}
	public synchronized boolean checkCloseFlag(){
		return CloseFlag;
	}
	public synchronized void sendMessage(String message, String sendingUser){
		//PendingMessages.add(new Message(message,sendingUser));
		for(TCPConnection t : threads){
			t.printMessage(sendingUser+": "+message);
		}
		Iterator it = LastPublicMessages.entrySet().iterator();
	    while (it.hasNext()) {
	        Map.Entry pair = (Map.Entry)it.next();
	        //System.out.println("Key: "+(String)pair.getKey()+" sendingUser: "+sendingUser);
	        if(!((String)pair.getKey()).equals(sendingUser)){
	        	if(UserLoginStatusMap.get(sendingUser))
		        	//System.out.println("Hier sollte er nur zwei mal herkommen");
	        		LastPublicMessages.put((String)pair.getKey(),message);
	        }
	    }
	}
	public synchronized String lastPublicMessage(String username){
		return LastPublicMessages.get(username);
	}
	/**
	 * NOT USED
	 * @return
	 */
	public synchronized String returnPendingMessage(){
		if(PendingMessages.isEmpty()) return null;
		Message message = PendingMessages.peek();
		message.incrementReadBy();
		System.out.println("Message was seen: "+ message.Readby()+ " times of "+numberOfActiveUsers()+" times.");
		String sendingText = message.SenderUsername() + ": "+ message.Message();
		if(message.Readby() >= numberOfActiveUsers()) {
			PendingMessages.remove();
			System.out.println("Message was removed");
		}
		return sendingText;
	}
	
	public synchronized int getNumberOfUsers(){
		return NumberOfUsers;
	}
	
	/**
	 * @param address
	 * @param username
	 */
	private void addActiveUser(InetSocketAddress address, String username){
		IPUsernameMap.put(address, username);
		UserLoginStatusMap.put(username, true);
	}
	
	public synchronized void deleteActiveUser(InetSocketAddress address, String username){
		UserLoginStatusMap.put(username, false);
		IPUsernameMap.remove(address);
		
	}
	
	private HashMap<String,String> createUserPasswordMap(Config _userproperties){
		HashMap<String,String> UserPasswordMap = new HashMap();
		Set<String> users = _userproperties.listKeys();
		for(String st : users){
			String actualUsername = st.substring(0, st.length()-9);
			UserPasswordMap.put(actualUsername,_userproperties.getString(st));
		}
		return UserPasswordMap;
	}
	
	public synchronized String formattedUserLoginStatuses() {
	    Iterator it = UserLoginStatusMap.entrySet().iterator();
	    String output = "";
	    while (it.hasNext()) {
	        Map.Entry pair = (Map.Entry)it.next();
	        output += (pair.getKey() +  ((Boolean)pair.getValue()?" is online\n":" is offline\n"));
	    }
	    return output;
	}
	
	public synchronized void setLoginStatus(String _username, boolean _status){
		UserLoginStatusMap.put(_username, _status);
	}
	
	public synchronized boolean getLoginStatus(String _username){
		if(!doesUserExist(_username)) return false;
		return UserLoginStatusMap.get(_username);
		
	}
	
	private HashMap<String,Boolean> initiateLoginStatuses(Config _userproperties){
		HashMap<String,Boolean> StatusMap = new HashMap();
		Set<String> users = _userproperties.listKeys();
		for(String st : users){
			String actualUsername = st.substring(0, st.length()-9);
			StatusMap.put(actualUsername,false);
		}
		return StatusMap;
	}
	
	public synchronized boolean doesUserExist(String username){
		if(UserPasswordMap.containsKey(username)) return true;
		
		return false;
	}
	
	private boolean correctPasswordForUser(String username,String password){
		if(UserPasswordMap.get(username).equals(password)) return true;
		else return false;
	}
	
	public synchronized boolean loginUser(String username, String password, InetSocketAddress adress){
		if(!doesUserExist(username)) return false;
		if(correctPasswordForUser(username,password)){
			addActiveUser(adress,username);
			return true;
		}
		return false;
	}
}
