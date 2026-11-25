package ar.edu.unsam.ddso.tp.service;

import ar.edu.unsam.ddso.tp.dto.DigitizationCreateRequestDTO;
import ar.edu.unsam.ddso.tp.dto.DigitizationResponseDTO;
import ar.edu.unsam.ddso.tp.dto.PieceCreateRequestDTO;
import ar.edu.unsam.ddso.tp.dto.PieceResponseDTO;
import ar.edu.unsam.ddso.tp.dto.TagResponseDTO;
import ar.edu.unsam.ddso.tp.exception.PieceNotFoundException;
import ar.edu.unsam.ddso.tp.model.Digitization;
import ar.edu.unsam.ddso.tp.model.Piece;
import ar.edu.unsam.ddso.tp.repository.DigitizationRepository;
import ar.edu.unsam.ddso.tp.repository.PieceRepository;
import java.util.Comparator;
import java.util.HashSet;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PieceService {

  private final PieceRepository pieceRepository;
  private final DigitizationRepository digitizationRepository;

  public PieceService(
      PieceRepository pieceRepository, DigitizationRepository digitizationRepository) {
    this.pieceRepository = pieceRepository;
    this.digitizationRepository = digitizationRepository;
  }

  // ---------- Operations ----------

  @Transactional
  public PieceResponseDTO createPiece(PieceCreateRequestDTO request) {
    Piece piece = new Piece(null, request.name(), request.description(), request.date());
    Piece saved = pieceRepository.save(piece);
    return toResponse(saved);
  }

  @Transactional
  public DigitizationResponseDTO addDigitization(
      Long pieceId, DigitizationCreateRequestDTO request) {
    Piece piece =
        pieceRepository.findById(pieceId).orElseThrow(() -> new PieceNotFoundException(pieceId));
    Digitization digitization =
        new Digitization(null, request.url(), request.description(), piece, new HashSet<>());
    Digitization saved = digitizationRepository.save(digitization);
    return toResponse(saved);
  }

  // ---------- Private ----------

  private PieceResponseDTO toResponse(Piece piece) {
    return new PieceResponseDTO(
        piece.getId(), piece.getName(), piece.getDescription(), piece.getDate());
  }

  private DigitizationResponseDTO toResponse(Digitization digitization) {
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