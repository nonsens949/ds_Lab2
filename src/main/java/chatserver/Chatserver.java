package chatserver;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.SocketException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.Executor;

import util.Config;

import java.util.Set;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.List;

import nameserver.INameserver;
import cli.Command;
import cli.Shell;

public class Chatserver implements IChatserverCli, Runnable {

	private String componentName;
	private Config config;
	private InputStream userRequestStream;
	private PrintStream userResponseStream;
	private DatagramSocket datagramSocket;
	private ChatserverData data;
	private ServerSocket serverSocket;

	private Executor TCPConnectionExecutor;
	private Executor UDPConnectionExecutor;
	private List<Thread> threads;
	
	//Lab2
	private Registry registry;
	private INameserver iNameserver;

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
	public Chatserver(String componentName, Config config,
			InputStream userRequestStream, PrintStream userResponseStream) {
		this.componentName = componentName;
		this.config = config;
		this.userRequestStream = userRequestStream;
		this.userResponseStream = userResponseStream;
		this.data = new ChatserverData();
		this.threads = null;
		
	}
	
	
	private boolean connectToRootNameServer(){
		try{
			registry = LocateRegistry.getRegistry(config.getString("registry.host"),config.getInt("registry.port"));
			iNameserver = (INameserver) registry.lookup(config.getString("root_id"));
			
		}catch(RemoteException e){
			System.err.println("Connection to nameserver failed. " + e.getMessage());
		}catch(NotBoundException ex){
			System.err.println("Connection to nameserver failed. " + ex.getMessage());
		}
		return true;
	}

	@Override
	public void run() {
		Shell shell = new Shell(componentName, userRequestStream, userResponseStream);

		shell.register(this);

		//Creating TCP socket for client connections
		try
		{				
			serverSocket = new ServerSocket(config.getInt("tcp.port"));
			// incoming connections get a separate thread
			for(int i = 0; i<data.getNumberOfUsers(); i++){
				//TCPConnectionExecutor.execute(new TCPConnection(serverSocket, data));
				//Thread thread = new Thread(new TCPConnection(serverSocket, data));				
				//thread.start();
				//threads.add(thread);
				Thread thread;
				TCPConnection connection = new TCPConnection(serverSocket, data);
				data.addThread(connection);	
				thread = new Thread(connection);			
				thread.start();
			}
		} 
		catch (Exception e) {			
			throw new RuntimeException("Cannot listen on TCP port. "+e.getMessage() , e);
		}
		
		//Creating Datagram socket for UDP requests		
		try {
			datagramSocket = new DatagramSocket(config.getInt("udp.port"));
			new Thread(new UDPRequest(datagramSocket, data)).start();
		} 
		catch (IOException e) {
			throw new RuntimeException("Cannot listen on UDP port.", e);
		}
		
		//connect to root nameserver
		if(!connectToRootNameServer()){
			System.err.println("Could not connect to root nameserver");
		}
		
		new Thread(shell).start();
		System.out.println(getClass().getName()+ " up and waiting for commands!");
	}

	public void closeUDPSocket() {
		if (datagramSocket != null)
			datagramSocket.close();
	}
	
	@Command
	@Override
	public synchronized String users() throws IOException {
		return this.data.formattedUserLoginStatuses();
	}
	
	@Command
	@Override
	public synchronized String exit() throws IOException{
		data.setCloseFlag();
		closeUDPSocket();
		try{
			if(!serverSocket.isClosed())
				//System.out.println("Serversocket isn't closed");
				serverSocket.close();
		}catch(SocketException e){
			//System.out.println(e.toString());
		}
		
		return componentName+" closed";
	}

	/**
	 * @param args
	 *            the first argument is the name of the {@link Chatserver}
	 *            component
	 */
	public static void main(String[] args) {
		IChatserverCli chatserver = new Chatserver(args[0],new Config("chatserver"), System.in, System.out);

		(new Thread((Chatserver)chatserver)).start();
	}

}
