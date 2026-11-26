package ar.edu.unsam.ddso.tp.repository;

import ar.edu.unsam.ddso.tp.model.Digitization;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DigitizationRepository extends JpaRepository<Digitization, Long> {

    boolean existsByTagsId(Long tagId);
}
