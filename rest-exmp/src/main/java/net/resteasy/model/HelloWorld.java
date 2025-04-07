package net.resteasy.model;

import jakarta.xml.bind.annotation.XmlRootElement;


@XmlRootElement
public class HelloWorld {
	private String message;

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public HelloWorld(String message) {
		super();
		this.message = message;
	}
	
	
	

}
