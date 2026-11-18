package ma.ump.deptinfo.backend.dtos;

import ma.ump.deptinfo.backend.entities.GradeStatus;
import ma.ump.deptinfo.backend.entities.GradeType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record GradeResponseDTO(
        Long id,
        Long studentId,
        Long moduleId,
        BigDecimal score,
        GradeType type,
        BigDecimal coefficient,
        LocalDate sessionDate,
        GradeStatus status
) {
}