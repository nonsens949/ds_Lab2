package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import cli.Shell;
import util.Config;
import cli.Command;
import cli.Shell;

public class Client implements IClientCli, Runnable {

	private String componentName;
	private Config config;
	private Config usersConfig;
	private InputStream userRequestStream;
	private PrintStream userResponseStream;
	BufferedReader serverReader;
	PrintWriter serverWriter;
	Shell shell;
	Socket socket;
	DatagramSocket udpsocket;
	ServerSocket privateServerSocket;
	ClientData data;
	/**
	 * @param componentName
	 *            the name of the component - represented in the prompt
	 * @param config
	 *            the configuration to use
	 * @param userRequestStream
	 *            the input stream to read user input from
	 * @param userResponseStream
	 *            the output stream to write the console output to
	 */
	public Client(String componentName, Config config, InputStream userRequestStream, PrintStream userResponseStream) {
		this.componentName = componentName;
		this.config = config;
		this.userRequestStream = userRequestStream;
		this.userResponseStream = userResponseStream;
		this.usersConfig = new Config("user");
		this.data = new ClientData();
		

		socket = null;
		udpsocket = null;
		BufferedReader userInputReader = null;
	
		try {
			socket = new Socket(config.getString("chatserver.host"), config.getInt("chatserver.tcp.port"));
			serverReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			serverWriter = new PrintWriter(socket.getOutputStream(), true);
			


		} catch (UnknownHostException e) {
			System.out.println("Cannot connect to host: " + e.getMessage());
		} catch (IOException e) {
			System.out.println(e.getClass().getSimpleName() + ": "
					+ e.getMessage());
		}
	}

	@Override
	public void run() {
	
		shell = new Shell(componentName, userRequestStream, userResponseStream);
		shell.register(this);
		
		new Thread(shell).start();
		System.out.println(getClass().getName()+ " up and waiting for commands!");	
		
		/*try {
			
		serverWriter.println("!login bill.de 23456");
		System.out.println(serverReader.readLine());		
		serverWriter.println("!send testmessage1");
		System.out.println(serverReader.readLine());
		serverWriter.println("!send testmessage2");
		System.out.println(serverReader.readLine());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
	}
	
	@Command
	@Override
	public String login(String username, String password) throws IOException {
		serverWriter.println("!login "+username+" "+password);
		return serverReader.readLine();
		
	}
	@Command
	@Override
	public String logout() throws IOException {
		serverWriter.println("!logout");
		return serverReader.readLine();
	}
	@Command
	@Override
	public String send(String message) throws IOException {
		serverWriter.println("!send "+message);
		return "Message sent";
	}
	@Command
	@Override
	public String list() throws IOException {
		ClientData clientdata = new ClientData();
		ClientUDPRequest t = new ClientUDPRequest(clientdata,config);
		Thread tt = new Thread(t);
		tt.start();
		while(tt.isAlive()){}
		String message = clientdata.getMessage();
		data.resetMessage();
		return message;
		
	}
	@Command
	@Override
	public String msg(String username, String message) throws IOException {
		
		String address = lookup(username);
		if(address.contains("Wrong username"))
			return address;
		//return "Message could've been sent";
		//System.out.println("Adress: "+address);
		String[] temp = address.split(":");
		String ipp = temp[0];
		Integer portt = Integer.parseInt(temp[1]);
		Socket privatesocket = new Socket(ipp,(int)portt);
		BufferedReader privatereader = new BufferedReader(new InputStreamReader(privatesocket.getInputStream()));
		PrintWriter privatewriter = new PrintWriter(privatesocket.getOutputStream(), true);
		
		privatewriter.println("("+username+"): "+message);
		System.out.println(privatereader.readLine());
		return "Private Message to "+username+" sent.";
	}
	@Command
	@Override
	public String lookup(String username) throws IOException {
		serverWriter.println("!lookup "+username);
		String response = serverReader.readLine();
		return response;
	}
	@Command
	@Override
	public String register(String privateAddress) throws IOException {
		serverWriter.println("!register "+privateAddress);
		
		String[] temp = privateAddress.split(":");
		String ip = temp[0];
		int port = Integer.parseInt(temp[1]);
		privateServerSocket = new ServerSocket(port);
		new Thread(new PrivateMessageListener(privateServerSocket,data)).start();
		
		return "Registration was successful.";
	}
	@Command
	@Override
	public String lastMsg() throws IOException {
		serverWriter.println("!lastMsg");
		return serverReader.readLine();
	}
	@Command
	@Override
	public String exit() throws IOException {
		serverReader.close();
		serverWriter.close();
		socket.close();
		shell.close();
		return componentName+" closed";
	}

	/**
	 * @param args
	 *            the first argument is the name of the {@link Client} component
	 */
	public static void main(String[] args) {
		Client client = new Client(args[0], new Config("client"), System.in,
				System.out);
		new Thread(client).start();
	}

	// --- Commands needed for Lab 2. Please note that you do not have to
	// implement them for the first submission. ---
	@Command
	@Override
	public String authenticate(String username) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

}
