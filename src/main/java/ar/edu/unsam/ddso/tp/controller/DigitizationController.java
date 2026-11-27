package ar.edu.unsam.ddso.tp.controller;

import ar.edu.unsam.ddso.tp.config.OpenApiConfig;
import ar.edu.unsam.ddso.tp.dto.DigitizationResponseDTO;
import ar.edu.unsam.ddso.tp.service.DigitizationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/digitizations")
@Tag(name = OpenApiConfig.CATALOGING)
public class DigitizationController {

    private final DigitizationService digitizationService;

    public DigitizationController(DigitizationService digitizationService) {
        this.digitizationService = digitizationService;
    }

    @PostMapping("/{id}/tags/{tagId}")
    public ResponseEntity<DigitizationResponseDTO> addTag(@PathVariable Long id, @PathVariable Long tagId) {
        return ResponseEntity.ok(digitizationService.addTag(id, tagId));
    }

    @DeleteMapping("/{id}/tags/{tagId}")
    public ResponseEntity<DigitizationResponseDTO> removeTag(@PathVariable Long id, @PathVariable Long tagId) {
        return ResponseEntity.ok(digitizationService.removeTag(id, tagId));
    }
}
