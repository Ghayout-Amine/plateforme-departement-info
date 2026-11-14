package ma.ump.deptinfo.backend.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import ma.ump.deptinfo.backend.dtos.SalleRequestDTO;
import ma.ump.deptinfo.backend.dtos.SalleResponseDTO;
import ma.ump.deptinfo.backend.entities.RoomType;
import ma.ump.deptinfo.backend.services.SalleService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API des salles (PGDI2026-44, US21).
 * Lecture : tout utilisateur connecté (les enseignants et étudiants verront les salles du planning).
 * Écriture : ADMIN et CHEF_DEPARTEMENT.
 */
@RestController
@RequestMapping("/api/salles")
@Tag(name = "Salles", description = "Gestion des salles du département (US21)")
public class SalleController {

    private final SalleService salleService;

    public SalleController(SalleService salleService) {
        this.salleService = salleService;
    }

    // GET /api/salles  |  /api/salles?type=AMPHI  |  /api/salles?active=true
    @GetMapping
    @Operation(summary = "Lister les salles (filtres facultatifs : type, active)")
    public ResponseEntity<List<SalleResponseDTO>> getAllSalles(
            @RequestParam(required = false) RoomType type,
            @RequestParam(required = false) Boolean active) {
        return ResponseEntity.ok(salleService.getAllSalles(type, active));
    }

    // GET /api/salles/{id}
    @GetMapping("/{id}")
    @Operation(summary = "Consulter une salle")
    public ResponseEntity<SalleResponseDTO> getSalleById(@PathVariable Long id) {
        return ResponseEntity.ok(salleService.getSalleById(id));
    }

    // POST /api/salles
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_DEPARTEMENT')")
    @Operation(summary = "Créer une salle (ADMIN, CHEF_DEPARTEMENT)")
    public ResponseEntity<SalleResponseDTO> createSalle(@Valid @RequestBody SalleRequestDTO dto) {
        return new ResponseEntity<>(salleService.createSalle(dto), HttpStatus.CREATED);
    }

    // PUT /api/salles/{id}
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_DEPARTEMENT')")
    @Operation(summary = "Modifier une salle (ADMIN, CHEF_DEPARTEMENT)")
    public ResponseEntity<SalleResponseDTO> updateSalle(@PathVariable Long id,
                                                        @Valid @RequestBody SalleRequestDTO dto) {
        return ResponseEntity.ok(salleService.updateSalle(id, dto));
    }

    // DELETE /api/salles/{id}
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_DEPARTEMENT')")
    @Operation(summary = "Supprimer une salle (ADMIN, CHEF_DEPARTEMENT)")
    public ResponseEntity<Void> deleteSalle(@PathVariable Long id) {
        salleService.deleteSalle(id);
        return ResponseEntity.noContent().build();
    }
}
