package ar.edu.unsam.ddso.tp.model;

import ar.edu.unsam.ddso.tp.model.enums.ImportStatus;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * A CSV import that runs in the background. It keeps the progress (rows processed, pieces and
 * digitizations created) and the rows that were skipped, so the client can follow it with GET
 * /api/imports/{id}. The counters only change through the methods below.
 */
@Entity
@Table(name = "import_jobs")
@Getter
@NoArgsConstructor
public class ImportJob {

    private static final int MAX_MESSAGE_LENGTH = 1000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fileName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ImportStatus status;

    private int totalRows;
    private int processedRows;
    private int createdPieces;
    private int createdDigitizations;

    @Column(length = MAX_MESSAGE_LENGTH)
    private String failureMessage;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime finishedAt;

    @ElementCollection
    @CollectionTable(name = "import_job_errors", joinColumns = @JoinColumn(name = "import_job_id"))
    @OrderBy("line")
    private List<ImportRowError> errors = new ArrayList<>();

    public static ImportJob start(String fileName, int totalRows) {
        ImportJob job = new ImportJob();
        job.fileName = fileName;
        job.totalRows = totalRows;
        job.status = ImportStatus.IN_PROGRESS;
        job.createdAt = LocalDateTime.now();
        return job;
    }

    // ---------- Business rules ----------

    /** A row was loaded: it always creates a digitization, and a piece if it did not exist. */
    public void recordImported(boolean createdPiece) {
        processedRows++;
        createdDigitizations++;
        if (createdPiece) {
            createdPieces++;
        }
    }

    /** A row was skipped: the import goes on with the next one. */
    public void recordError(int line, String message) {
        processedRows++;
        errors.add(new ImportRowError(line, truncate(message)));
    }

    public void complete() {
        status = ImportStatus.COMPLETED;
        finishedAt = LocalDateTime.now();
    }

    /** An unexpected error stopped the import. The rows of the chunks already saved are kept. */
    public void fail(String message) {
        status = ImportStatus.FAILED;
        failureMessage = truncate(message);
        finishedAt = LocalDateTime.now();
    }

    // ---------- Private ----------

    private static String truncate(String message) {
        return message.length() <= MAX_MESSAGE_LENGTH ? message : message.substring(0, MAX_MESSAGE_LENGTH);
    }
}
