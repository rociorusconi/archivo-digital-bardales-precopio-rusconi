package ar.edu.unsam.ddso.tp.repository;

import ar.edu.unsam.ddso.tp.model.Digitization;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DigitizationRepository extends JpaRepository<Digitization, Long> {

    boolean existsByTagsId(Long tagId);

    boolean existsByPieceIdAndUrl(Long pieceId, String url);

    // Loads the tags in the same query to avoid one extra query per digitization (N+1).
    @EntityGraph(attributePaths = "tags")
    List<Digitization> findByPieceIdOrderByIdAsc(Long pieceId);
}
