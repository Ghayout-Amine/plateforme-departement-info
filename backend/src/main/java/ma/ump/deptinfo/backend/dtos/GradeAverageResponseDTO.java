package ma.ump.deptinfo.backend.dtos;

import java.math.BigDecimal;

public record GradeAverageResponseDTO(
        Long studentId,
        Long moduleId,
        Long academicYearId,
        BigDecimal average
) {
}