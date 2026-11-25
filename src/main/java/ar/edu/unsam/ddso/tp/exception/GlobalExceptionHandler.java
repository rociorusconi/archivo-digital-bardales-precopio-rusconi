package ar.edu.unsam.ddso.tp.exception;

import ar.edu.unsam.ddso.tp.dto.ErrorResponseDTO;
import ar.edu.unsam.ddso.tp.model.exceptions.DuplicateTagException;
import ar.edu.unsam.ddso.tp.model.exceptions.InvalidTagNameException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({PieceNotFoundException.class, TagNotFoundException.class})
    public ResponseEntity<ErrorResponseDTO> handleNotFound(RuntimeException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex);
    }

    @ExceptionHandler(DuplicateTagException.class)
    public ResponseEntity<ErrorResponseDTO> handleConflict(DuplicateTagException ex) {
        return buildResponse(HttpStatus.CONFLICT, ex);
    }

    @ExceptionHandler(InvalidTagNameException.class)
    public ResponseEntity<ErrorResponseDTO> handleBadRequest(InvalidTagNameException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex);
    }

    private ResponseEntity<ErrorResponseDTO> buildResponse(HttpStatus status, RuntimeException ex) {
        return ResponseEntity.status(status)
                .body(new ErrorResponseDTO(status.value(), ex.getMessage()));
    }
}