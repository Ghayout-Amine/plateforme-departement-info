package ma.ump.deptinfo.backend.services;

import ma.ump.deptinfo.backend.entities.Grade;
import ma.ump.deptinfo.backend.entities.GradeStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class GradeAverageCalculatorTest {

    private final GradeAverageCalculator calculator =
            new GradeAverageCalculator();

    private Grade grade(String score, String coefficient,
                        GradeStatus status) {
        Grade grade = new Grade();
        grade.setScore(score == null ? null : new BigDecimal(score));
        grade.setCoefficient(new BigDecimal(coefficient));
        grade.setStatus(status);
        return grade;
    }

    @Test
    void shouldCalculateWeightedAverage() {
        List<Grade> grades = List.of(
                grade("14.00", "1.00", GradeStatus.PUBLISHED),
                grade("16.00", "1.00", GradeStatus.PUBLISHED),
                grade("12.00", "2.00", GradeStatus.PUBLISHED)
        );

        Optional<BigDecimal> result = calculator.calculate(grades);

        assertTrue(result.isPresent());
        assertEquals(0,
                new BigDecimal("13.50").compareTo(result.get()));
    }

    @Test
    void shouldIgnoreDraftAndAbsentGrades() {
        List<Grade> grades = List.of(
                grade("14.00", "1.00", GradeStatus.PUBLISHED),
                grade("18.00", "1.00", GradeStatus.DRAFT),
                grade(null, "1.00", GradeStatus.ABSENT)
        );

        Optional<BigDecimal> result = calculator.calculate(grades);

        assertTrue(result.isPresent());
        assertEquals(0,
                new BigDecimal("14.00").compareTo(result.get()));
    }

    @Test
    void shouldReturnEmptyForNoPublishedGrades() {
        assertEquals(Optional.empty(), calculator.calculate(List.of()));

        assertEquals(Optional.empty(), calculator.calculate(List.of(
                grade("12.00", "1.00", GradeStatus.DRAFT)
        )));
    }

    @Test
    void shouldRejectInvalidCoefficient() {
        List<Grade> grades = List.of(
                grade("15.00", "0.00", GradeStatus.PUBLISHED)
        );

        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculate(grades));
    }
}