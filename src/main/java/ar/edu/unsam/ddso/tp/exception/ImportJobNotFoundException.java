package ar.edu.unsam.ddso.tp.exception;

public class ImportJobNotFoundException extends RuntimeException {

    public ImportJobNotFoundException(Long id) {
        super("Import not found with id: " + id);
    }
}
