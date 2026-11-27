package ma.ump.deptinfo.backend.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AbsenceJustificationDTO(
        @NotBlank
        @Size(max = 500)
        String motif
) {
}