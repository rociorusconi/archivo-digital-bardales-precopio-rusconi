package ar.edu.unsam.ddso.tp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.unsam.ddso.tp.dto.DigitizationCreateRequestDTO;
import ar.edu.unsam.ddso.tp.dto.DigitizationResponseDTO;
import ar.edu.unsam.ddso.tp.dto.PieceCreateRequestDTO;
import ar.edu.unsam.ddso.tp.dto.PieceDetailResponseDTO;
import ar.edu.unsam.ddso.tp.dto.PieceResponseDTO;
import ar.edu.unsam.ddso.tp.dto.TagResponseDTO;
import ar.edu.unsam.ddso.tp.exception.PieceNotFoundException;
import ar.edu.unsam.ddso.tp.model.Digitization;
import ar.edu.unsam.ddso.tp.model.Piece;
import ar.edu.unsam.ddso.tp.model.Tag;
import ar.edu.unsam.ddso.tp.repository.DigitizationRepository;
import ar.edu.unsam.ddso.tp.repository.PieceRepository;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PieceServiceTest {

  @Mock private PieceRepository pieceRepository;
  @Mock private DigitizationRepository digitizationRepository;

  private PieceService pieceService;

  @BeforeEach
  void setUp() {
    pieceService = new PieceService(pieceRepository, digitizationRepository);
  }

  @Test
  void createPieceSavesThePieceAndReturnsItsData() {
    // Given
    LocalDate date = LocalDate.of(1977, 5, 3);
    PieceCreateRequestDTO request =
        new PieceCreateRequestDTO("Letter from Cordoba", "Handwritten letter", date);
    when(pieceRepository.save(any(Piece.class)))
        .thenReturn(new Piece(1L, "Letter from Cordoba", "Handwritten letter", date));

    // When
    PieceResponseDTO response = pieceService.createPiece(request);

    // Then
    assertEquals(1L, response.id());
    assertEquals("Letter from Cordoba", response.name());
    assertEquals("Handwritten letter", response.description());
    assertEquals(date, response.date());
  }

  @Test
  void addDigitizationToExistingPieceReturnsDigitizationLinkedToThePiece() {
    // Given
    Piece piece = new Piece(5L, "Letter from Cordoba", "Handwritten letter", LocalDate.of(1977, 5, 3));
    DigitizationCreateRequestDTO request =
        new DigitizationCreateRequestDTO("https://files.example/letter-p1.pdf", "Scan of page 1");
    when(pieceRepository.findById(5L)).thenReturn(Optional.of(piece));
    when(digitizationRepository.save(any(Digitization.class)))
        .thenReturn(
            new Digitization(
                10L, "https://files.example/letter-p1.pdf", "Scan of page 1", piece, new HashSet<>()));

    // When
    DigitizationResponseDTO response = pieceService.addDigitization(5L, request);

    // Then
    assertEquals(10L, response.id());
    assertEquals("https://files.example/letter-p1.pdf", response.url());
    assertEquals("Scan of page 1", response.description());
    assertEquals(5L, response.pieceId());
  }

  @Test
  void getPieceReturnsThePieceWithItsDigitizationsAndTags() {
    // Given
    Piece piece = new Piece(5L, "Letter from Cordoba", "Handwritten letter", LocalDate.of(1977, 5, 3));
    Digitization scan =
        new Digitization(
            10L,
            "https://files.example/letter-p1.pdf",
            "Scan of page 1",
            piece,
            new HashSet<>(Set.of(new Tag(2L, "Press"), new Tag(1L, "Letters"))));
    when(pieceRepository.findById(5L)).thenReturn(Optional.of(piece));
    when(digitizationRepository.findByPieceIdOrderByIdAsc(5L)).thenReturn(List.of(scan));

    // When
    PieceDetailResponseDTO response = pieceService.getPiece(5L);

    // Then
    assertEquals(5L, response.id());
    assertEquals("Letter from Cordoba", response.name());
    assertEquals(1, response.digitizations().size());
    assertEquals("https://files.example/letter-p1.pdf", response.digitizations().get(0).url());
    assertEquals(
        List.of(new TagResponseDTO(1L, "Letters"), new TagResponseDTO(2L, "Press")),
        response.digitizations().get(0).tags());
  }

  @Test
  void getMissingPieceThrowsPieceNotFoundException() {
    // Given
    when(pieceRepository.findById(99L)).thenReturn(Optional.empty());

    // When / Then
    assertThrows(PieceNotFoundException.class, () -> pieceService.getPiece(99L));
    verify(digitizationRepository, never()).findByPieceIdOrderByIdAsc(any());
  }

  @Test
  void addDigitizationToMissingPieceThrowsPieceNotFoundException() {
    // Given
    DigitizationCreateRequestDTO request =
        new DigitizationCreateRequestDTO("https://files.example/letter-p1.pdf", "Scan of page 1");
    when(pieceRepository.findById(99L)).thenReturn(Optional.empty());

    // When / Then
    assertThrows(PieceNotFoundException.class, () -> pieceService.addDigitization(99L, request));
    verify(digitizationRepository, never()).save(any(Digitization.class));
  }
}