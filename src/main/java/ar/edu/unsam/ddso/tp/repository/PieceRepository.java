package ar.edu.unsam.ddso.tp.repository;

import ar.edu.unsam.ddso.tp.model.Piece;
import java.time.LocalDate;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PieceRepository extends JpaRepository<Piece, Long> {

    /**
     * A null filter (or no tags) does not restrict. Texts arrive in lowercase. A piece has a tag if
     * any of its digitizations has it, and it must have all the requested tags.
     */
    @Query("""
            SELECT p FROM Piece p
            WHERE (:name IS NULL OR LOWER(p.name) LIKE CONCAT('%', :name, '%'))
              AND (:description IS NULL OR LOWER(p.description) LIKE CONCAT('%', :description, '%'))
              AND (:dateFrom IS NULL OR p.date >= :dateFrom)
              AND (:dateTo IS NULL OR p.date <= :dateTo)
              AND (:tagCount = 0 OR :tagCount = (
                  SELECT COUNT(DISTINCT t.id) FROM Digitization d JOIN d.tags t
                  WHERE d.piece = p AND LOWER(t.name) IN :tags))
            """)
    Page<Piece> search(
            @Param("name") String name,
            @Param("description") String description,
            @Param("dateFrom") LocalDate dateFrom,
            @Param("dateTo") LocalDate dateTo,
            @Param("tags") Set<String> tags,
            @Param("tagCount") long tagCount,
            Pageable pageable);
}
