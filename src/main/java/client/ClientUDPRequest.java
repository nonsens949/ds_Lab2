package client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;

import util.Config;

public class ClientUDPRequest implements Runnable {

	DatagramSocket udpsocket;
	Config config;
	ClientData clientdata;
	String messageresponse;
	
	public ClientUDPRequest(ClientData clientdata,Config config){
		this.clientdata = clientdata;
		this.config = config;
		this.messageresponse = "";
	}
	
	public synchronized String readOutMessage(){
		return messageresponse;
	}
	
	public void run(){
		String response="No return message from udpMessage";
		try {
			// open a new DatagramSocket
			udpsocket = new DatagramSocket();
			byte[] buffer;
			DatagramPacket packet;
			String input = "!list";
			
			buffer = input.getBytes();
			if(buffer == null) System.out.println("Buffer ist null");
			InetAddress serveraddress = InetAddress.getByName(config.getString("chatserver.host"));
			if(serveraddress == null) System.out.println("InetAddress ist null");
			int port = config.getInt("chatserver.udp.port");

			
			packet = new DatagramPacket(buffer, buffer.length,
					serveraddress,port);
	
			udpsocket.send(packet);
	
			buffer = new byte[1024];

			packet = new DatagramPacket(buffer, buffer.length);

			udpsocket.receive(packet);
	
			response = new String(packet.getData());
			int actualresponselenght = packet.getLength();
			
			
			clientdata.setMessage(response.substring(0, actualresponselenght));
			//System.out.println("Clientdata: "+clientdata.getMessage());
			this.messageresponse = response.substring(0, actualresponselenght);
			
		}catch (UnknownHostException e) {
			System.out.println("Cannot connect to host: " + e.getMessage());
		} catch (IOException e) {
			System.out.println(e.getClass().getSimpleName() + ": "
					+ e.getMessage());
		} finally {
			if (udpsocket != null && !udpsocket.isClosed()){
				udpsocket.close();
			}
		}
	}
}
