package ar.edu.unsam.ddso.tp.dto;

import java.time.LocalDate;

public record PieceResponseDTO(Long id, String name, String description, LocalDate date) {}