package ar.edu.unsam.ddso.tp.bot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.unsam.ddso.tp.dto.DigitizationResponseDTO;
import ar.edu.unsam.ddso.tp.dto.PageResponseDTO;
import ar.edu.unsam.ddso.tp.dto.PieceDetailResponseDTO;
import ar.edu.unsam.ddso.tp.dto.PieceResponseDTO;
import ar.edu.unsam.ddso.tp.dto.PieceSearchRequestDTO;
import ar.edu.unsam.ddso.tp.dto.TagResponseDTO;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BotCommandHandlerTest {

    @Mock private ArchiveApiClient archiveApiClient;

    private BotCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new BotCommandHandler(archiveApiClient);
    }

    @Test
    void searchWithFiltersCallsTheApiAndListsThePieces() {
        // Given
        PieceResponseDTO letter =
                new PieceResponseDTO(1L, "Letter from Cordoba", "Handwritten letter", LocalDate.of(1977, 5, 3));
        when(archiveApiClient.searchPieces(any(PieceSearchRequestDTO.class), eq(1), eq(BotCommandHandler.PAGE_SIZE)))
                .thenReturn(new PageResponseDTO<>(List.of(letter), 1, BotCommandHandler.PAGE_SIZE, 11, 2));

        // When
        String reply = handler.handle("/buscar letter from tag: local press, letters desde: 1970-01-01 pagina: 2");

        // Then
        ArgumentCaptor<PieceSearchRequestDTO> request = ArgumentCaptor.forClass(PieceSearchRequestDTO.class);
        verify(archiveApiClient).searchPieces(request.capture(), eq(1), eq(BotCommandHandler.PAGE_SIZE));
        assertEquals("letter from", request.getValue().name());
        assertEquals(Set.of("local press", "letters"), request.getValue().tags());
        assertEquals(LocalDate.of(1970, 1, 1), request.getValue().dateFrom());
        assertTrue(reply.contains("#1 · Letter from Cordoba · 1977-05-03"));
        assertTrue(reply.contains("página 2 de 2"));
    }

    @Test
    void searchAcceptsFiltersWrittenWithAccents() {
        // Given
        when(archiveApiClient.searchPieces(any(PieceSearchRequestDTO.class), eq(1), eq(BotCommandHandler.PAGE_SIZE)))
                .thenReturn(new PageResponseDTO<>(List.of(), 1, BotCommandHandler.PAGE_SIZE, 0, 0));

        // When
        handler.handle("/buscar Descripción: handwritten página: 2");

        // Then
        ArgumentCaptor<PieceSearchRequestDTO> request = ArgumentCaptor.forClass(PieceSearchRequestDTO.class);
        verify(archiveApiClient).searchPieces(request.capture(), eq(1), eq(BotCommandHandler.PAGE_SIZE));
        assertEquals("handwritten", request.getValue().description());
        assertNull(request.getValue().name());
    }

    @Test
    void searchWithAnUnknownFilterAnswersWhichFiltersExistAndDoesNotSearch() {
        // When
        String reply = handler.handle("/buscar carta fecha: 1977");

        // Then
        assertTrue(reply.contains("No conozco el filtro \"fecha:\""));
        verify(archiveApiClient, never()).searchPieces(any(), anyInt(), anyInt());
    }

    @Test
    void searchWithAnInvalidDateAnswersHowToWriteItAndDoesNotSearch() {
        // When
        String reply = handler.handle("/buscar desde: 03/05/1977");

        // Then
        assertTrue(reply.contains("no es válida"));
        verify(archiveApiClient, never()).searchPieces(any(), anyInt(), anyInt());
    }

    @Test
    void searchByTagWithoutResultsSuggestsTheTagsCommand() {
        // Given
        when(archiveApiClient.searchPieces(any(PieceSearchRequestDTO.class), eq(0), eq(BotCommandHandler.PAGE_SIZE)))
                .thenReturn(new PageResponseDTO<>(List.of(), 0, BotCommandHandler.PAGE_SIZE, 0, 0));

        // When
        String reply = handler.handle("/buscar tag: testim");

        // Then
        assertTrue(reply.contains("/tags"));
    }

    @Test
    void tagsListsTheExistingTags() {
        // Given
        when(archiveApiClient.listTags())
                .thenReturn(List.of(new TagResponseDTO(2L, "Letters"), new TagResponseDTO(1L, "Press")));

        // When
        String reply = handler.handle("/tags");

        // Then
        assertTrue(reply.contains("Tags (2): Letters, Press"));
    }

    @Test
    void pieceShowsItsDigitizationsWithLinksAndTags() {
        // Given
        DigitizationResponseDTO scan = new DigitizationResponseDTO(
                10L, "https://files.example/letter-p1.pdf", "Scan of page 1", 1L,
                List.of(new TagResponseDTO(2L, "Letters"), new TagResponseDTO(1L, "Press")));
        when(archiveApiClient.getPiece(1L)).thenReturn(new PieceDetailResponseDTO(
                1L, "Letter from Cordoba", "Handwritten letter", LocalDate.of(1977, 5, 3), List.of(scan)));

        // When
        String reply = handler.handle("/pieza 1");

        // Then
        assertTrue(reply.contains("https://files.example/letter-p1.pdf"));
        assertTrue(reply.contains("Tags: Letters, Press"));
    }

    @Test
    void missingPieceAnswersThatItDoesNotExist() {
        // Given
        when(archiveApiClient.getPiece(99L)).thenThrow(new ArchiveApiException(404, "Piece not found with id: 99"));

        // When
        String reply = handler.handle("/pieza 99");

        // Then
        assertEquals("No existe la pieza 99.", reply);
    }
}
