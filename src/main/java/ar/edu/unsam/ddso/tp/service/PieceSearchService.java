package ar.edu.unsam.ddso.tp.service;

import ar.edu.unsam.ddso.tp.dto.PageResponseDTO;
import ar.edu.unsam.ddso.tp.dto.PieceResponseDTO;
import ar.edu.unsam.ddso.tp.dto.PieceSearchRequestDTO;
import ar.edu.unsam.ddso.tp.model.Piece;
import ar.edu.unsam.ddso.tp.model.PieceSearchCriteria;
import ar.edu.unsam.ddso.tp.repository.PieceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PieceSearchService {

    static final int MAX_PAGE_SIZE = 50;

    private final PieceRepository pieceRepository;

    public PieceSearchService(PieceRepository pieceRepository) {
        this.pieceRepository = pieceRepository;
    }

    // ---------- Consultas ----------

    @Transactional(readOnly = true)
    public PageResponseDTO<PieceResponseDTO> search(PieceSearchRequestDTO request, int page, int size) {
        PieceSearchCriteria criteria = new PieceSearchCriteria(
                request.name(), request.description(), request.dateFrom(), request.dateTo(), request.tags());
        Page<Piece> pieces = pieceRepository.search(
                criteria.name(),
                criteria.description(),
                criteria.dateFrom(),
                criteria.dateTo(),
                criteria.tags(),
                criteria.tags().size(),
                toPageable(page, size));
        return toResponse(pieces);
    }

    // ---------- Privados ----------

    private Pageable toPageable(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.clamp(size, 1, MAX_PAGE_SIZE);
        return PageRequest.of(safePage, safeSize, Sort.by(Sort.Order.desc("date"), Sort.Order.asc("id")));
    }

    private PageResponseDTO<PieceResponseDTO> toResponse(Page<Piece> pieces) {
        return new PageResponseDTO<>(
                pieces.getContent().stream().map(this::toResponse).toList(),
                pieces.getNumber(),
                pieces.getSize(),
                pieces.getTotalElements(),
                pieces.getTotalPages());
    }

    private PieceResponseDTO toResponse(Piece piece) {
        return new PieceResponseDTO(piece.getId(), piece.getName(), piece.getDescription(), piece.getDate());
    }
}
