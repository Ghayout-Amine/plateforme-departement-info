
package ma.ump.deptinfo.backend.controllers;

import jakarta.validation.Valid;

import ma.ump.deptinfo.backend.dtos.AbsenceJustificationDTO;
import ma.ump.deptinfo.backend.dtos.AbsenceRequestDTO;
import ma.ump.deptinfo.backend.dtos.AbsenceResponseDTO;
import ma.ump.deptinfo.backend.services.AbsenceService;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/absences")
public class AbsenceController {

    private final AbsenceService absenceService;

    public AbsenceController(AbsenceService absenceService) {

        this.absenceService = absenceService;
    }

    // Creation d'une absence par un enseignant.

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ENSEIGNANT')")
    public AbsenceResponseDTO create(
            @Valid @RequestBody AbsenceRequestDTO request,
            Authentication authentication) {

        return absenceService.create(
                request,
                authentication.getName()
        );
    }

    // Justification d'une absence par un enseignant.

    @PatchMapping("/{id}/justify")
    @PreAuthorize("hasRole('ENSEIGNANT')")
    public AbsenceResponseDTO justify(
            @PathVariable Long id,
            @Valid @RequestBody AbsenceJustificationDTO request,
            Authentication authentication) {

        return absenceService.justify(
                id,
                request,
                authentication.getName()
        );
    }

    // Consultation des absences de l'etudiant.

    @GetMapping("/me")
    @PreAuthorize("hasRole('ETUDIANT')")
    public List<AbsenceResponseDTO> myAbsences(
            Authentication authentication) {

        return absenceService.getMyAbsences(
                authentication.getName()
        );
    }

    // PGDI2026-91 : depot d'un justificatif medical PDF.

    @PostMapping(
        value = "/{id}/justificatif",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("hasRole('ETUDIANT')")
    @ResponseStatus(HttpStatus.OK)
    public AbsenceResponseDTO uploadJustificatif(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {

        return absenceService.uploadJustificatif(
                id,
                file,
                authentication.getName()
        );
    }
}
