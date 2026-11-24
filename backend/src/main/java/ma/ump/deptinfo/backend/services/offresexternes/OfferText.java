package ma.ump.deptinfo.backend.services.offresexternes;

import java.util.Map;

/** Petites fonctions communes aux connecteurs : nettoyage du HTML et lecture sûre des réponses JSON. */
final class OfferText {

    private OfferText() {
    }

    /** Texte brut : balises retirées, entités courantes décodées, espaces compactés, longueur limitée. */
    static String plain(String html, int maxLength) {
        if (html == null) return "";
        String text = html.replaceAll("(?s)<[^>]*>", " ")
                .replace("&nbsp;", " ").replace("&amp;", "&").replace("&quot;", "\"")
                .replace("&#39;", "'").replace("&apos;", "'").replace("&lt;", "<").replace("&gt;", ">")
                .replaceAll("&[a-zA-Z]+;", " ")
                .replaceAll("\\s+", " ")
                .trim();
        if (text.length() <= maxLength) return text;
        int cut = text.lastIndexOf(' ', maxLength);
        return text.substring(0, cut > maxLength / 2 ? cut : maxLength).trim() + "…";
    }

    /** Valeur d'un champ JSON en texte ("" si absent ou null). */
    static String str(Map<?, ?> json, String key) {
        Object value = json == null ? null : json.get(key);
        return value == null ? "" : String.valueOf(value).trim();
    }

    /** Date ISO (2026-11-20T08:00:00…) réduite à AAAA-MM-JJ. */
    static String isoDate(String value) {
        if (value == null || value.length() < 10 || !value.substring(0, 10).matches("\\d{4}-\\d{2}-\\d{2}")) return "";
        return value.substring(0, 10);
    }

    /** Lien utilisable : uniquement http(s), pour ne jamais afficher un lien "javascript:" venu d'une API. */
    static boolean isHttpLink(String url) {
        return url != null && url.trim().toLowerCase().matches("https?://\\S+");
    }
}
