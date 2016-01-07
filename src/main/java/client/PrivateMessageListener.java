package client;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.InvalidKeyException;
import java.security.Key;
import javax.crypto.Mac;


import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;

import util.Keys;

public class PrivateMessageListener implements Runnable {
	
	ServerSocket serverSocket;
	Socket socket;
	private BufferedReader reader;
	private PrintWriter writer;
	private ClientData data;
	
	public PrivateMessageListener(ServerSocket serverSocket, ClientData data){
		this.serverSocket = serverSocket;
		this.data = data;
	}
	
	public void run(){
		while(true){
			try{
				try{
					socket = serverSocket.accept();
				}catch(SocketException e){
					e.toString();
				}
				reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				writer = new PrintWriter(socket.getOutputStream(),true);
				
				String request;
				request = reader.readLine();
				System.out.println(request);
				
				String[] parts = request.split("!msg ");
				//byte[] inB64 = parts[0].getBytes();
				byte[] receivedHash = Base64.decode(parts[0]);
				
				
				Key secretKey = Keys.readSecretKey(new File("keys/hmac.key"));
				Mac hMac = null;
				try {
					hMac = Mac.getInstance("HmacSHA256");
				} catch (NoSuchAlgorithmException e1) {
					e1.printStackTrace();
				}
				try {
					hMac.init(secretKey);
				} catch (InvalidKeyException e) {
					e.printStackTrace();
				}
				byte[] computedHash = hMac.doFinal(parts[2].getBytes());
							
				boolean validHash = MessageDigest.isEqual(computedHash, receivedHash);
				
				if(validHash){
					writer.println("!ack");
				}else{
					System.out.println("Empfangene Message wurde manipuliert.");
					writer.println(new String(computedHash)+"!tampered "+parts[2]);
				}
				
				
				reader.close();
				writer.close();
				socket.close();
				
				System.out.println("yoyo");
				reader = null;
				writer = null;
				socket = null;
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
