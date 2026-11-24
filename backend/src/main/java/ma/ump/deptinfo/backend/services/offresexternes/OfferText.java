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

    /**
     * Résumé lisible : Jooble renvoie un extrait coupé (« ...) de Direction pour… … »).
     * On retire la ponctuation orpheline au début et on remplace « ... » par « … ».
     */
    static String snippet(String html, int maxLength) {
        String text = plain(html, maxLength + 20)
                .replaceAll("\\.{3,}", "…")
                .replaceAll("^[\\s…).,;:!?-]+", "")
                .replaceAll("\\s*…\\s*…\\s*", " … ")
                .trim();
        if (text.isEmpty()) return "";
        if (text.length() > maxLength) text = plain(text, maxLength);
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    /** Titre en majuscules (« OFFRE DE STAGE - DATA ») remis en casse normale (« Offre de stage - Data »). */
    static String title(String raw) {
        String t = plain(raw, 200);
        long letters = t.chars().filter(Character::isLetter).count();
        long upper = t.chars().filter(Character::isUpperCase).count();
        if (letters < 6 || upper < letters * 0.8) return t;
        StringBuilder out = new StringBuilder();
        for (String word : t.toLowerCase(java.util.Locale.FRENCH).split(" ")) {
            if (out.length() > 0) out.append(' ');
            // sigles courants gardés en majuscules
            if (word.matches("(pfe|it|ia|ai|ui|ux|rh|bi|qa|sql|php|api|erp|sap|aws|soc|si|h/f|f/h|\\(h/f\\)|\\(f/h\\))")) {
                out.append(word.toUpperCase(java.util.Locale.FRENCH));
            } else {
                out.append(word);
            }
        }
        return Character.toUpperCase(out.charAt(0)) + out.substring(1);
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
