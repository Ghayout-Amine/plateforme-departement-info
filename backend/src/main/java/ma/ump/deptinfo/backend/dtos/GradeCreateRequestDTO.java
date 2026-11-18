package ma.ump.deptinfo.backend.dtos;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import ma.ump.deptinfo.backend.entities.GradeType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record GradeCreateRequestDTO(

        @NotNull
        Long studentId,

        @NotNull
        Long moduleId,

        @NotNull
        @DecimalMin("0.00")
        @DecimalMax("20.00")
        @Digits(integer = 2, fraction = 2)
        BigDecimal score,

        @NotNull
        GradeType type,

        @NotNull
        @DecimalMin(value = "0.00", inclusive = false)
        @Digits(integer = 3, fraction = 2)
        BigDecimal coefficient,

        @NotNull
        LocalDate sessionDate

) {
}