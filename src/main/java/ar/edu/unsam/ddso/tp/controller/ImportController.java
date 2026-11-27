package ar.edu.unsam.ddso.tp.controller;

import ar.edu.unsam.ddso.tp.config.OpenApiConfig;
import ar.edu.unsam.ddso.tp.dto.ImportJobResponseDTO;
import ar.edu.unsam.ddso.tp.service.ImportService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/imports")
@Tag(name = OpenApiConfig.IMPORT)
public class ImportController {

    private final ImportService importService;

    public ImportController(ImportService importService) {
        this.importService = importService;
    }

    /** Starts the import and answers right away (202): the progress is queried with GET. */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportJobResponseDTO> startImport(@RequestParam("file") MultipartFile file) {
        ImportJobResponseDTO job = importService.startImport(file);
        return ResponseEntity.accepted().location(URI.create("/api/imports/" + job.id())).body(job);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ImportJobResponseDTO> getImport(@PathVariable Long id) {
        return ResponseEntity.ok(importService.getImport(id));
    }
}
