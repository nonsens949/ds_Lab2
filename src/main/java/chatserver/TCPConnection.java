package chatserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.InterruptedException;
import java.io.File;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.InetSocketAddress;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import org.bouncycastle.util.encoders.Base64;

import util.Config;
import util.Keys;

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

	private Key key;
	private boolean authenticated;
	public static final String ALGORITHM = "RSA/NONE/OAEPWithSHA256AndMGF1Padding";
	public static final String AESALGORITHM = "AES/CTR/NoPadding";
	private IvParameterSpec iv = null;
	private SecretKey secretKey = null;
	private Config config;
	
	public TCPConnection(ServerSocket serverSocket, ChatserverData data, Config config) {
		this.serverSocket = serverSocket;
		this.data = data;
		this.loggedIn = false;
		this.username = "";
		this.password = "";
		this.socket = null;
		this.address = null;
		this.reader = null;
		this.writer = null;
		this.config = config;
		try {
			File pemFile = new File(config.getString("key"));
			key = Keys.readPrivatePEM(pemFile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
	
	private String authenticate(String encryptedCmd64, BufferedReader reader,
			PrintWriter writer) {
		byte[] encryptedCmd = Base64.decode(encryptedCmd64.getBytes());
		String cmd = "";
		Cipher cipher;
		try {
			cipher = Cipher.getInstance(ALGORITHM);
			cipher.init(Cipher.DECRYPT_MODE, key);
			cmd = new String(Base64.encode(cipher.doFinal(encryptedCmd)));
		} catch (NoSuchAlgorithmException | NoSuchPaddingException
				| InvalidKeyException | IllegalBlockSizeException
				| BadPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (cmd.contains("!authenticate")) {
			String[] splittedCmd = cmd.split(" ");
			String userName = splittedCmd[1];
			String clientChallenge64 = splittedCmd[2];
			SecureRandom secureRandom = new SecureRandom();
			final byte[] serverChallenge = new byte[32];
			secureRandom.nextBytes(serverChallenge);
			String controllerChallenge64 = new String(
					Base64.encode(serverChallenge));

			KeyGenerator generator;
			try {
				generator = KeyGenerator.getInstance("AES");
				generator.init(256);
				secretKey = generator.generateKey();
				String secretKey64 = new String(
						encryptBase64(secretKey.getEncoded()));

				SecureRandom random = new SecureRandom();
				byte[] ivBytes = new byte[16];
				random.nextBytes(ivBytes);
				iv = new IvParameterSpec(ivBytes);
				String iv64 = new String(encryptBase64(iv.getIV()));
				
				File pemFile = new File(config.getString("keys.dir")+userName);
				Key publicKey = Keys.readPublicPEM(pemFile);

				cipher = Cipher.getInstance(ALGORITHM);
				cipher.init(Cipher.ENCRYPT_MODE, publicKey);
				byte[] msgToSend = ("!ok " + clientChallenge64 + " "
						+ controllerChallenge64 + " " + secretKey64 + " " + iv64)
						.getBytes();
				String controllerResponse64 = new String(Base64.encode(cipher
						.doFinal(msgToSend)));
				
				// send second message: !ok <client-challenge>
				// <controller-challenge> <secret-key> <iv-parameter>
				writer.println(controllerResponse64);
				
				// receive third message: <controller-challenge>
				String clientResponse64AES64 = reader.readLine();
				
				cipher = Cipher.getInstance(AESALGORITHM);
				cipher.init(Cipher.DECRYPT_MODE, secretKey, iv);
				String clientResponse64 = new String(Base64.encode(cipher.doFinal(clientResponse64AES64.getBytes())));

				if (clientResponse64.equals(controllerChallenge64)) {
					return "success";
				} else {
					return "fail";
				}

			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvalidKeyException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchPaddingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalBlockSizeException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (BadPaddingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvalidAlgorithmParameterException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			return "no !authenticate command found";
		}
		return "fail";

	}
	
	private byte[] encryptBase64(byte[] msg) {
		byte[] base64Msg = Base64.encode(msg);

		return base64Msg;
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
						
						if (!authenticated) {
							// receive first message: !authenticate <user>
							// <client-challenge>
							String auth = authenticate(request, reader, writer);
							if (auth.equals("success")) {
								authenticated = true;
								continue;
							}
						}
						
//						if(!loggedIn){								//!login
//							if(!login(request)) continue;
//						}
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
