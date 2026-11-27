package ar.edu.unsam.ddso.tp.service;

import ar.edu.unsam.ddso.tp.dto.DigitizationCreateRequestDTO;
import ar.edu.unsam.ddso.tp.dto.PieceCreateRequestDTO;
import ar.edu.unsam.ddso.tp.exception.ImportJobNotFoundException;
import ar.edu.unsam.ddso.tp.model.ImportJob;
import ar.edu.unsam.ddso.tp.model.Piece;
import ar.edu.unsam.ddso.tp.model.Tag;
import ar.edu.unsam.ddso.tp.repository.DigitizationRepository;
import ar.edu.unsam.ddso.tp.repository.ImportJobRepository;
import ar.edu.unsam.ddso.tp.repository.PieceRepository;
import ar.edu.unsam.ddso.tp.repository.TagRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Imports the rows of a CSV, one chunk per transaction. Each row goes through the same rules as
 * the API: the DTO validations, tags that must already exist in the catalog (tags are not free),
 * and the same services that create pieces, digitizations and assign tags.
 */
@Service
public class ImportChunkService {

    private final ImportJobRepository importJobRepository;
    private final PieceRepository pieceRepository;
    private final TagRepository tagRepository;
    private final DigitizationRepository digitizationRepository;
    private final PieceService pieceService;
    private final DigitizationService digitizationService;
    private final Validator validator;

    public ImportChunkService(
            ImportJobRepository importJobRepository,
            PieceRepository pieceRepository,
            TagRepository tagRepository,
            DigitizationRepository digitizationRepository,
            PieceService pieceService,
            DigitizationService digitizationService,
            Validator validator) {
        this.importJobRepository = importJobRepository;
        this.pieceRepository = pieceRepository;
        this.tagRepository = tagRepository;
        this.digitizationRepository = digitizationRepository;
        this.pieceService = pieceService;
        this.digitizationService = digitizationService;
        this.validator = validator;
    }

    // ---------- Operations ----------

    /** Imports a chunk of rows. A row with errors is skipped and recorded; the others are saved. */
    @Transactional
    public void importChunk(Long jobId, List<ImportRow> rows) {
        ImportJob job = findJob(jobId);
        for (ImportRow row : rows) {
            try {
                job.recordImported(importRow(row));
            } catch (InvalidRowException e) {
                job.recordError(row.line(), e.getMessage());
            }
        }
        importJobRepository.save(job);
    }

    @Transactional
    public void complete(Long jobId) {
        ImportJob job = findJob(jobId);
        job.complete();
        importJobRepository.save(job);
    }

    @Transactional
    public void fail(Long jobId, String message) {
        ImportJob job = findJob(jobId);
        job.fail(message);
        importJobRepository.save(job);
    }

    // ---------- Private ----------

    /**
     * Loads one row and returns whether it created a new piece. Everything is validated before
     * creating anything: a service that threw an exception would mark the whole chunk for rollback.
     */
    private boolean importRow(ImportRow row) {
        PieceCreateRequestDTO pieceRequest =
                new PieceCreateRequestDTO(row.pieceName(), emptyToNull(row.pieceDescription()), parseDate(row));
        DigitizationCreateRequestDTO digitizationRequest =
                new DigitizationCreateRequestDTO(row.url(), emptyToNull(row.digitizationDescription()));
        validate(pieceRequest, digitizationRequest);
        List<Tag> tags = findTags(row.tags());

        Optional<Piece> existingPiece = findPiece(pieceRequest.name(), pieceRequest.date());
        if (existingPiece.isPresent()
                && digitizationRepository.existsByPieceIdAndUrl(existingPiece.get().getId(), row.url())) {
            throw new InvalidRowException("url: already loaded for the piece '" + pieceRequest.name() + "'");
        }

        Long pieceId = existingPiece
                .map(Piece::getId)
                .orElseGet(() -> pieceService.createPiece(pieceRequest).id());
        Long digitizationId = pieceService.addDigitization(pieceId, digitizationRequest).id();
        tags.forEach(tag -> digitizationService.addTag(digitizationId, tag.getId()));
        return existingPiece.isEmpty();
    }

    private LocalDate parseDate(ImportRow row) {
        if (row.pieceDate().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(row.pieceDate());
        } catch (DateTimeParseException e) {
            throw new InvalidRowException("piece_date: invalid date '" + row.pieceDate() + "', use yyyy-MM-dd");
        }
    }

    // Same validations as the API (@NotBlank, @Size, @URL), reported with the CSV column names.
    private void validate(PieceCreateRequestDTO piece, DigitizationCreateRequestDTO digitization) {
        String errors = Stream.concat(
                        violations(piece, field -> "piece_" + field),
                        violations(digitization, field -> "url".equals(field) ? field : "digitization_" + field))
                .sorted()
                .collect(Collectors.joining(", "));
        if (!errors.isEmpty()) {
            throw new InvalidRowException(errors);
        }
    }

    private <T> Stream<String> violations(T request, Function<String, String> columnName) {
        return validator.validate(request).stream()
                .map((ConstraintViolation<T> violation) ->
                        columnName.apply(violation.getPropertyPath().toString()) + ": " + violation.getMessage());
    }

    private List<Tag> findTags(List<String> names) {
        List<Tag> tags = new ArrayList<>();
        for (String name : names) {
            tags.add(tagRepository
                    .findByNameIgnoreCase(name)
                    .orElseThrow(() -> new InvalidRowException("tags: the tag '" + name + "' does not exist")));
        }
        return tags;
    }

    // A piece with the same name and date is the same piece: its digitizations are added to it.
    private Optional<Piece> findPiece(String name, LocalDate date) {
        String trimmed = name.trim();
        return date == null
                ? pieceRepository.findFirstByNameIgnoreCaseAndDateIsNull(trimmed)
                : pieceRepository.findFirstByNameIgnoreCaseAndDate(trimmed, date);
    }

    private ImportJob findJob(Long jobId) {
        return importJobRepository.findById(jobId).orElseThrow(() -> new ImportJobNotFoundException(jobId));
    }

    private static String emptyToNull(String value) {
        return value.isEmpty() ? null : value;
    }

    /** A row that cannot be imported; the message says which column and why. */
    private static final class InvalidRowException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        InvalidRowException(String message) {
            super(message);
        }
    }
}
