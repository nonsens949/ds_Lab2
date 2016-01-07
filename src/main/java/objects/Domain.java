package objects;

public class Domain {
	
	private String domain;
	
	public Domain(String domain){
		this.domain = domain;
	}
	
	public boolean hasSubDomain(){
		return domain.contains(".");
	}
	
	public String getRootDomain(){
		return domain.substring(domain.lastIndexOf(".")+1);
	}
	
	public Domain getSubDomain(){
		return new Domain(domain.substring(0,domain.lastIndexOf(".")));
	}
	

}
