package ma.ump.deptinfo.backend.entities;

/**
 * État d'une séance (PGDI2026-75, US36).
 * Une séance annulée reste dans l'historique mais ne bloque plus la salle,
 * l'enseignant ni le groupe.
 */
public enum PlanningStatus {
    PLANNED,    // Séance prévue
    CANCELLED   // Séance annulée
}
