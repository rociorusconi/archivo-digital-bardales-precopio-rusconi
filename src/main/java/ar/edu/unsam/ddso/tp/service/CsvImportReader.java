package ar.edu.unsam.ddso.tp.service;

import ar.edu.unsam.ddso.tp.exception.InvalidImportFileException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

/**
 * Reads the import CSV (one row per digitization, tags separated by "|") into {@link ImportRow}s.
 * It only checks the structure of the file; the values of each row are validated when imported.
 */
final class CsvImportReader {

    static final List<String> COLUMNS = List.of(
            "piece_name", "piece_description", "piece_date", "url", "digitization_description", "tags");

    private static final Pattern TAG_SEPARATOR = Pattern.compile(Pattern.quote("|"));

    // Excel saves "CSV UTF-8" with this mark at the beginning of the file.
    private static final byte[] UTF8_BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    private static final CSVFormat FORMAT = CSVFormat.DEFAULT.builder()
            .setHeader()
            .setSkipHeaderRecord(true)
            .setIgnoreEmptyLines(true)
            .setTrim(true)
            .get();

    private CsvImportReader() {}

    static List<ImportRow> read(byte[] content) {
        int start = startsWithBom(content) ? UTF8_BOM.length : 0;
        try (CSVParser parser = FORMAT.parse(new InputStreamReader(
                new ByteArrayInputStream(content, start, content.length - start), StandardCharsets.UTF_8))) {
            checkColumns(parser.getHeaderNames());
            List<ImportRow> rows = new ArrayList<>();
            for (CSVRecord csvRecord : parser) {
                rows.add(toRow(csvRecord));
            }
            if (rows.isEmpty()) {
                throw new InvalidImportFileException("The file has no data rows");
            }
            return rows;
        } catch (IOException | UncheckedIOException | IllegalArgumentException | IllegalStateException e) {
            throw new InvalidImportFileException("The file is not a valid CSV: " + e.getMessage());
        }
    }

    // ---------- Private ----------

    private static void checkColumns(List<String> header) {
        List<String> missing = COLUMNS.stream().filter(column -> !header.contains(column)).toList();
        if (!missing.isEmpty()) {
            throw new InvalidImportFileException(
                    "Missing columns: " + String.join(", ", missing) + ". Expected: " + String.join(",", COLUMNS));
        }
    }

    private static ImportRow toRow(CSVRecord csvRecord) {
        return new ImportRow(
                // The header is line 1, so the first data row is line 2 (as seen in a spreadsheet).
                (int) csvRecord.getRecordNumber() + 1,
                value(csvRecord, "piece_name"),
                value(csvRecord, "piece_description"),
                value(csvRecord, "piece_date"),
                value(csvRecord, "url"),
                value(csvRecord, "digitization_description"),
                TAG_SEPARATOR.splitAsStream(value(csvRecord, "tags"))
                        .map(String::trim)
                        .filter(tag -> !tag.isEmpty())
                        .distinct()
                        .toList());
    }

    // A row with fewer columns than the header has the missing values empty.
    private static String value(CSVRecord csvRecord, String column) {
        return csvRecord.isSet(column) ? csvRecord.get(column).trim() : "";
    }

    private static boolean startsWithBom(byte[] content) {
        return content.length >= UTF8_BOM.length
                && Arrays.equals(Arrays.copyOf(content, UTF8_BOM.length), UTF8_BOM);
    }
}
