package ar.edu.unsam.ddso.tp.service;

import ar.edu.unsam.ddso.tp.dto.DigitizationResponseDTO;
import ar.edu.unsam.ddso.tp.dto.TagResponseDTO;
import ar.edu.unsam.ddso.tp.model.Digitization;
import java.util.Comparator;

// Single place where a Digitization is mapped to its response DTO (used by PieceService and
// DigitizationService). It stays in the service layer, which is responsible for mapping to DTOs.
final class DigitizationMapper {

    private DigitizationMapper() {}

    static DigitizationResponseDTO toResponse(Digitization digitization) {
        return new DigitizationResponseDTO(
                digitization.getId(),
                digitization.getUrl(),
                digitization.getDescription(),
                digitization.getPiece().getId(),
                digitization.getTags().stream()
                        .map(tag -> new TagResponseDTO(tag.getId(), tag.getName()))
                        .sorted(Comparator.comparing(TagResponseDTO::name))
                        .toList());
    }
}
