package ma.ump.deptinfo.backend.services.veille;

import ma.ump.deptinfo.backend.dtos.TechArticleDto;

import java.util.List;

/**
 * PGDI2026-88 : une source de veille technologique (flux RSS/Atom, API Dev.to...).
 * fetch() lève une exception si la source est injoignable ; le service la gère.
 */
public interface TechFeedSource {

    /** Nom affiché de la source, ex. « Dev.to ». */
    String name();

    List<TechArticleDto> fetch();
}
