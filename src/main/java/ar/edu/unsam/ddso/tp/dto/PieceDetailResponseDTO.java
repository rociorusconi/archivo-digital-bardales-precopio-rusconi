package ar.edu.unsam.ddso.tp.dto;

import java.time.LocalDate;
import java.util.List;

public record PieceDetailResponseDTO(
    Long id,
    String name,
    String description,
    LocalDate date,
    List<DigitizationResponseDTO> digitizations) {

  public PieceDetailResponseDTO {
    digitizations = List.copyOf(digitizations);
  }
}
