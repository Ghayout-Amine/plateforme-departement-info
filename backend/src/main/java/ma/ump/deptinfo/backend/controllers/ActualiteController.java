package ma.ump.deptinfo.backend.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import ma.ump.deptinfo.backend.dtos.AnnouncementRequestDTO;
import ma.ump.deptinfo.backend.dtos.AnnouncementResponseDTO;
import ma.ump.deptinfo.backend.dtos.TechWatchResponseDTO;
import ma.ump.deptinfo.backend.services.AnnouncementService;
import ma.ump.deptinfo.backend.services.veille.TechWatchService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Actualités du département (PGDI2026-88, PGDI2026-89).
 * Lecture publique (GET), gestion des annonces réservée à ADMIN et CHEF_DEPARTEMENT.
 */
@RestController
@RequestMapping("/api/actualites")
@Tag(name = "Actualités", description = "Annonces officielles du département et veille technologique externe")
public class ActualiteController {

    private final AnnouncementService announcementService;
    private final TechWatchService techWatchService;

    public ActualiteController(AnnouncementService announcementService, TechWatchService techWatchService) {
        this.announcementService = announcementService;
        this.techWatchService = techWatchService;
    }

    // ---------- PGDI2026-89 : annonces officielles ----------

    // GET /api/actualites/departement (épinglées d'abord, puis les plus récentes)
    @GetMapping("/departement")
    @Operation(summary = "Annonces officielles du département (épinglées en tête)")
    public ResponseEntity<List<AnnouncementResponseDTO>> getAnnouncements() {
        return ResponseEntity.ok(announcementService.getAll());
    }

    @GetMapping("/departement/{id}")
    @Operation(summary = "Consulter une annonce")
    public ResponseEntity<AnnouncementResponseDTO> getAnnouncement(@PathVariable Long id) {
        return ResponseEntity.ok(announcementService.getById(id));
    }

    @PostMapping("/departement")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_DEPARTEMENT')")
    @Operation(summary = "Publier une annonce (ADMIN, CHEF_DEPARTEMENT)")
    public ResponseEntity<AnnouncementResponseDTO> createAnnouncement(@Valid @RequestBody AnnouncementRequestDTO dto,
                                                                      Authentication authentication) {
        return new ResponseEntity<>(announcementService.create(dto, authentication.getName()), HttpStatus.CREATED);
    }

    @PutMapping("/departement/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_DEPARTEMENT')")
    @Operation(summary = "Modifier une annonce (ADMIN, CHEF_DEPARTEMENT)")
    public ResponseEntity<AnnouncementResponseDTO> updateAnnouncement(@PathVariable Long id,
                                                                      @Valid @RequestBody AnnouncementRequestDTO dto) {
        return ResponseEntity.ok(announcementService.update(id, dto));
    }

    // PATCH /api/actualites/departement/{id}/epingler : épingle ou désépingle
    @PatchMapping("/departement/{id}/epingler")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_DEPARTEMENT')")
    @Operation(summary = "Épingler / désépingler une annonce (ADMIN, CHEF_DEPARTEMENT)")
    public ResponseEntity<AnnouncementResponseDTO> togglePin(@PathVariable Long id) {
        return ResponseEntity.ok(announcementService.togglePin(id));
    }

    @DeleteMapping("/departement/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_DEPARTEMENT')")
    @Operation(summary = "Supprimer une annonce (ADMIN, CHEF_DEPARTEMENT)")
    public ResponseEntity<Void> deleteAnnouncement(@PathVariable Long id) {
        announcementService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ---------- PGDI2026-88 : veille technologique ----------

    // GET /api/actualites/veille-externe?limit=20
    @GetMapping("/veille-externe")
    @Operation(summary = "Veille technologique (Dev.to et flux RSS), mise en cache 30 min")
    public ResponseEntity<TechWatchResponseDTO> getTechWatch(@RequestParam(required = false) Integer limit) {
        return ResponseEntity.ok(techWatchService.getArticles(limit));
    }
}
