package ma.ump.deptinfo.backend.entities;

/**
 * Type d'évaluation (PGDI2026-75, US49), aligné sur la table examens du schéma V1.
 */
public enum ExamType {
    CC,          // Contrôle continu
    TP,          // Évaluation de TP
    FINAL,       // Examen final
    RATTRAPAGE   // Session de rattrapage
}
