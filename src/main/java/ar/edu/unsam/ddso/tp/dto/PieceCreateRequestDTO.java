package ar.edu.unsam.ddso.tp.dto;

import java.time.LocalDate;

public record PieceCreateRequestDTO(String name, String description, LocalDate date) {}