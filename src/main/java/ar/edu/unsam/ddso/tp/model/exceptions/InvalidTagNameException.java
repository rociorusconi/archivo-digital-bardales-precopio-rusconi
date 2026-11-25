package ar.edu.unsam.ddso.tp.model.exceptions;

public class InvalidTagNameException extends RuntimeException {

    public InvalidTagNameException() {
        super("Tag name must not be empty");
    }
}
