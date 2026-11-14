package ma.ump.deptinfo.backend.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import ma.ump.deptinfo.backend.dtos.GroupeRequestDTO;
import ma.ump.deptinfo.backend.dtos.GroupeResponseDTO;
import ma.ump.deptinfo.backend.services.GroupeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API des groupes (PGDI2026-44, US22).
 * Lecture : tout utilisateur connecté. Écriture : ADMIN et CHEF_DEPARTEMENT.
 */
@RestController
@RequestMapping("/api/groupes")
@Tag(name = "Groupes", description = "Gestion des groupes d'étudiants par filière (US22)")
public class GroupeController {

    private final GroupeService groupeService;

    public GroupeController(GroupeService groupeService) {
        this.groupeService = groupeService;
    }

    // GET /api/groupes  |  GET /api/groupes?filiereId=1
    @GetMapping
    @Operation(summary = "Lister les groupes (filtre facultatif : filiereId)")
    public ResponseEntity<List<GroupeResponseDTO>> getAllGroupes(
            @RequestParam(required = false) Long filiereId) {
        return ResponseEntity.ok(groupeService.getAllGroupes(filiereId));
    }

    // GET /api/groupes/filiere/{filiereId} (même convention que /api/modules/filiere/{id})
    @GetMapping("/filiere/{filiereId}")
    @Operation(summary = "Lister les groupes d'une filière")
    public ResponseEntity<List<GroupeResponseDTO>> getGroupesByFiliere(@PathVariable Long filiereId) {
        return ResponseEntity.ok(groupeService.getAllGroupes(filiereId));
    }

    // GET /api/groupes/{id}
    @GetMapping("/{id}")
    @Operation(summary = "Consulter un groupe")
    public ResponseEntity<GroupeResponseDTO> getGroupeById(@PathVariable Long id) {
        return ResponseEntity.ok(groupeService.getGroupeById(id));
    }

    // POST /api/groupes
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_DEPARTEMENT')")
    @Operation(summary = "Créer un groupe dans une filière (ADMIN, CHEF_DEPARTEMENT)")
    public ResponseEntity<GroupeResponseDTO> createGroupe(@Valid @RequestBody GroupeRequestDTO dto) {
        return new ResponseEntity<>(groupeService.createGroupe(dto), HttpStatus.CREATED);
    }

    // PUT /api/groupes/{id}
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_DEPARTEMENT')")
    @Operation(summary = "Modifier un groupe (ADMIN, CHEF_DEPARTEMENT)")
    public ResponseEntity<GroupeResponseDTO> updateGroupe(@PathVariable Long id,
                                                          @Valid @RequestBody GroupeRequestDTO dto) {
        return ResponseEntity.ok(groupeService.updateGroupe(id, dto));
    }

    // DELETE /api/groupes/{id}
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_DEPARTEMENT')")
    @Operation(summary = "Supprimer un groupe (ADMIN, CHEF_DEPARTEMENT)")
    public ResponseEntity<Void> deleteGroupe(@PathVariable Long id) {
        groupeService.deleteGroupe(id);
        return ResponseEntity.noContent().build();
    }
}
