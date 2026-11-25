package ma.ump.deptinfo.backend.services.offresexternes;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * PGDI2026-86 : ne garder que les offres du domaine informatique (dev, IA, data, DevOps, cloud,
 * cybersécurité, réseaux, support IT) et adaptées à un étudiant.
 * Jooble cherche aussi dans le texte : « stage informatique » ramène des stages d'assistant de direction
 * qui demandent seulement « la maîtrise des outils informatiques ». Le titre décide donc en premier.
 */
public final class OfferRelevance {

    private OfferRelevance() {
    }

    /** Métiers de l'informatique dans le titre (FR, EN, ES, DE), texte sans accents et en minuscules. */
    private static final Pattern IT_TITLE = Pattern.compile("\\b("
            + "informatique|informatica|informatik|it|ti|tic|si|"
            + "developp\\w*|developer|desarrollador\\w*|entwickler\\w*|programm\\w*|programador\\w*|"
            + "software|logiciel\\w*|web|full.?stack|back.?end|front.?end|mobile|android|ios|"
            + "data|donnees|datos|bi|business intelligence|big data|"
            + "ia|ai|intelligence artificielle|machine learning|deep learning|ml|llm|nlp|"
            + "devops|devsecops|sre|cloud|kubernetes|docker|"
            + "cyber\\w*|securite informatique|soc|pentest\\w*|"
            + "reseaux? informatiques?|systemes? et reseaux?|reseaux? et (systemes?|telecom\\w*)|systemes? d.information|"
            + "telecom\\w*|network\\w*|sysadmin|infrastructure|administrat\\w* (systemes?|reseaux?|serveurs?)|linux|"
            + "(technicien|ingenieur|stage|stagiaire|alternance) (en )?reseaux?|"
            + "support informatique|helpdesk|help desk|technicien informatique|"
            + "qa|testeur|test automation|erp|sap|odoo|ux|ui|product owner|scrum master|"
            + "java|python|php|javascript|typescript|angular|react|node|spring|laravel|symfony|\\.net|c#|flutter|sql"
            + ")\\b");

    /** Autres métiers : écartés, sauf si le titre cite aussi un métier technique précis (voir DEV_ROLE). */
    private static final Pattern NON_IT_TITLE = Pattern.compile("\\b("
            + "assistant\\w* de direction|direction|secretari\\w*|commercial\\w*|vente|vendeu\\w*|sales|"
            + "comptab\\w*|accountant|audit\\w*|finance|juridique|juriste|achats?|"
            + "ressources humaines|rh|hr|recrutement|recruteu\\w*|recruit\\w*|talent|paie|"
            + "marketing|communication|evenementiel|community manager|reseaux sociaux|"
            + "logistique|transport|chauffeur|magasinier|hotellerie|restauration|tourisme|"
            + "infirmier\\w*|medical|pharmac\\w*|enseignant|professeur|immobilier|assurance|"
            + "mecanique|electrique|electrotechni\\w*|genie civil|btp"
            + ")\\b");

    /** Métier technique sans ambiguïté : garde l'offre même si le titre cite un autre domaine. */
    private static final Pattern DEV_ROLE = Pattern.compile("\\b("
            + "developp\\w*|developer|desarrollador\\w*|software engineer\\w*|ingenieur (logiciel|informatique|data|devops|cloud)|"
            + "data (scientist|engineer|analyst)|machine learning|devops|cyber\\w*|pentest\\w*|sysadmin"
            + ")\\b");

    /** Postes confirmés : la plateforme cible les stages, PFE et premiers emplois. */
    private static final Pattern SENIOR = Pattern.compile("\\b("
            + "senior|sr|lead|tech lead|head of|director|directeur|directrice|manager|responsable d.equipe|cto|"
            + "(5|6|7|8|9|10)\\+? (ans|years) d.experience"
            + ")\\b");

    /** Technologies citées dans un résumé (titre générique comme « Stagiaire PFE »). */
    private static final List<String> TECH = List.of(
            "java", "spring", "python", "django", "php", "laravel", "symfony", "javascript", "typescript", "angular",
            "react", "vue", "node", ".net", "c#", "sql", "mysql", "postgresql", "mongodb", "docker", "kubernetes",
            "linux", "git", "devops", "cloud", "aws", "azure", "api", "microservices", "machine learning", "power bi",
            "flutter", "android", "cybersecurite", "reseau", "cisco");

    public static boolean isComputing(String title, String snippet) {
        String t = " " + normalize(title) + " ";
        if (SENIOR.matcher(t).find()) return false;
        if (NON_IT_TITLE.matcher(t).find()) return DEV_ROLE.matcher(t).find();
        if (IT_TITLE.matcher(t).find()) return true;
        String text = " " + normalize(snippet) + " ";
        long tech = TECH.stream().filter(k -> text.contains(" " + k + " ") || text.contains(" " + k + ",")).count();
        return tech >= 2;
    }

    /** Minuscules sans accents, ponctuation remplacée par des espaces. */
    static String normalize(String text) {
        if (text == null) return "";
        String s = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
        return s.replaceAll("[^a-z0-9+#.]+", " ").replaceAll("\\s+", " ").trim();
    }
}
