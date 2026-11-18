package ma.ump.deptinfo.backend.dtos;

import java.time.LocalDate;

public record AbsenceResponseDTO(
        Long id,
        Long studentId,
        Long moduleId,
        LocalDate date,
        String sessionTime,
        Boolean justified,
        String motif
) {
}