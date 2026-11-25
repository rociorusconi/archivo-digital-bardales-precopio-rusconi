package ar.edu.unsam.ddso.tp.model;

import ar.edu.unsam.ddso.tp.model.exceptions.InvalidDateRangeException;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Piece search criteria. All are optional and combined with AND: partial text on name and
 * description, inclusive date range and tags (the piece must have all of them).
 */
public record PieceSearchCriteria(
        String name, String description, LocalDate dateFrom, LocalDate dateTo, Set<String> tags) {

    public PieceSearchCriteria {
        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            throw new InvalidDateRangeException(dateFrom, dateTo);
        }
        name = normalize(name);
        description = normalize(description);
        tags = normalizeTags(tags);
    }

    private static String normalize(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        return text.trim().toLowerCase(Locale.ROOT);
    }

    private static Set<String> normalizeTags(Set<String> tags) {
        if (tags == null) {
            return Set.of();
        }
        return tags.stream()
                .map(PieceSearchCriteria::normalize)
                .filter(tag -> tag != null)
                .collect(Collectors.toUnmodifiableSet());
    }
}
