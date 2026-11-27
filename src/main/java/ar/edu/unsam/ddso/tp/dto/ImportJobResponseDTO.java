package ar.edu.unsam.ddso.tp.dto;

import ar.edu.unsam.ddso.tp.model.enums.ImportStatus;
import java.time.LocalDateTime;
import java.util.List;

public record ImportJobResponseDTO(
        Long id,
        String fileName,
        ImportStatus status,
        int totalRows,
        int processedRows,
        int createdPieces,
        int createdDigitizations,
        List<ImportErrorDTO> errors,
        String failureMessage,
        LocalDateTime createdAt,
        LocalDateTime finishedAt) {

    public ImportJobResponseDTO {
        errors = List.copyOf(errors);
    }
}
