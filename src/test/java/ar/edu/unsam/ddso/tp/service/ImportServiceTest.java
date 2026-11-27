package ar.edu.unsam.ddso.tp.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import ar.edu.unsam.ddso.tp.exception.InvalidImportFileException;
import ar.edu.unsam.ddso.tp.repository.ImportJobRepository;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class ImportServiceTest {

    @Mock private ImportJobRepository importJobRepository;
    @Mock private ImportBatchProcessor importBatchProcessor;

    private ImportService importService;

    @BeforeEach
    void setUp() {
        importService = new ImportService(importJobRepository, importBatchProcessor);
    }

    @Test
    void fileWithoutTheExpectedColumnsIsRejectedAndNotImported() {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file", "pieces.csv", "text/csv", "name,url\nLetter,https://files.example/a.pdf\n"
                        .getBytes(StandardCharsets.UTF_8));

        // When
        InvalidImportFileException error =
                assertThrows(InvalidImportFileException.class, () -> importService.startImport(file));

        // Then
        assertTrue(error.getMessage().startsWith("Missing columns: piece_name"));
        verify(importJobRepository, never()).save(any());
        verify(importBatchProcessor, never()).process(anyLong(), anyList());
    }
}
