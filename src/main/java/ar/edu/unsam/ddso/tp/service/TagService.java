package ar.edu.unsam.ddso.tp.service;

import ar.edu.unsam.ddso.tp.dto.TagRequestDTO;
import ar.edu.unsam.ddso.tp.dto.TagResponseDTO;
import ar.edu.unsam.ddso.tp.exception.TagNotFoundException;
import ar.edu.unsam.ddso.tp.model.Tag;
import ar.edu.unsam.ddso.tp.model.exceptions.DuplicateTagException;
import ar.edu.unsam.ddso.tp.model.exceptions.InvalidTagNameException;
import ar.edu.unsam.ddso.tp.repository.TagRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TagService {

    private final TagRepository tagRepository;

    public TagService(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    // ---------- Consultas ----------

    public List<TagResponseDTO> findAll() {
        return tagRepository.findAll().stream().map(this::toResponse).toList();
    }

    public TagResponseDTO findById(Long id) {
        return toResponse(getTag(id));
    }

    // ---------- Operaciones ----------

    @Transactional
    public TagResponseDTO create(TagRequestDTO request) {
        String name = validName(request.name());
        if (tagRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateTagException(name);
        }
        Tag tag = new Tag();
        tag.setName(name);
        return toResponse(tagRepository.save(tag));
    }

    @Transactional
    public TagResponseDTO rename(Long id, TagRequestDTO request) {
        Tag tag = getTag(id);
        String name = validName(request.name());
        if (tagRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new DuplicateTagException(name);
        }
        tag.setName(name);
        return toResponse(tagRepository.save(tag));
    }

    // ---------- Privados ----------

    private Tag getTag(Long id) {
        return tagRepository.findById(id).orElseThrow(() -> new TagNotFoundException(id));
    }

    private String validName(String name) {
        if (name == null || name.isBlank()) {
            throw new InvalidTagNameException();
        }
        return name.trim();
    }

    private TagResponseDTO toResponse(Tag tag) {
        return new TagResponseDTO(tag.getId(), tag.getName());
    }
}
