package ma.ump.deptinfo.backend.services;

import ma.ump.deptinfo.backend.entities.ModuleCours;
import ma.ump.deptinfo.backend.entities.Role;
import ma.ump.deptinfo.backend.entities.User;
import ma.ump.deptinfo.backend.repositories.ModuleCoursRepository;
import ma.ump.deptinfo.backend.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GradeServiceTest {

    @Mock
    private ModuleCoursRepository moduleCoursRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ma.ump.deptinfo.backend.repositories.GradeRepository gradeRepository;

    @Mock
    private ma.ump.deptinfo.backend.repositories.StudentRepository studentRepository;

    @Mock
    private EnrollmentEligibilityValidator enrollmentValidator;

    @Mock
    private GradeAverageCalculator averageCalculator;

    @Mock
    private ma.ump.deptinfo.backend.repositories.AcademicYearRepository academicYearRepository;

    @InjectMocks
    private GradeService gradeService;

    private User teacher;
    private ModuleCours module;

    @BeforeEach
    void setUp() {
        teacher = new User();
        teacher.setId(10L);
        teacher.setEmail("teacher@test.local");
        teacher.setRole(Role.ENSEIGNANT);
        teacher.setEnabled(true);

        module = new ModuleCours();
        module.setId(20L);
        module.setEnseignant(teacher);
    }

    @Test
    void shouldAllowAssignedTeacher() {
        when(userRepository.findByEmail("teacher@test.local"))
                .thenReturn(Optional.of(teacher));

        when(moduleCoursRepository.findById(20L))
                .thenReturn(Optional.of(module));

        assertDoesNotThrow(() ->
                gradeService.verifyTeacherAccess(
                        20L, "teacher@test.local"));

        verify(userRepository).findByEmail("teacher@test.local");
        verify(moduleCoursRepository).findById(20L);
    }

    @Test
    void shouldRejectTeacherNotAssignedToModule() {
        User anotherTeacher = new User();
        anotherTeacher.setId(99L);
        anotherTeacher.setEmail("another@test.local");
        anotherTeacher.setRole(Role.ENSEIGNANT);
        anotherTeacher.setEnabled(true);

        when(userRepository.findByEmail("another@test.local"))
                .thenReturn(Optional.of(anotherTeacher));

        when(moduleCoursRepository.findById(20L))
                .thenReturn(Optional.of(module));

        assertThrows(AccessDeniedException.class, () ->
                gradeService.verifyTeacherAccess(
                        20L, "another@test.local"));
    }

    @Test
    void shouldRejectDisabledTeacher() {
        teacher.setEnabled(false);

        when(userRepository.findByEmail("teacher@test.local"))
                .thenReturn(Optional.of(teacher));

        assertThrows(AccessDeniedException.class, () ->
                gradeService.verifyTeacherAccess(
                        20L, "teacher@test.local"));

        verifyNoInteractions(moduleCoursRepository);
    }

    @Test
    void shouldRejectModuleWithoutAssignedTeacher() {
        module.setEnseignant(null);

        when(userRepository.findByEmail("teacher@test.local"))
                .thenReturn(Optional.of(teacher));

        when(moduleCoursRepository.findById(20L))
                .thenReturn(Optional.of(module));

        assertThrows(AccessDeniedException.class, () ->
                gradeService.verifyTeacherAccess(
                        20L, "teacher@test.local"));
    }

    @Test
    void shouldRejectMissingAuthentication() {
        assertThrows(AccessDeniedException.class, () ->
                gradeService.verifyTeacherAccess(20L, null));

        verifyNoInteractions(userRepository, moduleCoursRepository);
    }

    @Test
    void shouldCreateDraftGrade() {
        ma.ump.deptinfo.backend.entities.Student student =
                new ma.ump.deptinfo.backend.entities.Student();

        org.springframework.test.util.ReflectionTestUtils.setField(
                student, "id", 1L);

        ma.ump.deptinfo.backend.dtos.GradeCreateRequestDTO dto =
                new ma.ump.deptinfo.backend.dtos.GradeCreateRequestDTO(
                        1L,
                        20L,
                        new java.math.BigDecimal("15.50"),
                        ma.ump.deptinfo.backend.entities.GradeType.CONTROLE_CONTINU,
                        java.math.BigDecimal.ONE,
                        java.time.LocalDate.of(2026, 11, 18)
                );

        when(userRepository.findByEmail("teacher@test.local"))
                .thenReturn(Optional.of(teacher));

        when(moduleCoursRepository.findById(20L))
                .thenReturn(Optional.of(module));

        when(studentRepository.findById(1L))
                .thenReturn(Optional.of(student));

        when(gradeRepository.save(
                any(ma.ump.deptinfo.backend.entities.Grade.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ma.ump.deptinfo.backend.entities.Grade result =
                gradeService.createDraftGrade(dto, "teacher@test.local");

        assertNotNull(result);
        assertEquals(
                ma.ump.deptinfo.backend.entities.GradeStatus.DRAFT,
                result.getStatus());

        assertEquals(
                0,
                new java.math.BigDecimal("15.50")
                        .compareTo(result.getScore()));

        assertEquals(
                ma.ump.deptinfo.backend.entities.GradeType.CONTROLE_CONTINU,
                result.getType());

        assertEquals(student, result.getStudent());
        assertEquals(module, result.getModule());

        verify(enrollmentValidator).validate(
                1L,
                module,
                java.time.LocalDate.of(2026, 11, 18));

        verify(gradeRepository).save(
                any(ma.ump.deptinfo.backend.entities.Grade.class));
    }

    @Test
    void shouldRejectGradeAboveTwenty() {
        ma.ump.deptinfo.backend.dtos.GradeCreateRequestDTO dto =
                new ma.ump.deptinfo.backend.dtos.GradeCreateRequestDTO(
                        1L,
                        20L,
                        new java.math.BigDecimal("25.00"),
                        ma.ump.deptinfo.backend.entities.GradeType.CONTROLE_CONTINU,
                        java.math.BigDecimal.ONE,
                        java.time.LocalDate.of(2026, 11, 18)
                );

        assertThrows(
                ma.ump.deptinfo.backend.exceptions.BadRequestException.class,
                () -> gradeService.createDraftGrade(
                        dto, "teacher@test.local")
        );

        verifyNoInteractions(
                userRepository,
                moduleCoursRepository,
                studentRepository,
                enrollmentValidator,
                gradeRepository
        );
    }

    @Test
    void shouldRejectGradeWithTooManyDecimals() {
        ma.ump.deptinfo.backend.dtos.GradeCreateRequestDTO dto =
                new ma.ump.deptinfo.backend.dtos.GradeCreateRequestDTO(
                        1L,
                        20L,
                        new java.math.BigDecimal("15.999"),
                        ma.ump.deptinfo.backend.entities.GradeType.CONTROLE_CONTINU,
                        java.math.BigDecimal.ONE,
                        java.time.LocalDate.of(2026, 11, 18)
                );

        assertThrows(
                ma.ump.deptinfo.backend.exceptions.BadRequestException.class,
                () -> gradeService.createDraftGrade(
                        dto, "teacher@test.local")
        );

        verifyNoInteractions(
                userRepository,
                moduleCoursRepository,
                studentRepository,
                enrollmentValidator,
                gradeRepository
        );
    }

    @Test
    void shouldRejectCoefficientWithTooManyDecimals() {
        ma.ump.deptinfo.backend.dtos.GradeCreateRequestDTO dto =
                new ma.ump.deptinfo.backend.dtos.GradeCreateRequestDTO(
                        1L,
                        20L,
                        new java.math.BigDecimal("15.50"),
                        ma.ump.deptinfo.backend.entities.GradeType.CONTROLE_CONTINU,
                        new java.math.BigDecimal("1.257"),
                        java.time.LocalDate.of(2026, 11, 18)
                );

        assertThrows(
                ma.ump.deptinfo.backend.exceptions.BadRequestException.class,
                () -> gradeService.createDraftGrade(
                        dto, "teacher@test.local")
        );

        verifyNoInteractions(
                userRepository,
                moduleCoursRepository,
                studentRepository,
                enrollmentValidator,
                gradeRepository
        );
    }

    @Test
    void shouldRejectCoefficientAboveDatabaseLimit() {
        ma.ump.deptinfo.backend.dtos.GradeCreateRequestDTO dto =
                new ma.ump.deptinfo.backend.dtos.GradeCreateRequestDTO(
                        1L,
                        20L,
                        new java.math.BigDecimal("15.50"),
                        ma.ump.deptinfo.backend.entities.GradeType.CONTROLE_CONTINU,
                        new java.math.BigDecimal("1000.00"),
                        java.time.LocalDate.of(2026, 11, 18)
                );

        assertThrows(
                ma.ump.deptinfo.backend.exceptions.BadRequestException.class,
                () -> gradeService.createDraftGrade(
                        dto, "teacher@test.local")
        );

        verifyNoInteractions(
                userRepository,
                moduleCoursRepository,
                studentRepository,
                enrollmentValidator,
                gradeRepository
        );
    }

    @Test
    void shouldCalculateModuleAverageForAssignedTeacher() {
        ma.ump.deptinfo.backend.entities.AcademicYear year =
                new ma.ump.deptinfo.backend.entities.AcademicYear();

        year.setStartDate(java.time.LocalDate.of(2026, 9, 1));
        year.setEndDate(java.time.LocalDate.of(2027, 8, 31));

        ma.ump.deptinfo.backend.entities.Grade grade =
                new ma.ump.deptinfo.backend.entities.Grade();

        grade.setScore(new java.math.BigDecimal("13.50"));
        grade.setCoefficient(java.math.BigDecimal.ONE);
        grade.setStatus(
                ma.ump.deptinfo.backend.entities.GradeStatus.PUBLISHED);

        when(userRepository.findByEmail("teacher@test.local"))
                .thenReturn(Optional.of(teacher));

        when(moduleCoursRepository.findById(20L))
                .thenReturn(Optional.of(module));

        when(academicYearRepository.findById(30L))
                .thenReturn(Optional.of(year));

        when(gradeRepository
                .findByStudentIdAndModuleIdAndSessionDateBetween(
                        1L, 20L, year.getStartDate(), year.getEndDate()))
                .thenReturn(java.util.List.of(grade));

        when(averageCalculator.calculate(java.util.List.of(grade)))
                .thenReturn(Optional.of(
                        new java.math.BigDecimal("13.50")));

        Optional<java.math.BigDecimal> result =
                gradeService.calculateModuleAverage(
                        1L, 20L, 30L, "teacher@test.local");

        assertTrue(result.isPresent());
        assertEquals(0,
                new java.math.BigDecimal("13.50")
                        .compareTo(result.get()));

        verify(enrollmentValidator)
                .validateForAcademicYear(1L, module, 30L);

        verify(gradeRepository)
                .findByStudentIdAndModuleIdAndSessionDateBetween(
                        1L, 20L, year.getStartDate(), year.getEndDate());
    }

    @Test
    void shouldRejectModuleAverageForUnassignedTeacher() {
        User anotherTeacher = new User();
        anotherTeacher.setId(99L);
        anotherTeacher.setEmail("other@test.local");
        anotherTeacher.setRole(Role.ENSEIGNANT);
        anotherTeacher.setEnabled(true);

        when(userRepository.findByEmail("other@test.local"))
                .thenReturn(Optional.of(anotherTeacher));

        when(moduleCoursRepository.findById(20L))
                .thenReturn(Optional.of(module));

        assertThrows(AccessDeniedException.class, () ->
                gradeService.calculateModuleAverage(
                        1L, 20L, 30L, "other@test.local"));

        verifyNoInteractions(
                academicYearRepository,
                enrollmentValidator,
                gradeRepository,
                averageCalculator,
                studentRepository
        );
    }

    @Test
    void shouldRejectAverageForStudentNotEnrolled() {
        ma.ump.deptinfo.backend.entities.AcademicYear year =
                new ma.ump.deptinfo.backend.entities.AcademicYear();

        year.setStartDate(java.time.LocalDate.of(2026, 9, 1));
        year.setEndDate(java.time.LocalDate.of(2027, 8, 31));

        when(userRepository.findByEmail("teacher@test.local"))
                .thenReturn(Optional.of(teacher));

        when(moduleCoursRepository.findById(20L))
                .thenReturn(Optional.of(module));

        when(academicYearRepository.findById(30L))
                .thenReturn(Optional.of(year));

        doThrow(new ma.ump.deptinfo.backend.exceptions.BadRequestException(
                "Etudiant non inscrit pour cette annee"))
                .when(enrollmentValidator)
                .validateForAcademicYear(1L, module, 30L);

        assertThrows(
                ma.ump.deptinfo.backend.exceptions.BadRequestException.class,
                () -> gradeService.calculateModuleAverage(
                        1L, 20L, 30L, "teacher@test.local")
        );

        verify(enrollmentValidator)
                .validateForAcademicYear(1L, module, 30L);

        verifyNoInteractions(gradeRepository, averageCalculator);
    }

    @Test
    void shouldPublishDraftGrade() {
        ma.ump.deptinfo.backend.entities.Grade grade =
                new ma.ump.deptinfo.backend.entities.Grade();
        grade.setModule(module);
        grade.setStatus(
                ma.ump.deptinfo.backend.entities.GradeStatus.DRAFT);
        grade.setScore(new java.math.BigDecimal("15.50"));
        grade.setCoefficient(java.math.BigDecimal.ONE);
        grade.setType(
                ma.ump.deptinfo.backend.entities.GradeType.CONTROLE_CONTINU);
        grade.setSessionDate(java.time.LocalDate.of(2026, 11, 18));

        when(gradeRepository.findById(100L))
                .thenReturn(Optional.of(grade));

        when(userRepository.findByEmail("teacher@test.local"))
                .thenReturn(Optional.of(teacher));

        when(moduleCoursRepository.findById(20L))
                .thenReturn(Optional.of(module));

        when(gradeRepository.save(grade))
                .thenReturn(grade);

        ma.ump.deptinfo.backend.entities.Grade result =
                gradeService.publishGrade(100L, "teacher@test.local");

        assertEquals(
                ma.ump.deptinfo.backend.entities.GradeStatus.PUBLISHED,
                result.getStatus());

        verify(gradeRepository).save(grade);
    }

    @Test
    void shouldRejectPublicationByUnassignedTeacher() {
        ma.ump.deptinfo.backend.entities.Grade grade =
                new ma.ump.deptinfo.backend.entities.Grade();
        grade.setModule(module);
        grade.setStatus(
                ma.ump.deptinfo.backend.entities.GradeStatus.DRAFT);

        User otherTeacher = new User();
        otherTeacher.setId(99L);
        otherTeacher.setEmail("other@test.local");
        otherTeacher.setRole(Role.ENSEIGNANT);
        otherTeacher.setEnabled(true);

        when(gradeRepository.findById(100L))
                .thenReturn(Optional.of(grade));

        when(userRepository.findByEmail("other@test.local"))
                .thenReturn(Optional.of(otherTeacher));

        when(moduleCoursRepository.findById(20L))
                .thenReturn(Optional.of(module));

        assertThrows(AccessDeniedException.class, () ->
                gradeService.publishGrade(100L, "other@test.local"));

        assertEquals(
                ma.ump.deptinfo.backend.entities.GradeStatus.DRAFT,
                grade.getStatus());

        verify(gradeRepository, never()).save(any());
    }

    @Test
    void shouldRejectAlreadyPublishedGrade() {
        ma.ump.deptinfo.backend.entities.Grade grade =
                new ma.ump.deptinfo.backend.entities.Grade();
        grade.setModule(module);
        grade.setStatus(
                ma.ump.deptinfo.backend.entities.GradeStatus.PUBLISHED);

        when(gradeRepository.findById(100L))
                .thenReturn(Optional.of(grade));

        when(userRepository.findByEmail("teacher@test.local"))
                .thenReturn(Optional.of(teacher));

        when(moduleCoursRepository.findById(20L))
                .thenReturn(Optional.of(module));

        assertThrows(
                ma.ump.deptinfo.backend.exceptions.BadRequestException.class,
                () -> gradeService.publishGrade(
                        100L, "teacher@test.local")
        );

        verify(gradeRepository, never()).save(any());
    }

    @Test
    void shouldRejectPublicationOfAbsentGrade() {
        ma.ump.deptinfo.backend.entities.Grade grade =
                new ma.ump.deptinfo.backend.entities.Grade();
        grade.setModule(module);
        grade.setStatus(
                ma.ump.deptinfo.backend.entities.GradeStatus.ABSENT);

        when(gradeRepository.findById(100L))
                .thenReturn(Optional.of(grade));

        when(userRepository.findByEmail("teacher@test.local"))
                .thenReturn(Optional.of(teacher));

        when(moduleCoursRepository.findById(20L))
                .thenReturn(Optional.of(module));

        assertThrows(
                ma.ump.deptinfo.backend.exceptions.BadRequestException.class,
                () -> gradeService.publishGrade(
                        100L, "teacher@test.local")
        );

        verify(gradeRepository, never()).save(any());
    }

    @Test
    void shouldRejectPublicationWithoutScore() {
        ma.ump.deptinfo.backend.entities.Grade grade =
                new ma.ump.deptinfo.backend.entities.Grade();

        grade.setModule(module);
        grade.setStatus(
                ma.ump.deptinfo.backend.entities.GradeStatus.DRAFT);
        grade.setScore(null);

        when(gradeRepository.findById(100L))
                .thenReturn(Optional.of(grade));

        when(userRepository.findByEmail("teacher@test.local"))
                .thenReturn(Optional.of(teacher));

        when(moduleCoursRepository.findById(20L))
                .thenReturn(Optional.of(module));

        assertThrows(
                ma.ump.deptinfo.backend.exceptions.BadRequestException.class,
                () -> gradeService.publishGrade(
                        100L, "teacher@test.local")
        );

        assertEquals(
                ma.ump.deptinfo.backend.entities.GradeStatus.DRAFT,
                grade.getStatus());

        verify(gradeRepository, never()).save(any());
    }

    @Test
    void shouldReturnPublishedGradesForAuthenticatedStudent() {
        User studentUser = new User();
        studentUser.setId(50L);
        studentUser.setEmail("student@test.local");
        studentUser.setRole(Role.ETUDIANT);
        studentUser.setEnabled(true);

        ma.ump.deptinfo.backend.entities.Grade published =
                new ma.ump.deptinfo.backend.entities.Grade();
        published.setStatus(
                ma.ump.deptinfo.backend.entities.GradeStatus.PUBLISHED);
        published.setScore(new java.math.BigDecimal("15.50"));

        when(userRepository.findByEmail("student@test.local"))
                .thenReturn(Optional.of(studentUser));

        when(gradeRepository.findByStudent_User_EmailAndStatus(
                "student@test.local",
                ma.ump.deptinfo.backend.entities.GradeStatus.PUBLISHED))
                .thenReturn(java.util.List.of(published));

        java.util.List<ma.ump.deptinfo.backend.entities.Grade> result =
                gradeService.getMyPublishedGrades("student@test.local");

        assertEquals(1, result.size());
        assertEquals(
                ma.ump.deptinfo.backend.entities.GradeStatus.PUBLISHED,
                result.get(0).getStatus());

        verify(gradeRepository).findByStudent_User_EmailAndStatus(
                "student@test.local",
                ma.ump.deptinfo.backend.entities.GradeStatus.PUBLISHED);
    }

    @Test
    void shouldRejectTeacherAccessToStudentGradeConsultation() {
        when(userRepository.findByEmail("teacher@test.local"))
                .thenReturn(Optional.of(teacher));

        assertThrows(AccessDeniedException.class, () ->
                gradeService.getMyPublishedGrades("teacher@test.local"));

        verifyNoInteractions(gradeRepository);
    }
}