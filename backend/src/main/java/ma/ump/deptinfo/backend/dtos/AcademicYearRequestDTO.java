package ma.ump.deptinfo.backend.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Corps de POST /api/academic-years.
 * Exemple JSON : { "label": "2026-2027", "startDate": "2026-09-01", "endDate": "2027-07-15", "current": true }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AcademicYearRequestDTO {

    @NotBlank(message = "Le libellé est obligatoire")
    @Pattern(regexp = "\\d{4}-\\d{4}", message = "Le libellé doit être au format AAAA-AAAA, ex. 2026-2027")
    private String label;

    @NotNull(message = "La date de début est obligatoire")
    private LocalDate startDate;

    @NotNull(message = "La date de fin est obligatoire")
    private LocalDate endDate;

    // true = devient l'année en cours (l'ancienne année en cours ne l'est plus)
    private boolean current;
}
