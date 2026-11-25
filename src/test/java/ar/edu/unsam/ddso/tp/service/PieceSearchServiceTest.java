package ar.edu.unsam.ddso.tp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ar.edu.unsam.ddso.tp.dto.PageResponseDTO;
import ar.edu.unsam.ddso.tp.dto.PieceResponseDTO;
import ar.edu.unsam.ddso.tp.dto.PieceSearchRequestDTO;
import ar.edu.unsam.ddso.tp.model.Piece;
import ar.edu.unsam.ddso.tp.model.exceptions.InvalidDateRangeException;
import ar.edu.unsam.ddso.tp.repository.PieceRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class PieceSearchServiceTest {

    @Mock private PieceRepository pieceRepository;

    private PieceSearchService pieceSearchService;

    @BeforeEach
    void setUp() {
        pieceSearchService = new PieceSearchService(pieceRepository);
    }

    @Test
    void searchPassesNormalizedCriteriaAndReturnsAPageOfPieces() {
        // Given
        LocalDate date = LocalDate.of(1977, 5, 3);
        PieceSearchRequestDTO request = new PieceSearchRequestDTO(" Letter ", null, null, null, Set.of("Press"));
        Piece piece = new Piece(1L, "Letter from Cordoba", "Handwritten letter", date);
        when(pieceRepository.search(
                        eq("letter"), isNull(), isNull(), isNull(), eq(Set.of("press")), eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(piece), PageRequest.of(0, 20), 1));

        // When
        PageResponseDTO<PieceResponseDTO> response = pieceSearchService.search(request, 0, 20);

        // Then
        assertEquals(List.of(new PieceResponseDTO(1L, "Letter from Cordoba", "Handwritten letter", date)),
                response.content());
        assertEquals(0, response.page());
        assertEquals(1, response.totalElements());
        assertEquals(1, response.totalPages());
    }

    @Test
    void searchWithInvertedDateRangeFailsWithoutQueryingTheRepository() {
        // Given
        PieceSearchRequestDTO request = new PieceSearchRequestDTO(
                null, null, LocalDate.of(1980, 1, 1), LocalDate.of(1970, 1, 1), null);

        // When / Then
        assertThrows(InvalidDateRangeException.class, () -> pieceSearchService.search(request, 0, 20));
        verifyNoInteractions(pieceRepository);
    }

    @Test
    void searchLimitsThePageSizeAndSortsByDateThenId() {
        // Given
        PieceSearchRequestDTO request = new PieceSearchRequestDTO(null, null, null, null, null);
        when(pieceRepository.search(any(), any(), any(), any(), anySet(), anyLong(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);

        // When
        pieceSearchService.search(request, -3, 500);

        // Then
        verify(pieceRepository).search(any(), any(), any(), any(), anySet(), anyLong(), pageable.capture());
        assertEquals(0, pageable.getValue().getPageNumber());
        assertEquals(PieceSearchService.MAX_PAGE_SIZE, pageable.getValue().getPageSize());
        assertEquals(Sort.by(Sort.Order.desc("date"), Sort.Order.asc("id")), pageable.getValue().getSort());
    }
}
