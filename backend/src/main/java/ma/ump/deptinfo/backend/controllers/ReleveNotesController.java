
package ma.ump.deptinfo.backend.controllers;

import ma.ump.deptinfo.backend.dtos.ReleveNotesResponseDTO;
import ma.ump.deptinfo.backend.services.ReleveNotesPdfService;
import ma.ump.deptinfo.backend.services.ReleveNotesService;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
    private final ReleveNotesPdfService releveNotesPdfService;

    public ReleveNotesController(
            ReleveNotesService releveNotesService,
            ReleveNotesPdfService releveNotesPdfService) {
        this.releveNotesService = releveNotesService;
        this.releveNotesPdfService = releveNotesPdfService;
    }

    @GetMapping("/releve/me")
    @PreAuthorize("hasRole('ETUDIANT')")
    public ReleveNotesResponseDTO getMyReleveNotes(
            @RequestParam(defaultValue = "S3") String semester,
            Authentication authentication) {
        return releveNotesService.getMyReleveNotes(
                semester,
                authentication.getName());
    }

    @GetMapping("/releve/me/pdf")
    @PreAuthorize("hasRole('ETUDIANT')")
    public ResponseEntity<byte[]> getMyRelevePdf(
            @RequestParam(defaultValue = "S3") String semester,
            Authentication authentication) {
        ReleveNotesResponseDTO releve = releveNotesService.getMyReleveNotes(
                semester,
                authentication.getName());
        byte[] pdf = releveNotesPdfService.generatePdf(releve);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .header("Content-Disposition", "attachment; filename=\"Releve_Notes_" + releve.semester() + ".pdf\"")
                .body(pdf);
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

    @GetMapping("/releve/{studentId}/pdf")
    @PreAuthorize(
            "hasAnyRole('ADMIN', 'CHEF_DEPARTEMENT', 'ETUDIANT')")
    public ResponseEntity<byte[]> getRelevePdf(
            @PathVariable Long studentId,
            @RequestParam(defaultValue = "S3") String semester,
            Authentication authentication) {
        ReleveNotesResponseDTO releve = releveNotesService.getReleveNotes(
                studentId,
                semester,
                authentication.getName());
        byte[] pdf = releveNotesPdfService.generatePdf(releve);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .header("Content-Disposition", "attachment; filename=\"Releve_Notes_" + releve.studentNumber() + "_" + releve.semester() + ".pdf\"")
                .body(pdf);
    }
}
