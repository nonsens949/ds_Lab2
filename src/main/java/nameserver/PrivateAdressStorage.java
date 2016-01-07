package nameserver;

import objects.PrivateAdress;
import java.util.HashMap;

public class PrivateAdressStorage {
	
	private HashMap<String, PrivateAdress> privateAdressStorage;
	
	public PrivateAdressStorage(){
		this.privateAdressStorage = new HashMap<String,PrivateAdress>();
	}
	
	public PrivateAdress getPrivateAdress(String user){
		return privateAdressStorage.get(user);
	}
	
	public boolean containsPrivateAdress(String user){
		return privateAdressStorage.containsKey(user);
	}
	
	public void addPrivateAdress(String user, PrivateAdress privateAdress){
		privateAdressStorage.put(user,privateAdress);
	}
	
	public void removePrivateAdress(String user){
		privateAdressStorage.remove(user);
	}
	
	public HashMap<String, PrivateAdress> getAll(){
		return new HashMap<>(privateAdressStorage);
	}
	
	

}
