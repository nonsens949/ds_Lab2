package nameserver;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.rmi.AlreadyBoundException;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;

import nameserver.exceptions.AlreadyRegisteredException;
import nameserver.exceptions.InvalidDomainException;
import cli.Command;
import cli.Shell;
import chatserver.Chatserver;
import util.Config;

/**
 * Please note that this class is not needed for Lab 1, but will later be used
 * in Lab 2. Hence, you do not have to implement it for the first submission.
 */
public class Nameserver implements INameserverCli, Runnable {

	private String componentName;
	private Config config;
	private InputStream userRequestStream;
	private PrintStream userResponseStream;
	
	private Chatserver chatserver;
	private Registry registry;
	private NameserverStorage nameserverStorage;
	private NameserverRMI nameserverRMI;
	
	private Shell shell;
	

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
	public Nameserver(String componentName, Config config,
			InputStream userRequestStream, PrintStream userResponseStream) {
		this.componentName = componentName;
		this.config = config;
		this.userRequestStream = userRequestStream;
		this.userResponseStream = userResponseStream;
		
		this.nameserverStorage = new NameserverStorage();
		this.nameserverRMI = new NameserverRMI(nameserverStorage);
	}
	
	
	private boolean register(){
		boolean registered = config.listKeys().contains("domain");
		if(!registered){
			//register as root
			return registerAsRoot();
		}else{
			//register not as root
			try{
			registry = LocateRegistry.getRegistry(config.getString("registry.host"), config.getInt("registry.port"));
			INameserver rootNameserver = (INameserver) registry.lookup(config.getString("root_id"));
			INameserver nameserverRMIexported = (INameserver) UnicastRemoteObject.exportObject(nameserverRMI, 0);
			rootNameserver.registerNameserver(config.getString("domain"), nameserverRMIexported, nameserverRMIexported);
			return true;
			}catch(RemoteException ex){
				System.err.print("Initalizing registry threw error." + ex.getMessage());
				return false;
			}catch(NotBoundException em){
				System.err.print("Binding registry to chatserver threw error." + em.getMessage());
				return false;
			}catch(InvalidDomainException e){
				System.err.print("Binding registry to chatserver threw error." + e.getMessage());
				return false;
			}catch(AlreadyRegisteredException ek){
				System.err.print("Binding registry to chatserver threw error." + ek.getMessage());
				return false;
			}
		}
	}
	

	private boolean registerAsRoot(){
		try{
			registry = LocateRegistry.createRegistry(config.getInt("registry.port"));
			NameserverRMI nameserverRMIexported = (NameserverRMI) UnicastRemoteObject.exportObject(nameserverRMI,0);
			registry.bind(config.getString("root_id"), nameserverRMIexported);
			return true;
		}catch(RemoteException ex){
			System.err.print("Initalizing registry threw error." + ex.getMessage());
			return false;
		}catch(AlreadyBoundException em){
			System.err.print("Binding registry to chatserver threw error." + em.getMessage());
			return false;
		}
	}
	

	@Override
	public void run() {
		// TODO
		shell = new Shell(componentName, userRequestStream, userResponseStream);
		shell.register(this);
		
		boolean registered = register();
		if(registered){
			System.err.println("Error while registering nameserver");
		}
		
		new Thread(shell).start();
		System.out.println("Nameserver started");
	}

	@Command
	@Override
	public String nameservers() throws IOException {
		// TODO Auto-generated method stub
		LinkedList<String> registeredZones = new LinkedList<String>(nameserverStorage.getRegisteredZones());
		Collections.sort(registeredZones);
		
		String sortedZones = "";
		for (String zone : registeredZones) {
			sortedZones += "# " + zone + "\n";
		}

		return sortedZones;
	}

	@Command
	@Override
	public String addresses() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Command
	@Override
	public String exit() throws IOException {
		// TODO Auto-generated method stub
		try{
			//not forcing
			if(!UnicastRemoteObject.unexportObject(nameserverRMI, false)){
				//forcing
				if(!UnicastRemoteObject.unexportObject(nameserverRMI, true)){
					System.out.println("Unexporting remote object was forced.");
				}
			}
		}catch(NoSuchObjectException e){
			System.err.println("Unexporting remote object failed. " + e.getMessage());
		}
		
		shell.close();
		String closing = "Successfully shut down.";
		return closing;
	}

	/**
	 * @param args
	 *            the first argument is the name of the {@link Nameserver}
	 *            component
	 */
	public static void main(String[] args) {
		Nameserver nameserver = new Nameserver(args[0], new Config(args[0]),
				System.in, System.out);
		
		// TODO: start the nameserver
		Thread t = new Thread(nameserver);
		t.start();
	}

}
