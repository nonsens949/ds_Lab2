package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

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
				writer.println("!ack");
				
				reader.close();
				writer.close();
				socket.close();
				
				reader = null;
				writer = null;
				socket = null;
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
