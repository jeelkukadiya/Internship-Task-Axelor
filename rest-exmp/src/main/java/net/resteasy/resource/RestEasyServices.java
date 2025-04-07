package net.resteasy.resource;

import java.util.HashSet;
import java.util.Set;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;


@ApplicationPath("/restapi")
public class RestEasyServices extends Application{
	
	private Set<Object> singletons = new HashSet<>();
	
	public RestEasyServices(){
		this.singletons.add(new HelloWorldResource());
	}

	@Override
	public Set<Object> getSingletons() {
		return singletons;
	}

}
