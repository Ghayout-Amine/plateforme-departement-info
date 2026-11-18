package ma.ump.deptinfo.backend.entities;

/**
 * Situation de l'étudiant dans son inscription (colonne enrollments.status).
 */
public enum EnrollmentStatus {
    ACTIVE,      // Inscrit, année en cours
    VALIDATED,   // Année validée
    REPEATING,   // Redoublant
    SUSPENDED,   // Inscription suspendue
    WITHDRAWN    // Abandon / désinscription
}
