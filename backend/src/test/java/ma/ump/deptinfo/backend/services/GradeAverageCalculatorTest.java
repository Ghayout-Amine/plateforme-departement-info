
package ma.ump.deptinfo.backend.services;

import ma.ump.deptinfo.backend.entities.Grade;
import ma.ump.deptinfo.backend.entities.GradeStatus;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class GradeAverageCalculatorTest {

    private final GradeAverageCalculator calculator =
            new GradeAverageCalculator();

    private Grade grade(
            String score,
            String coefficient,
            GradeStatus status) {

        Grade grade = new Grade();

        grade.setScore(
                score == null
                        ? null
                        : new BigDecimal(score));

        grade.setCoefficient(
                coefficient == null
                        ? null
                        : new BigDecimal(coefficient));

        grade.setStatus(status);

        return grade;
    }

    // Test 1 : Calcul de la moyenne ponderee.

    @Test
    void shouldCalculateWeightedAverage() {

        List<Grade> grades = List.of(
                grade("14.00", "1.00", GradeStatus.PUBLISHED),
                grade("16.00", "1.00", GradeStatus.PUBLISHED),
                grade("12.00", "2.00", GradeStatus.PUBLISHED)
        );

        Optional<BigDecimal> result =
                calculator.calculate(grades);

        assertTrue(result.isPresent());

        assertEquals(0,
                new BigDecimal("13.50")
                        .compareTo(result.get()));
    }

    // Test 2 : Ignorer les notes brouillons et absences.

    @Test
    void shouldIgnoreDraftAndAbsentGrades() {

        List<Grade> grades = List.of(
                grade("14.00", "1.00", GradeStatus.PUBLISHED),
                grade("18.00", "1.00", GradeStatus.DRAFT),
                grade(null, "1.00", GradeStatus.ABSENT)
        );

        Optional<BigDecimal> result =
                calculator.calculate(grades);

        assertTrue(result.isPresent());

        assertEquals(0,
                new BigDecimal("14.00")
                        .compareTo(result.get()));
    }

    // Test 3 : Aucune note publiee.

    @Test
    void shouldReturnEmptyForNoPublishedGrades() {

        assertEquals(
                Optional.empty(),
                calculator.calculate(List.of()));

        assertEquals(
                Optional.empty(),
                calculator.calculate(List.of(
                        grade("12.00", "1.00", GradeStatus.DRAFT)
                )));
    }

    // Test 4 : Coefficient egal a zero.

    @Test
    void shouldRejectInvalidCoefficient() {

        List<Grade> grades = List.of(
                grade("15.00", "0.00", GradeStatus.PUBLISHED)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> calculator.calculate(grades));
    }

    // Test 5 : Liste des notes null.

    @Test
    void shouldRejectNullGradeList() {

        assertThrows(
                IllegalArgumentException.class,
                () -> calculator.calculate(null));
    }

    // Test 6 : Ignorer les entrees null.

    @Test
    void shouldIgnoreNullGradeEntries() {

        List<Grade> grades = Arrays.asList(
                null,
                grade("14.00", "1.00", GradeStatus.PUBLISHED),
                null
        );

        Optional<BigDecimal> result =
                calculator.calculate(grades);

        assertTrue(result.isPresent());

        assertEquals(0,
                new BigDecimal("14.00")
                        .compareTo(result.get()));
    }

    // Test 7 : Ignorer les notes publiees sans score.

    @Test
    void shouldIgnorePublishedGradesWithoutScore() {

        List<Grade> grades = List.of(
                grade(null, "1.00", GradeStatus.PUBLISHED),
                grade("16.00", "2.00", GradeStatus.PUBLISHED)
        );

        Optional<BigDecimal> result =
                calculator.calculate(grades);

        assertTrue(result.isPresent());

        assertEquals(0,
                new BigDecimal("16.00")
                        .compareTo(result.get()));

        assertEquals(
                Optional.empty(),
                calculator.calculate(List.of(
                        grade(null, "1.00", GradeStatus.PUBLISHED)
                )));
    }

    // Test 8 : Coefficients null et negatif.

    @Test
    void shouldRejectNullAndNegativeCoefficients() {

        assertThrows(
                IllegalArgumentException.class,
                () -> calculator.calculate(List.of(
                        grade("15.00", null, GradeStatus.PUBLISHED)
                )));

        assertThrows(
                IllegalArgumentException.class,
                () -> calculator.calculate(List.of(
                        grade("15.00", "-1.00", GradeStatus.PUBLISHED)
                )));
    }

    // Test 9 : Arrondi HALF_UP a deux decimales.

    @Test
    void shouldRoundWeightedAverageToTwoDecimals() {

        List<Grade> grades = List.of(
                grade("10.00", "1.00", GradeStatus.PUBLISHED),
                grade("10.01", "1.00", GradeStatus.PUBLISHED)
        );

        Optional<BigDecimal> result =
                calculator.calculate(grades);

        assertTrue(result.isPresent());

        assertEquals(
                new BigDecimal("10.01"),
                result.get());

        assertEquals(2, result.get().scale());
    }
}
