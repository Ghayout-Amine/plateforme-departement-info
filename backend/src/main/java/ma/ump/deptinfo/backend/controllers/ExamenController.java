package ma.ump.deptinfo.backend.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import ma.ump.deptinfo.backend.dtos.ExamenRequestDTO;
import ma.ump.deptinfo.backend.dtos.ExamenResponseDTO;
import ma.ump.deptinfo.backend.services.ExamenService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * API du calendrier des examens (PGDI2026-75, US49).
 * Lecture : tout utilisateur connecté. Écriture : ADMIN et CHEF_DEPARTEMENT.
 */
@RestController
@RequestMapping("/api/examens")
@Tag(name = "Examens", description = "Calendrier des sessions d'examens par salle et par groupe (US49)")
public class ExamenController {

    private final ExamenService examenService;

    public ExamenController(ExamenService examenService) {
        this.examenService = examenService;
    }

    // GET /api/examens?from=2026-12-01&to=2026-12-31&groupeId=1&salleId=1&moduleId=2
    @GetMapping
    @Operation(summary = "Calendrier des examens (filtres facultatifs : période, groupe, module, salle)")
    public ResponseEntity<List<ExamenResponseDTO>> getExamens(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long groupeId,
            @RequestParam(required = false) Long moduleId,
            @RequestParam(required = false) Long salleId) {
        return ResponseEntity.ok(examenService.getExamens(from, to, groupeId, moduleId, salleId));
    }

    // GET /api/examens/me
    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('ETUDIANT', 'ENSEIGNANT')")
    @Operation(summary = "Mes examens (étudiant : son groupe, enseignant : ses modules)")
    public ResponseEntity<List<ExamenResponseDTO>> getMyExamens(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Authentication authentication) {
        return ResponseEntity.ok(examenService.getMyExamens(authentication.getName(), from, to));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulter un examen")
    public ResponseEntity<ExamenResponseDTO> getExamenById(@PathVariable Long id) {
        return ResponseEntity.ok(examenService.getExamenById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_DEPARTEMENT')")
    @Operation(summary = "Planifier un examen (409 si une salle ou le groupe est déjà pris)")
    public ResponseEntity<ExamenResponseDTO> createExamen(@Valid @RequestBody ExamenRequestDTO dto) {
        return new ResponseEntity<>(examenService.createExamen(dto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_DEPARTEMENT')")
    @Operation(summary = "Modifier un examen (mêmes contrôles qu'à la création)")
    public ResponseEntity<ExamenResponseDTO> updateExamen(@PathVariable Long id,
                                                          @Valid @RequestBody ExamenRequestDTO dto) {
        return ResponseEntity.ok(examenService.updateExamen(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_DEPARTEMENT')")
    @Operation(summary = "Supprimer un examen")
    public ResponseEntity<Void> deleteExamen(@PathVariable Long id) {
        examenService.deleteExamen(id);
        return ResponseEntity.noContent().build();
    }
}
