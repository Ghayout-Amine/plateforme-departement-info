
package ma.ump.deptinfo.backend.controllers;

import ma.ump.deptinfo.backend.dtos.ReleveNotesResponseDTO;
import ma.ump.deptinfo.backend.services.ReleveNotesService;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notes")
public class ReleveNotesController {

    private final ReleveNotesService releveNotesService;

    public ReleveNotesController(
            ReleveNotesService releveNotesService) {
        this.releveNotesService = releveNotesService;
    }

    @GetMapping("/releve/{studentId}")
    @PreAuthorize(
            "hasAnyRole('ADMIN', 'CHEF_DEPARTEMENT', 'ETUDIANT')")
    public ReleveNotesResponseDTO getReleveNotes(
            @PathVariable Long studentId,
            @RequestParam String semester,
            Authentication authentication) {

        return releveNotesService.getReleveNotes(
                studentId,
                semester,
                authentication.getName());
    }
}
