package nameserver;

import java.rmi.RemoteException;

import objects.Domain;
import objects.PrivateAdress;
import objects.PrivateAdressStorage;
import nameserver.exceptions.AlreadyRegisteredException;
import nameserver.exceptions.InvalidDomainException;

public class NameserverRMI implements INameserver{
	private NameserverStorage nameserverStorage;
	private PrivateAdressStorage privateAdressStorage;
	
	public NameserverRMI(NameserverStorage nameserverStorage, PrivateAdressStorage privateAdressStorage ){
		this.nameserverStorage = nameserverStorage;
		this.privateAdressStorage = privateAdressStorage;
	}

	@Override
	public void registerUser(String username, String address)
			throws RemoteException, AlreadyRegisteredException,
			InvalidDomainException {
		// TODO Auto-generated method stub
		
		Domain d = new Domain(username);
		if(d.hasSubDomain()){
			//weiterleiten, wenn noch domain vorhanden
			String zone = d.getRootDomain();
			if(nameserverStorage.containsNameserver(zone)){
				nameserverStorage.getNameserver(zone).registerUser(d.getSubDomain().toString(),address);
			}
		}else if (nameserverStorage.containsNameserver(d.toString())){
			//keine weitere domain, aber schon vorhanden
			throw new AlreadyRegisteredException("User: " + username + "has already been registered with the private adress: " + address);
		}else{
			//wird hinzugefügt
			PrivateAdress pA = new PrivateAdress(address);
			privateAdressStorage.addPrivateAdress(d.toString(), pA);
		}	
	}

	@Override
	public INameserverForChatserver getNameserver(String zone)
			throws RemoteException {
		// TODO Auto-generated method stub
		return nameserverStorage.getNameserverForChatserver(zone);
	}

	@Override
	public String lookup(String username) throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void registerNameserver(String domain, INameserver nameserver,
			INameserverForChatserver nameserverForChatserver)
			throws RemoteException, AlreadyRegisteredException,
			InvalidDomainException {
		// TODO Auto-generated method stub
		
		Domain d = new Domain(domain);
		
		if(d.hasSubDomain()){
			//weiterleiten, wenn noch domain vorhanden
			String zone = d.getRootDomain();
			if(nameserverStorage.containsNameserver(zone)){
				nameserverStorage.getNameserver(zone).registerNameserver(d.getSubDomain().toString(), nameserver, nameserverForChatserver);
			}
		}else if (nameserverStorage.containsNameserver(d.toString())){
			//keine weitere domain, aber schon vorhanden
			throw new AlreadyRegisteredException("Domain: " + domain + " already registered." );
		}else{
			//wird hinzugefügt
			nameserverStorage.addNameserver(d.toString(), nameserver,nameserverForChatserver);
		}
	}
	
	

}
