package com.example.restapi;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

@ApplicationPath("/api")  // Base path for all REST endpoints
public class RestApplication extends Application {
}
