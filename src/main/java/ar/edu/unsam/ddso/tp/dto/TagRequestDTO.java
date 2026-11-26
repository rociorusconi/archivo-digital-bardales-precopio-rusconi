package ar.edu.unsam.ddso.tp.dto;

import jakarta.validation.constraints.Size;

public record TagRequestDTO(@Size(max = 255, message = "must have at most 255 characters") String name) {}
