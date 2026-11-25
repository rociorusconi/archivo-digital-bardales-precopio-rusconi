package ar.edu.unsam.ddso.tp.dto;

import java.time.LocalDate;
import java.util.Set;

public record PieceSearchRequestDTO(
        String name, String description, LocalDate dateFrom, LocalDate dateTo, Set<String> tags) {

    public PieceSearchRequestDTO {
        tags = tags == null ? null : Set.copyOf(tags);
    }
}
