package ma.ump.deptinfo.backend.controllers;

import jakarta.validation.Valid;
import ma.ump.deptinfo.backend.dtos.AbsenceJustificationDTO;
import ma.ump.deptinfo.backend.dtos.AbsenceRequestDTO;
import ma.ump.deptinfo.backend.dtos.AbsenceResponseDTO;
import ma.ump.deptinfo.backend.services.AbsenceService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/absences")
public class AbsenceController {

    private final AbsenceService absenceService;

    public AbsenceController(AbsenceService absenceService) {
        this.absenceService = absenceService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ENSEIGNANT')")
    public AbsenceResponseDTO create(
            @Valid @RequestBody AbsenceRequestDTO request,
            Authentication authentication) {

        return absenceService.create(
                request, authentication.getName());
    }

    @PatchMapping("/{id}/justify")
    @PreAuthorize("hasRole('ENSEIGNANT')")
    public AbsenceResponseDTO justify(
            @PathVariable Long id,
            @Valid @RequestBody AbsenceJustificationDTO request,
            Authentication authentication) {

        return absenceService.justify(
                id, request, authentication.getName());
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('ETUDIANT')")
    public List<AbsenceResponseDTO> myAbsences(
            Authentication authentication) {

        return absenceService.getMyAbsences(
                authentication.getName());
    }
}