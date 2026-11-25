package ar.edu.unsam.ddso.tp.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import ar.edu.unsam.ddso.tp.dto.TagRequestDTO;
import ar.edu.unsam.ddso.tp.dto.TagResponseDTO;
import ar.edu.unsam.ddso.tp.exception.TagNotFoundException;
import ar.edu.unsam.ddso.tp.model.Tag;
import ar.edu.unsam.ddso.tp.model.exceptions.DuplicateTagException;
import ar.edu.unsam.ddso.tp.model.exceptions.InvalidTagNameException;
import ar.edu.unsam.ddso.tp.repository.TagRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TagServiceTest {

    @Mock private TagRepository tagRepository;

    private TagService tagService;

    @BeforeEach
    void setUp() {
        tagService = new TagService(tagRepository);
    }

    @Test
    void createSavesTagWithTrimmedName() {
        // Given
        when(tagRepository.existsByNameIgnoreCase("photo")).thenReturn(false);
        when(tagRepository.save(any(Tag.class))).thenReturn(new Tag(1L, "photo"));

        // When
        TagResponseDTO result = tagService.create(new TagRequestDTO("  photo  "));

        // Then
        assertEquals(1L, result.id());
        assertEquals("photo", result.name());
        verify(tagRepository).save(any(Tag.class));
    }

    @Test
    void createWithBlankNameFails() {
        // When / Then
        assertThrows(InvalidTagNameException.class, () -> tagService.create(new TagRequestDTO(" ")));
        verify(tagRepository, never()).save(any());
    }

    @Test
    void createWithDuplicateNameFails() {
        // Given
        when(tagRepository.existsByNameIgnoreCase("photo")).thenReturn(true);

        // When / Then
        assertThrows(DuplicateTagException.class, () -> tagService.create(new TagRequestDTO("photo")));
        verify(tagRepository, never()).save(any());
    }

    @Test
    void renameChangesName() {
        // Given
        Tag tag = new Tag(1L, "photo");
        when(tagRepository.findById(1L)).thenReturn(Optional.of(tag));
        when(tagRepository.existsByNameIgnoreCaseAndIdNot("photography", 1L)).thenReturn(false);
        when(tagRepository.save(tag)).thenReturn(tag);

        // When
        TagResponseDTO result = tagService.rename(1L, new TagRequestDTO("photography"));

        // Then
        assertEquals("photography", result.name());
    }

    @Test
    void renameToNameUsedByAnotherTagFails() {
        // Given
        when(tagRepository.findById(1L)).thenReturn(Optional.of(new Tag(1L, "photo")));
        when(tagRepository.existsByNameIgnoreCaseAndIdNot("press", 1L)).thenReturn(true);

        // When / Then
        assertThrows(DuplicateTagException.class, () -> tagService.rename(1L, new TagRequestDTO("press")));
        verify(tagRepository, never()).save(any());
    }

    @Test
    void renameNonExistentTagFails() {
        // Given
        when(tagRepository.findById(99L)).thenReturn(Optional.empty());

        // When / Then
        assertThrows(TagNotFoundException.class, () -> tagService.rename(99L, new TagRequestDTO("photo")));
    }

    @Test
    void findAllReturnsAllTags() {
        // Given
        when(tagRepository.findAll()).thenReturn(List.of(new Tag(1L, "photo"), new Tag(2L, "press")));

        // When
        List<TagResponseDTO> result = tagService.findAll();

        // Then
        assertEquals(2, result.size());
        assertEquals("press", result.get(1).name());
    }
}
