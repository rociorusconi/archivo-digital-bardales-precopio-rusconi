package ar.edu.unsam.ddso.tp.exception;

public class TagNotFoundException extends RuntimeException {

    public TagNotFoundException(Long id) {
        super("Tag not found: " + id);
    }
}
