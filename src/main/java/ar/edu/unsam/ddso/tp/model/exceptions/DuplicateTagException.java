package ar.edu.unsam.ddso.tp.model.exceptions;

public class DuplicateTagException extends RuntimeException {

    public DuplicateTagException(String name) {
        super("Tag already exists: " + name);
    }
}
