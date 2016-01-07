package chatserver;

public class Message {
	
	private String message;
	private String senderUsername;
	private int readBy;
	
	public Message(String message, String senderUsername){
		this.message = message;
		this.senderUsername = senderUsername;
		this.readBy = 0;
	}
	public synchronized void incrementReadBy(){
		readBy++;
	}
	public synchronized int Readby(){
		return readBy;
	}
	public String Message(){
		return message;
	}
	public String SenderUsername(){
		return senderUsername;
	}
}
