package client;

public class ClientData {
	private String message;
	private boolean continueListening;
	
	public ClientData(){
		this.message="";
		continueListening = true;
	}
	public void stopListening(){
		continueListening = false;
	}
	public synchronized void setMessage(String message) {
		this.message=message;
	}
	public synchronized String getMessage(){
		return message;
	}
	public synchronized void resetMessage(){
		message="";
	}
}
