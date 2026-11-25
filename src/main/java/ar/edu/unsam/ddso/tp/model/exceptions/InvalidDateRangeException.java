package ar.edu.unsam.ddso.tp.model.exceptions;

import java.time.LocalDate;

public class InvalidDateRangeException extends RuntimeException {

    public InvalidDateRangeException(LocalDate dateFrom, LocalDate dateTo) {
        super("Invalid date range: dateFrom " + dateFrom + " is after dateTo " + dateTo);
    }
}
