package ar.edu.unsam.ddso.tp.service;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Runs an import in the background, chunk by chunk. Each chunk is saved in its own transaction, so
 * a large file does not hold a single huge transaction and its progress is visible while it runs.
 */
@Service
public class ImportBatchProcessor {

    static final int CHUNK_SIZE = 100;

    private static final Logger LOG = LoggerFactory.getLogger(ImportBatchProcessor.class);

    private final ImportChunkService importChunkService;

    public ImportBatchProcessor(ImportChunkService importChunkService) {
        this.importChunkService = importChunkService;
    }

    // ---------- Operations ----------

    @Async
    public void process(Long jobId, List<ImportRow> rows) {
        try {
            for (int from = 0; from < rows.size(); from += CHUNK_SIZE) {
                importChunkService.importChunk(jobId, rows.subList(from, Math.min(from + CHUNK_SIZE, rows.size())));
            }
            importChunkService.complete(jobId);
        } catch (RuntimeException e) {
            // Row errors are recorded and skipped; this is something else (e.g. the database failed).
            LOG.error("Import {} failed", jobId, e);
            importChunkService.fail(jobId, "Unexpected error while importing: " + e.getMessage());
        }
    }
}
