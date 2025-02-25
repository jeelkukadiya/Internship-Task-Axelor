package com.example.restapi;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/hello") // API endpoint
public class HelloResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String sayHello() {
        return "{\"message\": \"Hello, RESTEasy!\"}";
    }
}
