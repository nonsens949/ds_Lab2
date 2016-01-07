package chatserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.InterruptedException;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.InetSocketAddress;

public class TCPConnection implements Runnable{

	private ServerSocket serverSocket;
	private boolean loggedIn;
	private String username;
	private String password;
	private ChatserverData data;
	private Socket socket;
	private InetSocketAddress address;
	private BufferedReader reader;
	private PrintWriter writer;
	
	public TCPConnection(ServerSocket serverSocket, ChatserverData data) {
		this.serverSocket = serverSocket;
		this.data = data;
		this.loggedIn = false;
		this.username = "";
		this.password = "";
		this.socket = null;
		this.address = null;
		this.reader = null;
		this.writer = null;
	}
	
	private boolean login(String request){
		
		if(!request.startsWith("!login ")){
			writer.println("You have to login first.");
			return false;
		}
		else{
			String[] logindata = null;
			try{
				logindata = request.substring(7, request.length()).split("\\s");
			}
			catch(Exception e){
				writer.println("Couldn't read login data.");
				return false;
			}
			if(logindata.length != 2){ 
				writer.println("Too many or too little parameters.");
				return false;
			}
			String username = logindata[0];
			String password = logindata[1];
			if(data.doesUserExist(username)){
				if(data.getLoginStatus(username)){
					writer.println("User with that username already logged in. Try another user.");
					return false;
				}
			}else{
				writer.println("Wrong username or password.");
				return false;
			}
			if(data.loginUser(username, password, new InetSocketAddress(socket.getInetAddress(),socket.getPort()))){
				writer.println("Successfully logged in.");
				loggedIn = true;
				this.username = username;
				this.password = password;
				this.address = new InetSocketAddress(socket.getInetAddress(),socket.getPort());
			}
			else{
				writer.println("Wrong username or password.");
				return false;
			}				
		}			
		return true;
	}
	
	public void logout(){
		data.deleteActiveUser(address, username);
		data.setLoginStatus(username, false);
		this.loggedIn = false;
		this.username = "";
		this.password = "";
		this.address = null;
		writer.println("Successfully logged out.");
		System.out.println("User "+username+" successfully logged out.");
	}
	
	public void send(String request){
		String message = request.substring(6,request.length());
		if(message.length()==0) return;
		data.sendMessage(message, username);
	}
	
	public void checkForPendingMessages(){
		String messageText = data.returnPendingMessage();

		if(messageText == null) return;
		else{
			System.out.println("Incoming Message (shown on every user): "+messageText);
			if(messageText.contains(username) || messageText.isEmpty()) return;
			else
				System.out.println(messageText);
		}
	}
	public void printMessage(String message){
		if(!loggedIn)return;
		if(message.contains(username))return;
		else System.out.println(message);
	}
	
	public void close() throws InterruptedException{
		if (socket != null && !socket.isClosed()){
			try {
				reader.close();
				writer.close();
				socket.close();
			} 
			catch (IOException e) {}
		}
		throw new InterruptedException();
	}
	public void InterrupsForMessage(){
	}
	
	public void run() {
		while(true){
			try{
				try{
					socket = serverSocket.accept();
				}catch(SocketException e){
					close();
				}
				reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				writer = new PrintWriter(socket.getOutputStream(),true);
				if(data.checkCloseFlag()) close();
				
				//Verarbeitung der Message
				String request;
				while ((request = reader.readLine()) != null) {
					//if(Thread.interrupted()) close();
					if(data.checkCloseFlag()) close();
					
					//request = reader.readLine();

					if(request!=null){
						if(!request.startsWith("!")) writer.println("Request has to start with '!'");
						
						if(!loggedIn){								//!login
							if(!login(request)) continue;
						}
						if(request.equals("!logout")){				//!logout
							logout();
							close();
							break;
							
						}else if(request.startsWith("!send ")){ 	//!send
							send(request);
							
						}else if(request.startsWith("!lastMsg")){ 	//!lastmsg
							writer.println(data.lastPublicMessage(username));
							
						}else if(request.startsWith("!register ")){	//!register
							data.registerUser(request,username);
							
						}else if(request.startsWith("!lookup ")){	//!lookup
							writer.println(data.lookupAddress(request));
							
						}
					}				
				}
				checkForPendingMessages();
			} 
			catch (InterruptedIOException e){
				//System.out.println(e.toString());	
			}
			catch (IOException e) {
				//System.out.println(e.toString());				
			}
			catch (InterruptedException e){
				return;
			}
		}
	}
}
