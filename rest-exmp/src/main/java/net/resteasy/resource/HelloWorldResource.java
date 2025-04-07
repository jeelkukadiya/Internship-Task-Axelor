package net.resteasy.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import net.resteasy.model.HelloWorld;

@Path("hello")
public class HelloWorldResource {
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response helloWorld() {
		HelloWorld helloworld = new HelloWorld("Hello World!");
		return Response.ok(helloworld).build();
	}

}
