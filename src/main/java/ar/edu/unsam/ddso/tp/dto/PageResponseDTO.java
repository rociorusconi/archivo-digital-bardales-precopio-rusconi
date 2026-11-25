package ar.edu.unsam.ddso.tp.dto;

import java.util.List;

public record PageResponseDTO<T>(
        List<T> content, int page, int size, long totalElements, int totalPages) {

    public PageResponseDTO {
        content = List.copyOf(content);
    }
}
