package objects;

public class PrivateAdress {
	
	private String name;
	private int port;
	
	public PrivateAdress(){
		this.name = "";
		this.port = 0;
	}

	@Override
	public String toString() {
		return "" + name + " " + port;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

}
