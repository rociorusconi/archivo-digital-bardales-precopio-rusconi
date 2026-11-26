package ar.edu.unsam.ddso.tp.model.exceptions;

public class TagInUseException extends RuntimeException {

    public TagInUseException(Long id) {
        super("Tag is assigned to at least one digitization and cannot be deleted: " + id);
    }
}
