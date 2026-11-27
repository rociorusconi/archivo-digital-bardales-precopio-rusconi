package ar.edu.unsam.ddso.tp.service;

import ar.edu.unsam.ddso.tp.dto.ImportErrorDTO;
import ar.edu.unsam.ddso.tp.dto.ImportJobResponseDTO;
import ar.edu.unsam.ddso.tp.exception.ImportJobNotFoundException;
import ar.edu.unsam.ddso.tp.exception.InvalidImportFileException;
import ar.edu.unsam.ddso.tp.model.ImportJob;
import ar.edu.unsam.ddso.tp.repository.ImportJobRepository;
import java.io.IOException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Entry point of the CSV import (batch process): checks the file, registers the import and starts
 * it in the background ({@link ImportBatchProcessor}); then reports its progress.
 */
@Service
public class ImportService {

    private static final int MAX_FILE_NAME_LENGTH = 255;

    private final ImportJobRepository importJobRepository;
    private final ImportBatchProcessor importBatchProcessor;

    public ImportService(ImportJobRepository importJobRepository, ImportBatchProcessor importBatchProcessor) {
        this.importJobRepository = importJobRepository;
        this.importBatchProcessor = importBatchProcessor;
    }

    // ---------- Queries ----------

    @Transactional(readOnly = true)
    public ImportJobResponseDTO getImport(Long id) {
        return toResponse(importJobRepository.findById(id).orElseThrow(() -> new ImportJobNotFoundException(id)));
    }

    // ---------- Operations ----------

    /**
     * Not transactional on purpose: the import must be saved (committed) before the background
     * process starts, otherwise the process could look for it and not find it yet.
     */
    public ImportJobResponseDTO startImport(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidImportFileException("The file is empty");
        }
        // The rows are read now: the uploaded file is deleted when this request ends.
        List<ImportRow> rows = CsvImportReader.read(readContent(file));
        ImportJob job = importJobRepository.save(ImportJob.start(fileName(file), rows.size()));
        importBatchProcessor.process(job.getId(), rows);
        return toResponse(job);
    }

    // ---------- Private ----------

    private byte[] readContent(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new InvalidImportFileException("The file could not be read");
        }
    }

    private String fileName(MultipartFile file) {
        String name = file.getOriginalFilename();
        if (name == null || name.isBlank()) {
            return "import.csv";
        }
        return name.length() <= MAX_FILE_NAME_LENGTH ? name : name.substring(0, MAX_FILE_NAME_LENGTH);
    }

    private ImportJobResponseDTO toResponse(ImportJob job) {
        return new ImportJobResponseDTO(
                job.getId(),
                job.getFileName(),
                job.getStatus(),
                job.getTotalRows(),
                job.getProcessedRows(),
                job.getCreatedPieces(),
                job.getCreatedDigitizations(),
                job.getErrors().stream()
                        .map(error -> new ImportErrorDTO(error.getLine(), error.getMessage()))
                        .toList(),
                job.getFailureMessage(),
                job.getCreatedAt(),
                job.getFinishedAt());
    }
}
