package ma.ump.deptinfo.backend.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ma.ump.deptinfo.backend.entities.AnnouncementPriority;

import java.time.LocalDateTime;

/**
 * Corps de POST et PUT /api/actualites/departement (PGDI2026-89).
 * Exemple : { "title": "Soutenances PFE", "content": "Les soutenances auront lieu...", "priority": "IMPORTANTE", "pinned": true }
 * priority : NORMALE par défaut. publishedAt : maintenant si absent.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnnouncementRequestDTO {

    @NotBlank(message = "Le titre est obligatoire")
    @Size(max = 200, message = "Le titre est limité à 200 caractères")
    private String title;

    @NotBlank(message = "Le contenu est obligatoire")
    @Size(max = 5000, message = "Le contenu est limité à 5000 caractères")
    private String content;

    private AnnouncementPriority priority;

    private boolean pinned;

    private LocalDateTime publishedAt;
}
