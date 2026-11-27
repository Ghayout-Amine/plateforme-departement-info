package ma.ump.deptinfo.backend.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import ma.ump.deptinfo.backend.dtos.PlanningRequestDTO;
import ma.ump.deptinfo.backend.dtos.PlanningResponseDTO;
import ma.ump.deptinfo.backend.services.PlanningService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * API de l'emploi du temps (PGDI2026-75, US36).
 * Lecture : tout utilisateur connecté. Écriture : ADMIN et CHEF_DEPARTEMENT.
 */
@RestController
@RequestMapping("/api/plannings")
@Tag(name = "Emploi du temps", description = "Séances de cours (CM, TD, TP) avec détection des conflits (US36)")
public class PlanningController {

    private final PlanningService planningService;

    public PlanningController(PlanningService planningService) {
        this.planningService = planningService;
    }

    // GET /api/plannings?semaine=2026-11-23&groupeId=1   (ou ?from=...&to=...)
    @GetMapping
    @Operation(summary = "Séances d'une semaine (par défaut la semaine en cours), filtres : groupe, enseignant, salle")
    public ResponseEntity<List<PlanningResponseDTO>> getPlannings(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate semaine,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long groupeId,
            @RequestParam(required = false) Long teacherId,
            @RequestParam(required = false) Long salleId) {
        return ResponseEntity.ok(planningService.getPlannings(semaine, from, to, groupeId, teacherId, salleId));
    }

    // GET /api/plannings/me?semaine=2026-11-23
    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('ETUDIANT', 'ENSEIGNANT')")
    @Operation(summary = "Mon emploi du temps de la semaine (étudiant : son groupe, enseignant : ses cours)")
    public ResponseEntity<List<PlanningResponseDTO>> getMyPlannings(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate semaine,
            Authentication authentication) {
        return ResponseEntity.ok(planningService.getMyPlannings(authentication.getName(), semaine));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulter une séance")
    public ResponseEntity<PlanningResponseDTO> getPlanningById(@PathVariable Long id) {
        return ResponseEntity.ok(planningService.getPlanningById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_DEPARTEMENT')")
    @Operation(summary = "Planifier une séance (409 si la salle, l'enseignant ou le groupe est déjà pris)")
    public ResponseEntity<PlanningResponseDTO> createPlanning(@Valid @RequestBody PlanningRequestDTO dto) {
        return new ResponseEntity<>(planningService.createPlanning(dto), HttpStatus.CREATED);
    }

    // POST /api/plannings/serie : la même séance chaque semaine pendant "weeks" semaines
    @PostMapping("/serie")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_DEPARTEMENT')")
    @Operation(summary = "Planifier une séance hebdomadaire sur plusieurs semaines (tout ou rien)")
    public ResponseEntity<List<PlanningResponseDTO>> createSerie(@Valid @RequestBody PlanningRequestDTO dto) {
        return new ResponseEntity<>(planningService.createSerie(dto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_DEPARTEMENT')")
    @Operation(summary = "Modifier une séance (mêmes contrôles qu'à la création)")
    public ResponseEntity<PlanningResponseDTO> updatePlanning(@PathVariable Long id,
                                                              @Valid @RequestBody PlanningRequestDTO dto) {
        return ResponseEntity.ok(planningService.updatePlanning(id, dto));
    }

    @PatchMapping("/{id}/annuler")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_DEPARTEMENT')")
    @Operation(summary = "Annuler une séance : elle reste visible mais libère le créneau")
    public ResponseEntity<PlanningResponseDTO> cancelPlanning(@PathVariable Long id) {
        return ResponseEntity.ok(planningService.cancelPlanning(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_DEPARTEMENT')")
    @Operation(summary = "Supprimer une séance")
    public ResponseEntity<Void> deletePlanning(@PathVariable Long id) {
        planningService.deletePlanning(id);
        return ResponseEntity.noContent().build();
    }
}
