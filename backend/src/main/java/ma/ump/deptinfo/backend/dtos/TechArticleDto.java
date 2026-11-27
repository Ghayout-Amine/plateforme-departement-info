package ma.ump.deptinfo.backend.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * PGDI2026-88 : article de veille technologique venu d'une source externe (flux RSS/Atom ou API Dev.to).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TechArticleDto {

    private String title;
    private String summary;          // texte brut, sans HTML, 280 caractères maximum
    private String link;             // lien vers l'article d'origine
    private String imageUrl;         // null si la source n'en fournit pas
    private OffsetDateTime publishedAt;
    private String source;           // ex. « Dev.to », « Le Monde Informatique »
}
