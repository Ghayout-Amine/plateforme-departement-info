package ma.ump.deptinfo.backend.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Réponse de GET /api/actualites/veille-externe (PGDI2026-88).
 * unavailableSources liste les sources injoignables lors de la dernière mise à jour
 * (timeout, erreur HTTP, flux invalide) : le front peut l'indiquer sans bloquer l'affichage.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TechWatchResponseDTO {

    private List<TechArticleDto> articles;
    private List<String> unavailableSources;
    private OffsetDateTime updatedAt;
}
