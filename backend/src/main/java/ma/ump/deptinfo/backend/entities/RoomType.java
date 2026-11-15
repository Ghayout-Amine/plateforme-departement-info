package ma.ump.deptinfo.backend.entities;

/**
 * Type d'une salle (UML v02, CL_2 Structure pédagogique).
 * Stocké en texte dans la colonne salles.room_type.
 */
public enum RoomType {
    AMPHI,      // Amphithéâtre
    CLASSROOM,  // Salle de cours / TD
    LAB,        // Salle de TP / laboratoire
    OTHER       // Autre (salle de réunion, salle de soutenance...)
}
