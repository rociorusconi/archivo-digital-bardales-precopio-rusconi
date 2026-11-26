package ar.edu.unsam.ddso.tp.controller;

import ar.edu.unsam.ddso.tp.dto.DigitizationCreateRequestDTO;
import ar.edu.unsam.ddso.tp.dto.DigitizationResponseDTO;
import ar.edu.unsam.ddso.tp.dto.PieceCreateRequestDTO;
import ar.edu.unsam.ddso.tp.dto.PieceDetailResponseDTO;
import ar.edu.unsam.ddso.tp.dto.PieceResponseDTO;
import ar.edu.unsam.ddso.tp.service.PieceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pieces")
public class PieceController {

    private final PieceService pieceService;

    public PieceController(PieceService pieceService) {
        this.pieceService = pieceService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<PieceDetailResponseDTO> getPiece(@PathVariable Long id) {
        return ResponseEntity.ok(pieceService.getPiece(id));
    }

    @PostMapping
    public ResponseEntity<PieceResponseDTO> createPiece(
            @Valid @RequestBody PieceCreateRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pieceService.createPiece(request));
    }

    @PostMapping("/{id}/digitizations")
    public ResponseEntity<DigitizationResponseDTO> addDigitization(
            @PathVariable Long id, @Valid @RequestBody DigitizationCreateRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(pieceService.addDigitization(id, request));
    }
}