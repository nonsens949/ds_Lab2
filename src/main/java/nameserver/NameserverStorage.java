package nameserver;

import java.util.Collection;
import java.util.HashMap;

public class NameserverStorage {
	
	private HashMap<String, INameserver> nameserverStorage = new HashMap<String,INameserver>();
	private HashMap<String, INameserverForChatserver> nameserverForChatserverStorage = new HashMap<String, INameserverForChatserver>();
	
	public synchronized boolean addNameserver(String zone, INameserver iNameserver,INameserverForChatserver nameserverForChatserver ){
		zone = zone.toLowerCase();
		
		//if nameserver already exists --> do not add again
		if(nameserverStorage.containsKey(zone)){
			return false;
		}else{
			nameserverStorage.put(zone, iNameserver);
			nameserverForChatserverStorage.put(zone, nameserverForChatserver);
			return true;
		}
	}
	
	public Collection<String> getRegisteredZones(){
		return nameserverStorage.keySet();
	}
	
	public INameserver getNameserver(String zone){
		zone = zone.toLowerCase();
		return nameserverStorage.get(zone);
	}
	
	public boolean containsNameserver(String zone){
		zone = zone.toLowerCase();
		return nameserverStorage.containsKey(zone);
	}
	
	public INameserverForChatserver getNameserverForChatserver(String zone) {
        return nameserverForChatserverStorage.get(zone.toLowerCase());
    }

}
