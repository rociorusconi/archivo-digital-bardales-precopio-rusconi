package ar.edu.unsam.ddso.tp.exception;

public class PieceNotFoundException extends RuntimeException {

  public PieceNotFoundException(Long id) {
    super("Piece not found with id: " + id);
  }
}