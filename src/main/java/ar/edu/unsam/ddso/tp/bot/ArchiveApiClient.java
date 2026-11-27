package ar.edu.unsam.ddso.tp.bot;

import ar.edu.unsam.ddso.tp.dto.ErrorResponseDTO;
import ar.edu.unsam.ddso.tp.dto.PageResponseDTO;
import ar.edu.unsam.ddso.tp.dto.PieceDetailResponseDTO;
import ar.edu.unsam.ddso.tp.dto.PieceResponseDTO;
import ar.edu.unsam.ddso.tp.dto.PieceSearchRequestDTO;
import ar.edu.unsam.ddso.tp.dto.TagResponseDTO;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriBuilder;

/**
 * HTTP client of the Archivo Digital REST API used by the bot. The bot goes through the
 * controllers like any other client, so it gets the same validations and error responses.
 */
@Component
public class ArchiveApiClient {

    private static final ParameterizedTypeReference<PageResponseDTO<PieceResponseDTO>> PIECE_PAGE =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<TagResponseDTO>> TAG_LIST =
            new ParameterizedTypeReference<>() {};

    private final RestClient restClient;

    public ArchiveApiClient(@Value("${archive.api.base-url}") String baseUrl) {
        this.restClient = RestClient.create(baseUrl);
    }

    // ---------- Queries ----------

    /** GET /api/pieces/search. */
    public PageResponseDTO<PieceResponseDTO> searchPieces(PieceSearchRequestDTO filters, int page, int size) {
        return call(() -> restClient.get()
                .uri(uriBuilder -> searchUri(uriBuilder, filters, page, size))
                .retrieve()
                .body(PIECE_PAGE));
    }

    /** GET /api/pieces/{id}. */
    public PieceDetailResponseDTO getPiece(long id) {
        return call(() -> restClient.get()
                .uri("/api/pieces/{id}", id)
                .retrieve()
                .body(PieceDetailResponseDTO.class));
    }

    /** GET /api/tags. */
    public List<TagResponseDTO> listTags() {
        return call(() -> restClient.get()
                .uri("/api/tags")
                .retrieve()
                .body(TAG_LIST));
    }

    // ---------- Private ----------

    // Values go as URI variables so that characters like "&" or "+" are encoded correctly.
    private URI searchUri(UriBuilder uriBuilder, PieceSearchRequestDTO filters, int page, int size) {
        Map<String, Object> values = new HashMap<>();
        uriBuilder.path("/api/pieces/search");
        addParam(uriBuilder, values, "name", filters.name());
        addParam(uriBuilder, values, "description", filters.description());
        addParam(uriBuilder, values, "dateFrom", filters.dateFrom());
        addParam(uriBuilder, values, "dateTo", filters.dateTo());
        if (filters.tags() != null) {
            List<String> tags = new ArrayList<>(filters.tags());
            for (int i = 0; i < tags.size(); i++) {
                addParam(uriBuilder, values, "tags", "tag" + i, tags.get(i));
            }
        }
        addParam(uriBuilder, values, "page", page);
        addParam(uriBuilder, values, "size", size);
        return uriBuilder.build(values);
    }

    private void addParam(UriBuilder uriBuilder, Map<String, Object> values, String name, Object value) {
        addParam(uriBuilder, values, name, name, value);
    }

    private void addParam(
            UriBuilder uriBuilder, Map<String, Object> values, String name, String variable, Object value) {
        if (value != null) {
            uriBuilder.queryParam(name, "{" + variable + "}");
            values.put(variable, value);
        }
    }

    // Turns an error response into an ArchiveApiException with the message of our ErrorResponseDTO.
    private <T> T call(Supplier<T> request) {
        try {
            return request.get();
        } catch (RestClientResponseException e) {
            throw new ArchiveApiException(e.getStatusCode().value(), errorMessage(e));
        } catch (RestClientException e) {
            throw new ArchiveApiException(0, "The Archivo Digital API could not be reached: " + e.getMessage());
        }
    }

    private String errorMessage(RestClientResponseException e) {
        try {
            ErrorResponseDTO error = e.getResponseBodyAs(ErrorResponseDTO.class);
            return error != null && error.message() != null ? error.message() : e.getStatusText();
        } catch (RestClientException | HttpMessageConversionException conversionError) {
            return e.getStatusText();
        }
    }
}
