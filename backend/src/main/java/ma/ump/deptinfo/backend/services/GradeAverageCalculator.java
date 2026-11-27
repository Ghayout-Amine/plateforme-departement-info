package ma.ump.deptinfo.backend.services;

import ma.ump.deptinfo.backend.entities.Grade;
import ma.ump.deptinfo.backend.entities.GradeStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

@Component
public class GradeAverageCalculator {

    public Optional<BigDecimal> calculate(List<Grade> grades) {
        if (grades == null) {
            throw new IllegalArgumentException("La liste des notes est obligatoire");
        }

        BigDecimal weightedSum = BigDecimal.ZERO;
        BigDecimal totalCoefficient = BigDecimal.ZERO;

        for (Grade grade : grades) {
            if (grade == null || grade.getStatus() != GradeStatus.PUBLISHED
                    || grade.getScore() == null) {
                continue;
            }

            BigDecimal coefficient = grade.getCoefficient();

            if (coefficient == null
                    || coefficient.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException(
                        "Coefficient invalide pour une note publiee");
            }

            weightedSum = weightedSum.add(
                    grade.getScore().multiply(coefficient));

            totalCoefficient = totalCoefficient.add(coefficient);
        }

        if (totalCoefficient.signum() == 0) {
            return Optional.empty();
        }

        return Optional.of(weightedSum.divide(
                totalCoefficient, 2, RoundingMode.HALF_UP));
    }
}