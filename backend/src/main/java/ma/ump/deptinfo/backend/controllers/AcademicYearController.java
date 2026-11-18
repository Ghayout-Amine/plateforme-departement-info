package ma.ump.deptinfo.backend.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import ma.ump.deptinfo.backend.dtos.AcademicYearRequestDTO;
import ma.ump.deptinfo.backend.dtos.AcademicYearResponseDTO;
import ma.ump.deptinfo.backend.services.AcademicYearService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API des années universitaires (PGDI2026-60).
 * Lecture : tout utilisateur connecté. Écriture : ADMIN et CHEF_DEPARTEMENT.
 */
@RestController
@RequestMapping("/api/academic-years")
@Tag(name = "Années universitaires", description = "Années universitaires utilisées par les inscriptions")
public class AcademicYearController {

    private final AcademicYearService academicYearService;

    public AcademicYearController(AcademicYearService academicYearService) {
        this.academicYearService = academicYearService;
    }

    // GET /api/academic-years
    @GetMapping
    @Operation(summary = "Lister les années universitaires (la plus récente d'abord)")
    public ResponseEntity<List<AcademicYearResponseDTO>> getAll() {
        return ResponseEntity.ok(academicYearService.getAll());
    }

    // GET /api/academic-years/current
    @GetMapping("/current")
    @Operation(summary = "Année universitaire en cours")
    public ResponseEntity<AcademicYearResponseDTO> getCurrent() {
        return ResponseEntity.ok(academicYearService.getCurrent());
    }

    // POST /api/academic-years
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_DEPARTEMENT')")
    @Operation(summary = "Créer une année universitaire (ADMIN, CHEF_DEPARTEMENT)")
    public ResponseEntity<AcademicYearResponseDTO> create(@Valid @RequestBody AcademicYearRequestDTO dto) {
        return new ResponseEntity<>(academicYearService.create(dto), HttpStatus.CREATED);
    }

    // PUT /api/academic-years/{id}/current
    @PutMapping("/{id}/current")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_DEPARTEMENT')")
    @Operation(summary = "Définir l'année en cours (ADMIN, CHEF_DEPARTEMENT)")
    public ResponseEntity<AcademicYearResponseDTO> setCurrent(@PathVariable Long id) {
        return ResponseEntity.ok(academicYearService.setCurrent(id));
    }
}
