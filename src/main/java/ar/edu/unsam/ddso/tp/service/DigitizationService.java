package ar.edu.unsam.ddso.tp.service;

import ar.edu.unsam.ddso.tp.dto.DigitizationResponseDTO;
import ar.edu.unsam.ddso.tp.exception.DigitizationNotFoundException;
import ar.edu.unsam.ddso.tp.exception.TagNotFoundException;
import ar.edu.unsam.ddso.tp.model.Digitization;
import ar.edu.unsam.ddso.tp.model.Tag;
import ar.edu.unsam.ddso.tp.repository.DigitizationRepository;
import ar.edu.unsam.ddso.tp.repository.TagRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DigitizationService {

    private final DigitizationRepository digitizationRepository;
    private final TagRepository tagRepository;

    public DigitizationService(DigitizationRepository digitizationRepository, TagRepository tagRepository) {
        this.digitizationRepository = digitizationRepository;
        this.tagRepository = tagRepository;
    }

    // ---------- Operaciones ----------

    @Transactional
    public DigitizationResponseDTO addTag(Long digitizationId, Long tagId) {
        Digitization digitization = digitizationRepository.findById(digitizationId)
                .orElseThrow(() -> new DigitizationNotFoundException(digitizationId));
        Tag tag = tagRepository.findById(tagId).orElseThrow(() -> new TagNotFoundException(tagId));
        digitization.addTag(tag);
        return DigitizationMapper.toResponse(digitizationRepository.save(digitization));
    }

    @Transactional
    public DigitizationResponseDTO removeTag(Long digitizationId, Long tagId) {
        Digitization digitization = digitizationRepository.findById(digitizationId)
                .orElseThrow(() -> new DigitizationNotFoundException(digitizationId));
        digitization.removeTag(tagId);
        return DigitizationMapper.toResponse(digitizationRepository.save(digitization));
    }
}
