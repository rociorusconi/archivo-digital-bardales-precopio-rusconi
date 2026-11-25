package ar.edu.unsam.ddso.tp.controller;

import ar.edu.unsam.ddso.tp.dto.PageResponseDTO;
import ar.edu.unsam.ddso.tp.dto.PieceResponseDTO;
import ar.edu.unsam.ddso.tp.dto.PieceSearchRequestDTO;
import ar.edu.unsam.ddso.tp.service.PieceSearchService;
import java.time.LocalDate;
import java.util.Set;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pieces/search")
public class PieceSearchController {

    private final PieceSearchService pieceSearchService;

    public PieceSearchController(PieceSearchService pieceSearchService) {
        this.pieceSearchService = pieceSearchService;
    }

    @GetMapping
    public ResponseEntity<PageResponseDTO<PieceResponseDTO>> search(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) Set<String> tags,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PieceSearchRequestDTO request = new PieceSearchRequestDTO(name, description, dateFrom, dateTo, tags);
        return ResponseEntity.ok(pieceSearchService.search(request, page, size));
    }
}
