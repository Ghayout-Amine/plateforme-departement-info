package ma.ump.deptinfo.backend.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record AbsenceRequestDTO(
        @NotNull Long studentId,
        @NotNull Long moduleId,
        @NotNull LocalDate date,
        @NotBlank @Size(max = 50) String sessionTime,
        @NotNull Boolean justified,
        @Size(max = 500) String motif
) {
}