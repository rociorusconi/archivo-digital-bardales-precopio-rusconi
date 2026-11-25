package ar.edu.unsam.ddso.tp.dto;

import java.util.List;

public record DigitizationResponseDTO(
    Long id, String url, String description, Long pieceId, List<TagResponseDTO> tags) {

  public DigitizationResponseDTO {
    tags = List.copyOf(tags);
  }
}
