package ma.ump.deptinfo.backend.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import ma.ump.deptinfo.backend.dtos.EnrollmentRequestDTO;
import ma.ump.deptinfo.backend.dtos.EnrollmentResponseDTO;
import ma.ump.deptinfo.backend.services.EnrollmentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API des inscriptions pédagogiques (PGDI2026-60, US26).
 * Réservée à ADMIN et CHEF_DEPARTEMENT (gestion de la scolarité).
 */
@RestController
@RequestMapping("/api/enrollments")
@Tag(name = "Inscriptions", description = "Inscription des étudiants à une filière, une année et un groupe (US26)")
@PreAuthorize("hasAnyRole('ADMIN', 'CHEF_DEPARTEMENT')")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    public EnrollmentController(EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    // GET /api/enrollments?academicYearId=1&filiereId=2&groupeId=3 (filtres facultatifs)
    @GetMapping
    @Operation(summary = "Lister les inscriptions (filtres facultatifs : année, filière, groupe)")
    public ResponseEntity<List<EnrollmentResponseDTO>> getEnrollments(
            @RequestParam(required = false) Long academicYearId,
            @RequestParam(required = false) Long filiereId,
            @RequestParam(required = false) Long groupeId) {
        return ResponseEntity.ok(enrollmentService.getEnrollments(academicYearId, filiereId, groupeId));
    }

    // GET /api/enrollments/{id}
    @GetMapping("/{id}")
    @Operation(summary = "Consulter une inscription")
    public ResponseEntity<EnrollmentResponseDTO> getEnrollmentById(@PathVariable Long id) {
        return ResponseEntity.ok(enrollmentService.getEnrollmentById(id));
    }

    // GET /api/enrollments/student/{studentId}
    @GetMapping("/student/{studentId}")
    @Operation(summary = "Historique des inscriptions d'un étudiant")
    public ResponseEntity<List<EnrollmentResponseDTO>> getEnrollmentsByStudent(@PathVariable Long studentId) {
        return ResponseEntity.ok(enrollmentService.getEnrollmentsByStudent(studentId));
    }

    // POST /api/enrollments
    @PostMapping
    @Operation(summary = "Inscrire un étudiant (filière + année, groupe facultatif)")
    public ResponseEntity<EnrollmentResponseDTO> createEnrollment(@Valid @RequestBody EnrollmentRequestDTO dto) {
        return new ResponseEntity<>(enrollmentService.createEnrollment(dto), HttpStatus.CREATED);
    }

    // PUT /api/enrollments/{id}
    @PutMapping("/{id}")
    @Operation(summary = "Modifier une inscription (changer de groupe, de filière ou de statut)")
    public ResponseEntity<EnrollmentResponseDTO> updateEnrollment(@PathVariable Long id,
                                                                  @Valid @RequestBody EnrollmentRequestDTO dto) {
        return ResponseEntity.ok(enrollmentService.updateEnrollment(id, dto));
    }

    // DELETE /api/enrollments/{id}
    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer une inscription")
    public ResponseEntity<Void> deleteEnrollment(@PathVariable Long id) {
        enrollmentService.deleteEnrollment(id);
        return ResponseEntity.noContent().build();
    }
}
