package chatserver;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import chatserver.ChatserverData;

public class UDPRequest implements Runnable {

	private DatagramSocket datagramSocket;
	private ChatserverData data;

	public UDPRequest(DatagramSocket datagramSocket, ChatserverData data) {
		this.datagramSocket = datagramSocket;
		this.data = data;
	}

	public void run() {

		byte[] buffer;
		DatagramPacket packet;
		try {
			while (true) {
				buffer = new byte[1024];
				// create a datagram packet of specified length (buffer.length)
				/*
				 * Keep in mind that: in UDP, packet delivery is not
				 * guaranteed,and the order of the delivery/processing is not
				 * guaranteed
				 */
				packet = new DatagramPacket(buffer, buffer.length);

				// wait for incoming packets from client
				datagramSocket.receive(packet);
				// get the data from the packet
				String request = new String(packet.getData());
				
				if(!request.startsWith("!list")){
					buffer = null;
					packet = null;
					continue;
				}
				List<String> onlineUsers = new ArrayList<String>();
				String allUsers = data.formattedUserLoginStatuses();
				String[] singleUsers = allUsers.split("\\n");
				for(String s : singleUsers){
					if(s != null){
						if(s.contains("online")){
							onlineUsers.add(s.substring(0,s.length()-10));
							//System.out.println(s);
						}
					}
				}
				java.util.Collections.sort(onlineUsers);
				String orderedUsers="Online users:\n";
				for(String s : onlineUsers){
					orderedUsers += s+"\n";
				}
				//System.out.println("Debug output: "+orderedUsers);
				InetAddress address = packet.getAddress();
				// get the port of the sender from the received packet
				int port = packet.getPort();
				buffer = orderedUsers.getBytes();
				/*
				 * create a new datagram packet, and write the response bytes,
				 * at specified address and port. the packet contains all the
				 * needed information for routing.
				 */
				packet = new DatagramPacket(buffer, buffer.length, address,
						port);
				// finally send the packet
				datagramSocket.send(packet);
				
				 
			} 
		}catch (IOException e) {
			System.out.println(e.getMessage());
		} finally {
			if (datagramSocket != null && !datagramSocket.isClosed())
				datagramSocket.close();
		}
	}
}
