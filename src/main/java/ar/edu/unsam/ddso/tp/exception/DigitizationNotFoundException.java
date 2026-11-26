package ar.edu.unsam.ddso.tp.exception;

public class DigitizationNotFoundException extends RuntimeException {

    public DigitizationNotFoundException(Long id) {
        super("Digitization not found with id: " + id);
    }
}
