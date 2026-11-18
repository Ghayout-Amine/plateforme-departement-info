package ma.ump.deptinfo.backend.services;

import ma.ump.deptinfo.backend.entities.*;
import ma.ump.deptinfo.backend.exceptions.BadRequestException;
import ma.ump.deptinfo.backend.repositories.AcademicYearRepository;
import ma.ump.deptinfo.backend.repositories.EnrollmentRepository;
import ma.ump.deptinfo.backend.repositories.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnrollmentEligibilityValidatorTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private AcademicYearRepository academicYearRepository;

    @InjectMocks
    private EnrollmentEligibilityValidator validator;

    private ModuleCours module;
    private AcademicYear year;
    private Enrollment enrollment;
    private LocalDate evaluationDate;

    @BeforeEach
    void setUp() {
        Filiere filiere = new Filiere();
        filiere.setId(10L);

        module = new ModuleCours();
        module.setId(20L);
        module.setFiliere(filiere);

        year = new AcademicYear();
        org.springframework.test.util.ReflectionTestUtils.setField(year, "id", 30L);
        year.setStartDate(LocalDate.of(2026, 9, 1));
        year.setEndDate(LocalDate.of(2027, 8, 31));

        enrollment = new Enrollment();
        enrollment.setFiliere(filiere);
        enrollment.setStatus(EnrollmentStatus.ACTIVE);

        evaluationDate = LocalDate.of(2026, 11, 18);
    }

    private void mockValidEnrollmentLookup() {
        when(studentRepository.existsById(1L)).thenReturn(true);

        when(academicYearRepository.findAllByOrderByStartDateDesc())
                .thenReturn(List.of(year));

        when(enrollmentRepository.findByStudentIdAndAcademicYearId(1L, 30L))
                .thenReturn(Optional.of(enrollment));
    }

    @Test
    void shouldAcceptActiveEnrollmentInCorrectFiliereAndYear() {
        mockValidEnrollmentLookup();

        assertDoesNotThrow(() ->
                validator.validate(1L, module, evaluationDate));

        verify(enrollmentRepository)
                .findByStudentIdAndAcademicYearId(1L, 30L);
    }

    @Test
    void shouldRejectSuspendedEnrollment() {
        enrollment.setStatus(EnrollmentStatus.SUSPENDED);
        mockValidEnrollmentLookup();

        assertThrows(BadRequestException.class, () ->
                validator.validate(1L, module, evaluationDate));
    }

    @Test
    void shouldRejectWrongFiliere() {
        Filiere otherFiliere = new Filiere();
        otherFiliere.setId(99L);
        enrollment.setFiliere(otherFiliere);

        mockValidEnrollmentLookup();

        assertThrows(BadRequestException.class, () ->
                validator.validate(1L, module, evaluationDate));
    }

    @Test
    void shouldRejectMissingEnrollment() {
        when(studentRepository.existsById(1L)).thenReturn(true);

        when(academicYearRepository.findAllByOrderByStartDateDesc())
                .thenReturn(List.of(year));

        when(enrollmentRepository.findByStudentIdAndAcademicYearId(1L, 30L))
                .thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () ->
                validator.validate(1L, module, evaluationDate));
    }

    @Test
    void shouldRejectDateOutsideAcademicYear() {
        when(studentRepository.existsById(1L)).thenReturn(true);

        when(academicYearRepository.findAllByOrderByStartDateDesc())
                .thenReturn(List.of(year));

        LocalDate invalidDate = LocalDate.of(2028, 1, 15);

        assertThrows(BadRequestException.class, () ->
                validator.validate(1L, module, invalidDate));

        verifyNoInteractions(enrollmentRepository);
    }

    @Test
    void shouldRejectOverlappingAcademicYears() {
        AcademicYear secondYear = new AcademicYear();
        org.springframework.test.util.ReflectionTestUtils.setField(
                secondYear, "id", 31L);
        secondYear.setStartDate(LocalDate.of(2026, 10, 1));
        secondYear.setEndDate(LocalDate.of(2027, 9, 30));

        when(studentRepository.existsById(1L)).thenReturn(true);

        when(academicYearRepository.findAllByOrderByStartDateDesc())
                .thenReturn(List.of(year, secondYear));

        assertThrows(BadRequestException.class, () ->
                validator.validate(1L, module, evaluationDate));

        verifyNoInteractions(enrollmentRepository);
    }

    @Test
    void shouldAcceptValidEnrollmentForAcademicYear() {
        when(studentRepository.existsById(1L))
                .thenReturn(true);

        when(academicYearRepository.existsById(30L))
                .thenReturn(true);

        when(enrollmentRepository
                .findByStudentIdAndAcademicYearId(1L, 30L))
                .thenReturn(Optional.of(enrollment));

        assertDoesNotThrow(() ->
                validator.validateForAcademicYear(
                        1L, module, 30L));

        verify(enrollmentRepository)
                .findByStudentIdAndAcademicYearId(1L, 30L);
    }

    @Test
    void shouldRejectWrongFiliereForAcademicYear() {
        Filiere otherFiliere = new Filiere();
        otherFiliere.setId(99L);
        enrollment.setFiliere(otherFiliere);

        when(studentRepository.existsById(1L))
                .thenReturn(true);

        when(academicYearRepository.existsById(30L))
                .thenReturn(true);

        when(enrollmentRepository
                .findByStudentIdAndAcademicYearId(1L, 30L))
                .thenReturn(Optional.of(enrollment));

        assertThrows(BadRequestException.class, () ->
                validator.validateForAcademicYear(
                        1L, module, 30L));
    }

    @Test
    void shouldRejectMissingAcademicYearById() {
        when(studentRepository.existsById(1L))
                .thenReturn(true);

        when(academicYearRepository.existsById(30L))
                .thenReturn(false);

        assertThrows(
                ma.ump.deptinfo.backend.exceptions.ResourceNotFoundException.class,
                () -> validator.validateForAcademicYear(
                        1L, module, 30L));

        verifyNoInteractions(enrollmentRepository);
    }

    @Test
    void shouldRejectMissingEnrollmentForAcademicYear() {
        when(studentRepository.existsById(1L))
                .thenReturn(true);

        when(academicYearRepository.existsById(30L))
                .thenReturn(true);

        when(enrollmentRepository.findByStudentIdAndAcademicYearId(1L, 30L))
                .thenReturn(Optional.empty());

        assertThrows(BadRequestException.class,
                () -> validator.validateForAcademicYear(
                        1L, module, 30L));
    }
}