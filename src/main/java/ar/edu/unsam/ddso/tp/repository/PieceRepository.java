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
     *
     * <p>PostgreSQL needs to know the type of every parameter, even when it is null (H2 does not).
     * That is why the texts have a CAST and the dates go inside COALESCE(param, p.date), which
     * gives them the type of the column. With no date filter, COALESCE returns p.date itself, so
     * the condition is true (and pieces without a date are kept).
     */
    @Query("""
            SELECT p FROM Piece p
            WHERE (CAST(:name AS String) IS NULL
                   OR LOWER(p.name) LIKE CONCAT('%', CAST(:name AS String), '%'))
              AND (CAST(:description AS String) IS NULL
                   OR LOWER(p.description) LIKE CONCAT('%', CAST(:description AS String), '%'))
              AND (COALESCE(:dateFrom, p.date) IS NULL OR p.date >= COALESCE(:dateFrom, p.date))
              AND (COALESCE(:dateTo, p.date) IS NULL OR p.date <= COALESCE(:dateTo, p.date))
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
