package ma.ump.deptinfo.backend.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import ma.ump.deptinfo.backend.dtos.InternshipRequestDTO;
import ma.ump.deptinfo.backend.dtos.InternshipResponseDTO;
import ma.ump.deptinfo.backend.dtos.InternshipValidationDTO;
import ma.ump.deptinfo.backend.dtos.SupervisorAssignmentDTO;
import ma.ump.deptinfo.backend.entities.InternshipStatus;
import ma.ump.deptinfo.backend.services.InternshipService;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * API des stages et conventions PFE (PGDI2026-80, US66/US21).
 * L'étudiant déclare son stage ; ADMIN et CHEF_DEPARTEMENT valident ou refusent.
 */
@RestController
@RequestMapping("/api/internships")
@Tag(name = "Stages / PFE", description = "Déclaration des stages, dépôt de la convention PDF et validation par le département (US66/US21)")
public class InternshipController {

    private final InternshipService internshipService;

    public InternshipController(InternshipService internshipService) {
        this.internshipService = internshipService;
    }

    // POST /api/internships/me (multipart : data = JSON, file = convention PDF)
    @PostMapping(value = "/me", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ETUDIANT')")
    @Operation(summary = "Déclarer mon stage/PFE avec la convention (PDF)")
    public ResponseEntity<InternshipResponseDTO> declare(
            @Valid @RequestPart("data") InternshipRequestDTO dto,
            @RequestPart("file") MultipartFile file,
            Authentication authentication) throws IOException {
        return new ResponseEntity<>(
                internshipService.declare(authentication.getName(), dto, file), HttpStatus.CREATED);
    }

    // GET /api/internships/me
    @GetMapping("/me")
    @PreAuthorize("hasRole('ETUDIANT')")
    @Operation(summary = "Mes stages/PFE et l'état de validation")
    public ResponseEntity<List<InternshipResponseDTO>> getMine(Authentication authentication) {
        return ResponseEntity.ok(internshipService.getMyInternships(authentication.getName()));
    }

    // GET /api/internships?status=PENDING_VALIDATION
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_DEPARTEMENT')")
    @Operation(summary = "Lister les stages (filtre de statut facultatif)")
    public ResponseEntity<List<InternshipResponseDTO>> getAll(
            @RequestParam(required = false) InternshipStatus status) {
        return ResponseEntity.ok(internshipService.getInternships(status));
    }

    // GET /api/internships/{id}
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_DEPARTEMENT', 'ETUDIANT')")
    @Operation(summary = "Consulter un stage (propriétaire ou département)")
    public ResponseEntity<InternshipResponseDTO> getById(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(internshipService.getInternshipById(id, authentication.getName()));
    }

    // GET /api/internships/{id}/agreement
    @GetMapping("/{id}/agreement")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_DEPARTEMENT', 'ETUDIANT')")
    @Operation(summary = "Télécharger la convention PDF (propriétaire ou département)")
    public ResponseEntity<byte[]> getAgreement(@PathVariable Long id, Authentication authentication)
            throws IOException {
        byte[] pdf = internshipService.getAgreement(id, authentication.getName());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .header("Content-Disposition", "inline; filename=\"convention-" + id + ".pdf\"")
                .body(pdf);
    }

    // PUT /api/internships/{id}/validation
    @PutMapping("/{id}/validation")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_DEPARTEMENT')")
    @Operation(summary = "Valider ou refuser une convention (motif obligatoire en cas de refus)")
    public ResponseEntity<InternshipResponseDTO> validate(
            @PathVariable Long id,
            @Valid @RequestBody InternshipValidationDTO dto,
            Authentication authentication) {
        return ResponseEntity.ok(internshipService.validate(id, dto, authentication.getName()));
    }

    // PUT /api/internships/{id}/supervisor
    @PutMapping("/{id}/supervisor")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_DEPARTEMENT')")
    @Operation(summary = "Affecter l'encadrant académique (la convention validée passe en cours)")
    public ResponseEntity<InternshipResponseDTO> assignSupervisor(
            @PathVariable Long id,
            @Valid @RequestBody SupervisorAssignmentDTO dto) {
        return ResponseEntity.ok(internshipService.assignSupervisor(id, dto));
    }
}
