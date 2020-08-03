package ca.anthonyting.personalplugins.exceptions;

public class PlayerNotFoundException extends Exception {
    public PlayerNotFoundException(String errorMessage) {
        super(errorMessage);
    }
}
