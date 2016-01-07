package nameserver;

import java.util.Collection;
import java.util.HashMap;

public class NameserverStorage {
	
	private HashMap<String, INameserver> nameserverStorage = new HashMap<String,INameserver>();
	
	
	public synchronized boolean addNameserver(String zone, INameserver iNameserver){
		zone = zone.toLowerCase();
		
		//if nameserver already exists --> do not add again
		if(nameserverStorage.containsKey(zone)){
			return false;
		}else{
			nameserverStorage.put(zone, iNameserver);
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

}
