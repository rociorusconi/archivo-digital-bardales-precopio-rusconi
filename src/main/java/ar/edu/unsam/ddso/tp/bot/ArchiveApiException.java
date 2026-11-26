package ar.edu.unsam.ddso.tp.bot;

public class ArchiveApiException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int status;

    public ArchiveApiException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
