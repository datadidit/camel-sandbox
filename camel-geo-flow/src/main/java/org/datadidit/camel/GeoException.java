package org.datadidit.camel;

public class GeoException extends Exception{
	public GeoException() {
		
	}
	
	public GeoException(String message, Throwable error) {
		super(message, error);
	}
}
