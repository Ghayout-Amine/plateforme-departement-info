package ma.ump.deptinfo.backend.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ma.ump.deptinfo.backend.entities.EnrollmentStatus;

import java.time.LocalDateTime;

/**
 * Inscription renvoyée par l'API, avec les libellés utiles à l'affichage
 * (nom de l'étudiant, code de la filière, année, nom du groupe).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentResponseDTO {

    private Long id;
    private EnrollmentStatus status;

    private Long studentId;
    private String studentNumber;
    private String studentFirstName;
    private String studentLastName;

    private Long filiereId;
    private String filiereCode;
    private String filiereNom;

    private Long academicYearId;
    private String academicYearLabel;

    private Long groupeId;      // null si pas encore affecté
    private String groupeName;  // null si pas encore affecté

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
