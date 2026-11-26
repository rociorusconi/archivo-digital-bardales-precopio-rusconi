package ar.edu.unsam.ddso.tp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.unsam.ddso.tp.dto.DigitizationResponseDTO;
import ar.edu.unsam.ddso.tp.dto.TagResponseDTO;
import ar.edu.unsam.ddso.tp.exception.DigitizationNotFoundException;
import ar.edu.unsam.ddso.tp.exception.TagNotFoundException;
import ar.edu.unsam.ddso.tp.model.Digitization;
import ar.edu.unsam.ddso.tp.model.Piece;
import ar.edu.unsam.ddso.tp.model.Tag;
import ar.edu.unsam.ddso.tp.repository.DigitizationRepository;
import ar.edu.unsam.ddso.tp.repository.TagRepository;
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
class DigitizationServiceTest {

    @Mock private DigitizationRepository digitizationRepository;
    @Mock private TagRepository tagRepository;

    private DigitizationService digitizationService;
    private Digitization digitization;

    @BeforeEach
    void setUp() {
        digitizationService = new DigitizationService(digitizationRepository, tagRepository);
        Piece piece = new Piece(1L, "Letter from Cordoba", "Handwritten letter", LocalDate.of(1977, 5, 3));
        digitization = new Digitization(10L, "https://files.example/letter.pdf", "Scan", piece, new HashSet<>());
    }

    @Test
    void addTagAssignsTheTagAndReturnsTheDigitizationWithItsTags() {
        // Given
        Tag press = new Tag(5L, "Press");
        when(digitizationRepository.findById(10L)).thenReturn(Optional.of(digitization));
        when(tagRepository.findById(5L)).thenReturn(Optional.of(press));
        when(digitizationRepository.save(digitization)).thenReturn(digitization);

        // When
        DigitizationResponseDTO response = digitizationService.addTag(10L, 5L);

        // Then
        assertEquals(10L, response.id());
        assertEquals(1L, response.pieceId());
        assertEquals(List.of(new TagResponseDTO(5L, "Press")), response.tags());
        verify(digitizationRepository).save(digitization);
    }

    @Test
    void addTagTheDigitizationAlreadyHasDoesNotDuplicateIt() {
        // Given
        digitization.setTags(new HashSet<>(Set.of(new Tag(5L, "Press"))));
        when(digitizationRepository.findById(10L)).thenReturn(Optional.of(digitization));
        when(tagRepository.findById(5L)).thenReturn(Optional.of(new Tag(5L, "Press")));
        when(digitizationRepository.save(digitization)).thenReturn(digitization);

        // When
        DigitizationResponseDTO response = digitizationService.addTag(10L, 5L);

        // Then
        assertEquals(1, response.tags().size());
    }

    @Test
    void addTagToMissingDigitizationThrowsNotFoundAndDoesNotSave() {
        // Given
        when(digitizationRepository.findById(99L)).thenReturn(Optional.empty());

        // When / Then
        assertThrows(DigitizationNotFoundException.class, () -> digitizationService.addTag(99L, 5L));
        verify(digitizationRepository, never()).save(any(Digitization.class));
    }

    @Test
    void addMissingTagThrowsNotFoundAndDoesNotSave() {
        // Given
        when(digitizationRepository.findById(10L)).thenReturn(Optional.of(digitization));
        when(tagRepository.findById(99L)).thenReturn(Optional.empty());

        // When / Then
        assertThrows(TagNotFoundException.class, () -> digitizationService.addTag(10L, 99L));
        verify(digitizationRepository, never()).save(any(Digitization.class));
    }
}
