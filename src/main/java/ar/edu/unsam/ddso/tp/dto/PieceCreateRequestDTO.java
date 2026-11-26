package ar.edu.unsam.ddso.tp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record PieceCreateRequestDTO(
        @NotBlank(message = "must not be blank")
                @Size(max = 255, message = "must have at most 255 characters")
                String name,
        @Size(max = 1000, message = "must have at most 1000 characters") String description,
        LocalDate date) {}
