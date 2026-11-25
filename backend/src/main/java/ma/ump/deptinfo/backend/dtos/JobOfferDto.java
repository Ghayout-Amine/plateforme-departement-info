package ma.ump.deptinfo.backend.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * PGDI2026-86 : offre de stage provenant d'une API externe (Jooble, JSearch).
 * Format renvoyé par GET /api/stages/offres-externes.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobOfferDto implements Serializable {
    private String id;
    private String title;
    private String company;
    private String location;
    private String url;          // lien externe vers l'annonce (candidature sur le site d'origine)
    private String snippet;      // résumé en texte brut (sans HTML)
    private String source;       // "Jooble", "JSearch" ou "Exemple" (aucune clé configurée)
    private String postedDate;   // AAAA-MM-JJ, peut être vide
}
