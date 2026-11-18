package ma.ump.deptinfo.backend.dtos;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ma.ump.deptinfo.backend.entities.EnrollmentStatus;

/**
 * Corps de POST /api/enrollments et PUT /api/enrollments/{id}.
 * Exemple JSON : { "studentId": 1, "filiereId": 2, "academicYearId": 1, "groupeId": 3 }
 * groupeId est facultatif : l'étudiant peut être inscrit avant d'être affecté à un groupe.
 * status est facultatif : ACTIVE par défaut à la création.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentRequestDTO {

    @NotNull(message = "L'étudiant est obligatoire")
    private Long studentId;

    @NotNull(message = "La filière est obligatoire")
    private Long filiereId;

    @NotNull(message = "L'année universitaire est obligatoire")
    private Long academicYearId;

    private Long groupeId;

    private EnrollmentStatus status;
}
