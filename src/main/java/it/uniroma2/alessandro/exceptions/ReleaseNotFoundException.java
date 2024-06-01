package it.uniroma2.alessandro.exceptions;

public class ReleaseNotFoundException extends Exception{
    public ReleaseNotFoundException() {
        super();
    }

    public ReleaseNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public ReleaseNotFoundException(String message) {
        super(message);
    }
}
