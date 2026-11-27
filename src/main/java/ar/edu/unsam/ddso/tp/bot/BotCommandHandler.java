package ar.edu.unsam.ddso.tp.bot;

import ar.edu.unsam.ddso.tp.dto.DigitizationResponseDTO;
import ar.edu.unsam.ddso.tp.dto.PageResponseDTO;
import ar.edu.unsam.ddso.tp.dto.PieceDetailResponseDTO;
import ar.edu.unsam.ddso.tp.dto.PieceResponseDTO;
import ar.edu.unsam.ddso.tp.dto.PieceSearchRequestDTO;
import ar.edu.unsam.ddso.tp.dto.TagResponseDTO;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Commands of the bot, independent of the chat platform: turns a text message into a call to the
 * REST API (through {@link ArchiveApiClient}) and formats the answer as text. Telegram is plugged
 * in by an adapter ({@link TelegramBot}); another platform would only need another adapter.
 * Messages are in Spanish because they are shown to the end user.
 */
@Component
public class BotCommandHandler {

    static final int PAGE_SIZE = 10;
    private static final int MAX_DESCRIPTION_LENGTH = 150;

    // Filters of /buscar, written as "key: value" (e.g. "nombre: carta tag: prensa").
    // Keys are accepted with or without accent ("descripción", "página") and in any case.
    private static final Pattern FILTER_KEY =
            Pattern.compile("(?iU)\\b(nombre|descripci[oó]n|tags?|desde|hasta|p[aá]gina):");

    private static final Pattern ANY_KEY = Pattern.compile("(?U)\\b(\\p{L}+):");

    static final String FILTERS = "nombre, descripcion, tag, desde, hasta y pagina";

    static final String WELCOME =
            """
            ¡Hola! Soy el bot del Archivo Digital. Con él podés buscar piezas del archivo \
            (cartas, fotografías, diarios…) y ver sus digitalizaciones.

            Probá, por ejemplo:
            /buscar carta
            /tags

            """;

    static final String HELP =
            """
            Archivo Digital: buscá piezas del archivo.

            /buscar <texto> — busca por nombre
            /buscar nombre: carta descripcion: manuscrita tag: prensa,cartas desde: 1970-01-01 hasta: 1980-12-31 pagina: 2
              (todos los filtros son opcionales y se combinan)
              (los tags se escriben completos y separados por coma)
            /pieza <id> — muestra una pieza con sus digitalizaciones y tags
            /tags — muestra los tags que existen
            /ayuda — muestra este mensaje""";

    static final String API_UNAVAILABLE = "No pude consultar el archivo en este momento. Probá de nuevo más tarde.";

    private final ArchiveApiClient archiveApiClient;

    public BotCommandHandler(ArchiveApiClient archiveApiClient) {
        this.archiveApiClient = archiveApiClient;
    }

    // ---------- Operations ----------

    public String handle(String text) {
        String message = text == null ? "" : text.trim();
        String[] parts = message.split("\\s+", 2);
        String command = parts[0].toLowerCase(Locale.ROOT).replaceFirst("@.*", "");
        String arguments = parts.length > 1 ? parts[1] : "";

        try {
            return switch (command) {
                case "/start" -> WELCOME + HELP;
                case "/ayuda", "/help" -> HELP;
                case "/buscar" -> search(arguments);
                case "/pieza" -> showPiece(arguments);
                case "/tags" -> listTags();
                default -> "No conozco ese comando.\n\n" + HELP;
            };
        } catch (ArchiveApiException e) {
            return API_UNAVAILABLE;
        }
    }

    // ---------- Private ----------

    private String search(String arguments) {
        String unknownKey = findUnknownKey(arguments);
        if (unknownKey != null) {
            return "No conozco el filtro \"" + unknownKey + ":\". Los filtros son: " + FILTERS + ".\n"
                    + "Por ejemplo: /buscar descripcion: manuscrita desde: 1970-01-01";
        }
        SearchFilters filters;
        try {
            filters = parseFilters(arguments);
        } catch (DateTimeParseException e) {
            return "La fecha \"" + e.getParsedString() + "\" no es válida. Usá el formato aaaa-mm-dd, por ejemplo 1977-05-03.";
        } catch (NumberFormatException e) {
            return "La página tiene que ser un número mayor o igual a 1.";
        }

        PageResponseDTO<PieceResponseDTO> result;
        try {
            result = archiveApiClient.searchPieces(filters.request(), filters.page() - 1, PAGE_SIZE);
        } catch (ArchiveApiException e) {
            if (e.getStatus() != 400) {
                throw e;
            }
            return "Revisá los filtros de la búsqueda: " + e.getMessage();
        }
        return formatResults(result, filters.request().tags() != null);
    }

    private SearchFilters parseFilters(String arguments) {
        String name = null;
        String description = null;
        LocalDate dateFrom = null;
        LocalDate dateTo = null;
        Set<String> tags = new HashSet<>();
        int page = 1;

        Matcher matcher = FILTER_KEY.matcher(arguments);
        int valueStart = 0;
        String key = "nombre";
        while (true) {
            boolean found = matcher.find();
            String value = arguments.substring(valueStart, found ? matcher.start() : arguments.length()).trim();
            if (!value.isEmpty()) {
                switch (key) {
                    case "nombre" -> name = value;
                    case "descripcion" -> description = value;
                    case "tag", "tags" -> tags.addAll(splitTags(value));
                    case "desde" -> dateFrom = LocalDate.parse(value);
                    case "hasta" -> dateTo = LocalDate.parse(value);
                    default -> page = parsePage(value);
                }
            }
            if (!found) {
                break;
            }
            key = normalizeKey(matcher.group(1));
            valueStart = matcher.end();
        }

        PieceSearchRequestDTO request =
                new PieceSearchRequestDTO(name, description, dateFrom, dateTo, tags.isEmpty() ? null : tags);
        return new SearchFilters(request, page);
    }

    // "Descripción" -> "descripcion", "PÁGINA" -> "pagina".
    private String normalizeKey(String key) {
        return key.toLowerCase(Locale.ROOT).replace('ó', 'o').replace('á', 'a').replace('é','e').replace('í', 'i').replace('ú', 'u');
    }

    private String findUnknownKey(String arguments) {
        Matcher matcher = ANY_KEY.matcher(arguments);
        while (matcher.find()) {
            if (!FILTER_KEY.matcher(matcher.group()).matches()) {
                return matcher.group(1);
            }
        }
        return null;
    }

    private Set<String> splitTags(String value) {
        return Pattern.compile(",").splitAsStream(value)
                .map(String::trim)
                .filter(tag -> !tag.isBlank())
                .collect(Collectors.toSet());
    }

    private int parsePage(String value) {
        int page = Integer.parseInt(value);
        if (page < 1) {
            throw new NumberFormatException(value);
        }
        return page;
    }

    private String formatResults(PageResponseDTO<PieceResponseDTO> result, boolean filteredByTags) {
        if (result.totalElements() == 0) {
            return filteredByTags
                    ? "No encontré piezas con esos filtros. Los tags se escriben completos: mirá cuáles existen con /tags"
                    : "No encontré piezas con esos filtros.";
        }
        if (result.content().isEmpty()) {
            return "No hay más resultados: la búsqueda tiene " + result.totalPages() + " página(s).";
        }
        StringBuilder text = new StringBuilder();
        text.append("Encontré ").append(result.totalElements()).append(" pieza(s)");
        if (result.totalPages() > 1) {
            text.append(" (página ").append(result.page() + 1).append(" de ").append(result.totalPages()).append(')');
        }
        text.append(":\n");
        for (PieceResponseDTO piece : result.content()) {
            text.append("\n#").append(piece.id()).append(" · ").append(piece.name());
            if (piece.date() != null) {
                text.append(" · ").append(piece.date());
            }
            if (piece.description() != null && !piece.description().isBlank()) {
                text.append('\n').append(shorten(piece.description()));
            }
            text.append('\n');
        }
        text.append("\nPara ver una pieza: /pieza <id>");
        if (result.page() + 1 < result.totalPages()) {
            text.append("\nPara ver más resultados, repetí la búsqueda con pagina: ").append(result.page() + 2);
        }
        return text.toString();
    }

    private String showPiece(String arguments) {
        long id;
        try {
            id = Long.parseLong(arguments.trim());
        } catch (NumberFormatException e) {
            return "Indicá el número de pieza, por ejemplo: /pieza 12";
        }

        PieceDetailResponseDTO piece;
        try {
            piece = archiveApiClient.getPiece(id);
        } catch (ArchiveApiException e) {
            if (e.getStatus() != 404) {
                throw e;
            }
            return "No existe la pieza " + id + ".";
        }
        return formatPiece(piece);
    }

    private String formatPiece(PieceDetailResponseDTO piece) {
        StringBuilder text = new StringBuilder();
        text.append('#').append(piece.id()).append(" · ").append(piece.name()).append('\n');
        if (piece.date() != null) {
            text.append("Fecha: ").append(piece.date()).append('\n');
        }
        if (piece.description() != null && !piece.description().isBlank()) {
            text.append(piece.description()).append('\n');
        }
        if (piece.digitizations().isEmpty()) {
            return text.append("\nTodavía no tiene digitalizaciones.").toString();
        }
        text.append("\nDigitalizaciones (").append(piece.digitizations().size()).append("):\n");
        int number = 1;
        for (DigitizationResponseDTO digitization : piece.digitizations()) {
            text.append('\n').append(number++).append(". ");
            if (digitization.description() != null && !digitization.description().isBlank()) {
                text.append(digitization.description()).append("\n   ");
            }
            text.append(digitization.url());
            if (!digitization.tags().isEmpty()) {
                String tagNames = digitization.tags().stream().map(TagResponseDTO::name).collect(Collectors.joining(", "));
                text.append("\n   Tags: ").append(tagNames);
            }
            text.append('\n');
        }
        return text.toString();
    }

    private String listTags() {
        List<TagResponseDTO> tags = archiveApiClient.listTags();
        if (tags.isEmpty()) {
            return "Todavía no hay tags cargados.";
        }
        String names = tags.stream().map(TagResponseDTO::name).collect(Collectors.joining(", "));
        return "Tags (" + tags.size() + "): " + names + "\n\nPara buscar por tag: /buscar tag: " + tags.get(0).name();
    }

    private String shorten(String value) {
        return value.length() <= MAX_DESCRIPTION_LENGTH ? value : value.substring(0, MAX_DESCRIPTION_LENGTH) + "…";
    }

    private record SearchFilters(PieceSearchRequestDTO request, int page) {}
}
