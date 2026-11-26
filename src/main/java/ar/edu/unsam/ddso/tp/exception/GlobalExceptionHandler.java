package ar.edu.unsam.ddso.tp.exception;

import ar.edu.unsam.ddso.tp.dto.ErrorResponseDTO;
import ar.edu.unsam.ddso.tp.model.exceptions.DuplicateTagException;
import ar.edu.unsam.ddso.tp.model.exceptions.InvalidDateRangeException;
import ar.edu.unsam.ddso.tp.model.exceptions.InvalidTagNameException;
import ar.edu.unsam.ddso.tp.model.exceptions.TagInUseException;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({
        PieceNotFoundException.class,
        TagNotFoundException.class,
        DigitizationNotFoundException.class
    })
    public ResponseEntity<ErrorResponseDTO> handleNotFound(RuntimeException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex);
    }

    @ExceptionHandler({DuplicateTagException.class, TagInUseException.class})
    public ResponseEntity<ErrorResponseDTO> handleConflict(RuntimeException ex) {
        return buildResponse(HttpStatus.CONFLICT, ex);
    }

    @ExceptionHandler({InvalidTagNameException.class, InvalidDateRangeException.class})
    public ResponseEntity<ErrorResponseDTO> handleBadRequest(RuntimeException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex);
    }

    // Fields that fail the DTO validations (@Valid), e.g. "name: must not be blank".
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleInvalidRequest(MethodArgumentNotValidException ex) {
        String message =
                ex.getBindingResult().getFieldErrors().stream()
                        .map(error -> error.getField() + ": " + error.getDefaultMessage())
                        .sorted()
                        .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest()
                .body(new ErrorResponseDTO(HttpStatus.BAD_REQUEST.value(), message));
    }

    private ResponseEntity<ErrorResponseDTO> buildResponse(HttpStatus status, RuntimeException ex) {
        return ResponseEntity.status(status)
                .body(new ErrorResponseDTO(status.value(), ex.getMessage()));
    }
}