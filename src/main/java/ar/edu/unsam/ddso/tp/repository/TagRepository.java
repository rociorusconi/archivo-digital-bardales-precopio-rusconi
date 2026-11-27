package ar.edu.unsam.ddso.tp.repository;

import ar.edu.unsam.ddso.tp.model.Tag;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TagRepository extends JpaRepository<Tag, Long> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    Optional<Tag> findByNameIgnoreCase(String name);
}
