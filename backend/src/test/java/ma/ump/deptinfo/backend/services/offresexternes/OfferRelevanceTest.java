package ma.ump.deptinfo.backend.services.offresexternes;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/** PGDI2026-86 : seules les offres du domaine informatique, pour étudiants, sont affichées. */
class OfferRelevanceTest {

    @ParameterizedTest(name = "« {0} » → {1}")
    @CsvSource(delimiter = '|', value = {
            // vraies réponses Jooble pour « stage informatique » au Maroc (24/11/2026)
            "OFFRE DE STAGE - ASSISTANT(E) DE DIRECTION - MARRAKECH | false",
            "OFFRE DE STAGE RÉMUNÉRÉ - CHARGÉ(E) DES RESSOURCES HUMAINES | false",
            "Stage PFE en Développement, Infrastructure & Applicatif (F/H) | true",
            "STAGE PFE - DATA & IA | true",
            "Stage PFE - Chargé(e) de Recrutement IT | false",
            // autres cas
            "Stage développeur Java Spring Boot | true",
            "Stagiaire administrateur systèmes et réseaux | true",
            "Stage cybersécurité - analyste SOC | true",
            "Prácticas desarrollador backend | true",
            "Stage assistant commercial | false",
            "Senior Software Engineer | false",
            "Data Engineer - Finance | true",
            "Stage marketing digital | false"
    })
    void keepsOnlyComputingOffers(String title, boolean expected) {
        assertEquals(expected, OfferRelevance.isComputing(title, ""));
    }

    @Test
    @DisplayName("titre générique : décidé par les technologies citées dans le résumé")
    void genericTitleUsesSnippet() {
        assertTrue(OfferRelevance.isComputing("Stagiaire PFE", "Projet en Java et Angular avec Docker."));
        assertFalse(OfferRelevance.isComputing("Stagiaire PFE", "Maîtrise des outils informatiques (Word, Excel)."));
    }
}
