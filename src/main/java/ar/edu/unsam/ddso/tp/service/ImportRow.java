package ar.edu.unsam.ddso.tp.service;

import java.util.List;

/** One data row of the CSV, as text, with its line number in the file. Validated when imported. */
record ImportRow(
        int line,
        String pieceName,
        String pieceDescription,
        String pieceDate,
        String url,
        String digitizationDescription,
        List<String> tags) {

    ImportRow {
        tags = List.copyOf(tags);
    }
}
