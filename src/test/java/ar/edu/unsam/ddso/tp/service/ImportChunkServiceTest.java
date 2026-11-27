package ar.edu.unsam.ddso.tp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.unsam.ddso.tp.dto.DigitizationCreateRequestDTO;
import ar.edu.unsam.ddso.tp.dto.DigitizationResponseDTO;
import ar.edu.unsam.ddso.tp.dto.PieceCreateRequestDTO;
import ar.edu.unsam.ddso.tp.dto.PieceResponseDTO;
import ar.edu.unsam.ddso.tp.model.ImportJob;
import ar.edu.unsam.ddso.tp.model.Piece;
import ar.edu.unsam.ddso.tp.model.Tag;
import ar.edu.unsam.ddso.tp.repository.DigitizationRepository;
import ar.edu.unsam.ddso.tp.repository.ImportJobRepository;
import ar.edu.unsam.ddso.tp.repository.PieceRepository;
import ar.edu.unsam.ddso.tp.repository.TagRepository;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ImportChunkServiceTest {

    private static final LocalDate DATE = LocalDate.of(1979, 2, 11);
    private static final String URL = "https://files.example/rosario-p1.pdf";

    @Mock private ImportJobRepository importJobRepository;
    @Mock private PieceRepository pieceRepository;
    @Mock private TagRepository tagRepository;
    @Mock private DigitizationRepository digitizationRepository;
    @Mock private PieceService pieceService;
    @Mock private DigitizationService digitizationService;

    // The real validator, so the rows are checked with the same annotations as the API DTOs.
    private ValidatorFactory validatorFactory;
    private ImportChunkService importChunkService;
    private ImportJob job;

    @BeforeEach
    void setUp() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        importChunkService = new ImportChunkService(
                importJobRepository, pieceRepository, tagRepository, digitizationRepository,
                pieceService, digitizationService, validatorFactory.getValidator());
        job = ImportJob.start("pieces.csv", 1);
        when(importJobRepository.findById(1L)).thenReturn(Optional.of(job));
    }

    @AfterEach
    void tearDown() {
        validatorFactory.close();
    }

    @Test
    void validRowCreatesThePieceTheDigitizationAndAssignsItsTags() {
        // Given
        when(tagRepository.findByNameIgnoreCase("letters")).thenReturn(Optional.of(new Tag(3L, "Letters")));
        when(pieceRepository.findFirstByNameIgnoreCaseAndDate("Letter from Rosario", DATE)).thenReturn(Optional.empty());
        when(pieceService.createPiece(any(PieceCreateRequestDTO.class)))
                .thenReturn(new PieceResponseDTO(10L, "Letter from Rosario", null, DATE));
        when(pieceService.addDigitization(eq(10L), any(DigitizationCreateRequestDTO.class)))
                .thenReturn(new DigitizationResponseDTO(20L, URL, "Page 1", 10L, List.of()));

        // When
        importChunkService.importChunk(1L, List.of(row("Letter from Rosario", "1979-02-11", URL, "letters")));

        // Then
        verify(digitizationService).addTag(20L, 3L);
        assertEquals(1, job.getCreatedPieces());
        assertEquals(1, job.getCreatedDigitizations());
        assertEquals(0, job.getErrors().size());
    }

    @Test
    void rowWithATagThatDoesNotExistIsSkippedAndNothingIsCreated() {
        // Given
        when(tagRepository.findByNameIgnoreCase("Local press")).thenReturn(Optional.empty());

        // When
        importChunkService.importChunk(1L, List.of(row("Letter from Rosario", "1979-02-11", URL, "Local press")));

        // Then
        verify(pieceService, never()).createPiece(any());
        assertEquals(1, job.getProcessedRows());
        assertEquals(0, job.getCreatedDigitizations());
        assertEquals(2, job.getErrors().getFirst().getLine());
        assertEquals("tags: the tag 'Local press' does not exist", job.getErrors().getFirst().getMessage());
    }

    @Test
    void rowOfAnExistingPieceAddsTheDigitizationToThatPiece() {
        // Given
        Piece existing = new Piece(5L, "Letter from Rosario", "Handwritten letter", DATE);
        when(pieceRepository.findFirstByNameIgnoreCaseAndDate("Letter from Rosario", DATE))
                .thenReturn(Optional.of(existing));
        when(digitizationRepository.existsByPieceIdAndUrl(5L, URL)).thenReturn(false);
        when(pieceService.addDigitization(eq(5L), any(DigitizationCreateRequestDTO.class)))
                .thenReturn(new DigitizationResponseDTO(21L, URL, "Page 1", 5L, List.of()));

        // When
        importChunkService.importChunk(1L, List.of(row("Letter from Rosario", "1979-02-11", URL)));

        // Then
        verify(pieceService, never()).createPiece(any());
        verify(digitizationService, never()).addTag(anyLong(), anyLong());
        assertEquals(0, job.getCreatedPieces());
        assertEquals(1, job.getCreatedDigitizations());
    }

    private ImportRow row(String pieceName, String date, String url, String... tags) {
        return new ImportRow(2, pieceName, "", date, url, "Page 1", List.of(tags));
    }
}
