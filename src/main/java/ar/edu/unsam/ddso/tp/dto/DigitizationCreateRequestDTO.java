package ar.edu.unsam.ddso.tp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

public record DigitizationCreateRequestDTO(
        @NotBlank(message = "must not be blank")
                @URL(message = "must be a valid URL")
                @Size(max = 255, message = "must have at most 255 characters")
                String url,
        @Size(max = 1000, message = "must have at most 1000 characters") String description) {}
