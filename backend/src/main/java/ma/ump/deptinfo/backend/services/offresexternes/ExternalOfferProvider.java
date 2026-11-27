package ma.ump.deptinfo.backend.services.offresexternes;

import ma.ump.deptinfo.backend.dtos.JobOfferDto;

import java.util.List;

/** PGDI2026-86 : une API externe d'offres d'emploi et de stages (Jooble, JSearch…). */
public interface ExternalOfferProvider {

    /** Nom affiché dans le champ "source" des offres. */
    String name();

    /** Vrai si la clé d'API est renseignée : sinon le fournisseur n'est pas appelé. */
    boolean isConfigured();

    /** Recherche des offres ; lève une exception si l'API ne répond pas ou refuse la requête. */
    List<JobOfferDto> search(String keyword, String location);
}
