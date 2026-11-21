package ma.ump.deptinfo.backend.entities;

/**
 * État du dossier de stage (colonne internships.status).
 * PGDI2026-80 : l'étudiant déclare (PENDING_VALIDATION), le département valide ou refuse.
 */
public enum InternshipStatus {
    PENDING_VALIDATION, // Déclaré, en attente de la décision du département
    VALIDATED,          // Convention validée
    REJECTED,           // Convention refusée (motif obligatoire)
    IN_PROGRESS,        // Stage en cours
    COMPLETED,          // Stage terminé
    CANCELLED           // Annulé
}
