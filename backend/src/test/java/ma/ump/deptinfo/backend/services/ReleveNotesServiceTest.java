
package ma.ump.deptinfo.backend.services;

import ma.ump.deptinfo.backend.dtos.ReleveNotesResponseDTO;
import ma.ump.deptinfo.backend.entities.*;
import ma.ump.deptinfo.backend.exceptions.BadRequestException;
import ma.ump.deptinfo.backend.exceptions.ResourceNotFoundException;
import ma.ump.deptinfo.backend.repositories.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReleveNotesServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AcademicYearRepository academicYearRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private ModuleCoursRepository moduleCoursRepository;

    @Mock
    private GradeRepository gradeRepository;

    @Mock
    private GradeAverageCalculator gradeAverageCalculator;

    @InjectMocks
    private ReleveNotesService releveNotesService;

    private Student student;
    private User studentUser;
    private AcademicYear year;
    private Enrollment enrollment;
    private List<ModuleCours> modules;

    @BeforeEach
    void setUp() {

        studentUser = new User();
        studentUser.setId(10L);
        studentUser.setEmail("student@test.local");
        studentUser.setFirstName("Test");
        studentUser.setLastName("Student");
        studentUser.setRole(Role.ETUDIANT);

        student = new Student();
        ReflectionTestUtils.setField(student, "id", 1L);
        student.setStudentNumber("M2I001");
        student.setUser(studentUser);

        year = new AcademicYear();
        ReflectionTestUtils.setField(year, "id", 30L);
        year.setStartDate(LocalDate.of(2026, 9, 1));
        year.setEndDate(LocalDate.of(2027, 8, 31));

        Filiere filiere = new Filiere();
        filiere.setId(5L);

        enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setFiliere(filiere);
        enrollment.setAcademicYear(year);

        ModuleCours java = new ModuleCours();
        java.setId(101L);
        java.setCode("JAVA");
        java.setNom("Java");
        java.setSemestre("S3");

        ModuleCours reseaux = new ModuleCours();
        reseaux.setId(102L);
        reseaux.setCode("RES");
        reseaux.setNom("Reseaux");
        reseaux.setSemestre("S3");

        ModuleCours cyber = new ModuleCours();
        cyber.setId(103L);
        cyber.setCode("CYBER");
        cyber.setNom("Cybersecurite");
        cyber.setSemestre("S3");

        modules = List.of(java, reseaux, cyber);
    }

    // Methodes utilitaires

    private void mockStudentAccess() {

        when(userRepository.findByEmail("student@test.local"))
                .thenReturn(Optional.of(studentUser));

        when(studentRepository.findById(1L))
                .thenReturn(Optional.of(student));
    }

    private void mockEnrollment() {

        mockStudentAccess();

        when(academicYearRepository.findFirstByCurrentTrue())
                .thenReturn(Optional.of(year));

        when(enrollmentRepository
                .findByStudentIdAndAcademicYearId(1L, 30L))
                .thenReturn(Optional.of(enrollment));
    }

    private void mockStudentAndSemester() {

        mockEnrollment();

        when(moduleCoursRepository
                .findByFiliereIdAndSemestre(5L, "S3"))
                .thenReturn(modules);
    }

    private void mockModuleAverage(Long moduleId, String average) {

        Grade grade = new Grade();
        grade.setType(GradeType.EXAMEN_FINAL);
        grade.setStatus(GradeStatus.PUBLISHED);
        grade.setScore(new BigDecimal(average));
        grade.setCoefficient(BigDecimal.ONE);

        when(gradeRepository
                .findByStudentIdAndModuleIdAndSessionDateBetween(
                        eq(1L),
                        eq(moduleId),
                        any(LocalDate.class),
                        any(LocalDate.class)))
                .thenReturn(List.of(grade));

        when(gradeAverageCalculator.calculate(List.of(grade)))
                .thenReturn(Optional.of(new BigDecimal(average)));
    }

    private ReleveNotesResponseDTO getReleve(
            String javaAverage,
            String reseauxAverage,
            String cyberAverage) {

        mockStudentAndSemester();

        mockModuleAverage(101L, javaAverage);
        mockModuleAverage(102L, reseauxAverage);
        mockModuleAverage(103L, cyberAverage);

        return releveNotesService.getReleveNotes(
                1L, "S3", "student@test.local");
    }

    private Grade createGrade(
            String score,
            String coefficient,
            GradeType type,
            GradeStatus status) {

        Grade grade = new Grade();

        grade.setScore(new BigDecimal(score));
        grade.setCoefficient(new BigDecimal(coefficient));
        grade.setType(type);
        grade.setStatus(status);

        return grade;
    }

    private void mockGrades(Long moduleId, List<Grade> grades) {

        when(gradeRepository
                .findByStudentIdAndModuleIdAndSessionDateBetween(
                        1L,
                        moduleId,
                        year.getStartDate(),
                        year.getEndDate()))
                .thenReturn(grades);
    }

    // ==================================================
    // TESTS DES REGLES ACADEMIQUES
    // ==================================================

    // Test 1 : Moyenne semestrielle et compensation.

    @Test
    void shouldCalculateSemesterAverageWithCompensation() {

        ReleveNotesResponseDTO result =
                getReleve("14.50", "11.00", "8.50");

        assertEquals(0, new BigDecimal("11.33")
                .compareTo(result.semesterAverage()));

        assertEquals("ADMIS", result.decision());
        assertEquals("Passable", result.mention());
        assertEquals(3, result.modules().size());

        assertEquals("VALIDE",
                result.modules().get(0).status());

        assertEquals("VALIDE",
                result.modules().get(1).status());

        assertEquals("VALIDE_PAR_COMPENSATION",
                result.modules().get(2).status());

        assertEquals("Test Student", result.studentName());
        assertEquals("M2I001", result.studentNumber());
        assertEquals("S3", result.semester());
    }

    // Test 2 : Note eliminatoire.

    @Test
    void shouldRequireRetakeWhenModuleBelowFive() {

        ReleveNotesResponseDTO result =
                getReleve("18.00", "15.00", "4.00");

        assertEquals(0, new BigDecimal("12.33")
                .compareTo(result.semesterAverage()));

        assertEquals("RATTRAPAGE", result.decision());
        assertNull(result.mention());

        assertEquals("NON_VALIDE",
                result.modules().get(2).status());
    }

    // Test 3 : Moyenne semestrielle inferieure a 10.

    @Test
    void shouldRequireRetakeWhenSemesterAverageBelowTen() {

        ReleveNotesResponseDTO result =
                getReleve("9.00", "8.00", "7.00");

        assertEquals(0, new BigDecimal("8.00")
                .compareTo(result.semesterAverage()));

        assertEquals("RATTRAPAGE", result.decision());
        assertNull(result.mention());
    }

    // Test 4 : Mention Tres Bien.

    @Test
    void shouldGiveTresBienMention() {

        ReleveNotesResponseDTO result =
                getReleve("18.00", "16.00", "17.00");

        assertEquals("ADMIS", result.decision());
        assertEquals("Tres Bien", result.mention());
    }

    // Test 5 : Mention Bien.

    @Test
    void shouldGiveBienMention() {

        ReleveNotesResponseDTO result =
                getReleve("15.00", "14.00", "16.00");

        assertEquals("ADMIS", result.decision());
        assertEquals("Bien", result.mention());
    }

    // Test 6 : Mention Assez Bien.

    @Test
    void shouldGiveAssezBienMention() {

        ReleveNotesResponseDTO result =
                getReleve("12.00", "13.00", "14.00");

        assertEquals("ADMIS", result.decision());
        assertEquals("Assez Bien", result.mention());
    }

    // Test 7 : Absence de notes publiees.

    @Test
    void shouldRejectMissingPublishedGrades() {

        mockStudentAndSemester();

        when(gradeRepository
                .findByStudentIdAndModuleIdAndSessionDateBetween(
                        eq(1L),
                        eq(101L),
                        any(LocalDate.class),
                        any(LocalDate.class)))
                .thenReturn(List.of());

        when(gradeAverageCalculator.calculate(List.of()))
                .thenReturn(Optional.empty());

        assertThrows(
                BadRequestException.class,
                () -> releveNotesService.getReleveNotes(
                        1L, "S3", "student@test.local"));
    }

    // Test 8 : Semestre invalide.

    @Test
    void shouldRejectInvalidSemester() {

        mockEnrollment();

        assertThrows(
                BadRequestException.class,
                () -> releveNotesService.getReleveNotes(
                        1L, "S9", "student@test.local"));

        verifyNoInteractions(moduleCoursRepository);
    }

    // Test 9 : Acces au releve d'un autre etudiant.

    @Test
    void shouldRejectAccessToAnotherStudentsTranscript() {

        User anotherStudent = new User();

        anotherStudent.setId(99L);
        anotherStudent.setEmail("other@test.local");
        anotherStudent.setRole(Role.ETUDIANT);

        when(userRepository.findByEmail("other@test.local"))
                .thenReturn(Optional.of(anotherStudent));

        when(studentRepository.findById(1L))
                .thenReturn(Optional.of(student));

        assertThrows(
                AccessDeniedException.class,
                () -> releveNotesService.getReleveNotes(
                        1L, "S3", "other@test.local"));

        verifyNoInteractions(
                academicYearRepository,
                enrollmentRepository,
                moduleCoursRepository,
                gradeRepository,
                gradeAverageCalculator);
    }

    // ==================================================
    // TESTS COMPLEMENTAIRES DE SECURITE
    // ==================================================

    // Test 10 : Identifiants invalides.

    @Test
    void shouldRejectInvalidStudentIds() {

        for (Long id : Arrays.asList(null, 0L, -1L)) {

            assertThrows(
                    BadRequestException.class,
                    () -> releveNotesService.getReleveNotes(
                            id, "S3", "student@test.local"));
        }

        verifyNoInteractions(
                userRepository,
                studentRepository);
    }

    // Test 11 : Email d'authentification absent.

    @Test
    void shouldRejectMissingAuthenticationEmail() {

        for (String email : Arrays.asList(null, "", "   ")) {

            assertThrows(
                    AccessDeniedException.class,
                    () -> releveNotesService.getReleveNotes(
                            1L, "S3", email));
        }

        verifyNoInteractions(
                userRepository,
                studentRepository);
    }

    // Test 12 : Utilisateur authentifie introuvable.

    @Test
    void shouldRejectUnknownAuthenticatedUser() {

        when(userRepository.findByEmail("unknown@test.local"))
                .thenReturn(Optional.empty());

        assertThrows(
                AccessDeniedException.class,
                () -> releveNotesService.getReleveNotes(
                        1L, "S3", "unknown@test.local"));

        verifyNoInteractions(studentRepository);
    }

    // Test 13 : Etudiant introuvable.

    @Test
    void shouldRejectUnknownStudent() {

        when(userRepository.findByEmail("student@test.local"))
                .thenReturn(Optional.of(studentUser));

        when(studentRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> releveNotesService.getReleveNotes(
                        999L, "S3", "student@test.local"));

        verifyNoInteractions(academicYearRepository);
    }

    // Test 14 : Acces administrateur.

    @Test
    void shouldAllowAdminAccess() {

        studentUser.setRole(Role.ADMIN);

        mockStudentAccess();

        assertSame(
                student,
                releveNotesService.validateStudentAccess(
                        1L, "student@test.local"));
    }

    // Test 15 : Acces chef de departement.

    @Test
    void shouldAllowDepartmentHeadAccess() {

        studentUser.setRole(Role.CHEF_DEPARTEMENT);

        mockStudentAccess();

        assertSame(
                student,
                releveNotesService.validateStudentAccess(
                        1L, "student@test.local"));
    }

    // Test 16 : Acces enseignant interdit.

    @Test
    void shouldRejectTeacherAccess() {

        studentUser.setRole(Role.ENSEIGNANT);

        mockStudentAccess();

        assertThrows(
                AccessDeniedException.class,
                () -> releveNotesService.getReleveNotes(
                        1L, "S3", "student@test.local"));

        verifyNoInteractions(academicYearRepository);
    }

    // Test 17 : Etudiant sans compte utilisateur associe.

    @Test
    void shouldRejectStudentWithoutLinkedUser() {

        student.setUser(null);

        mockStudentAccess();

        assertThrows(
                AccessDeniedException.class,
                () -> releveNotesService.getReleveNotes(
                        1L, "S3", "student@test.local"));

        verifyNoInteractions(academicYearRepository);
    }

    // ==================================================
    // TESTS DES DONNEES MANQUANTES
    // ==================================================

    // Test 18 : Aucune annee universitaire active.

    @Test
    void shouldRejectMissingCurrentAcademicYear() {

        mockStudentAccess();

        when(academicYearRepository.findFirstByCurrentTrue())
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> releveNotesService.getReleveNotes(
                        1L, "S3", "student@test.local"));

        verifyNoInteractions(
                enrollmentRepository,
                gradeRepository);
    }

    // Test 19 : Inscription introuvable.

    @Test
    void shouldRejectMissingEnrollment() {

        mockStudentAccess();

        when(academicYearRepository.findFirstByCurrentTrue())
                .thenReturn(Optional.of(year));

        when(enrollmentRepository
                .findByStudentIdAndAcademicYearId(1L, 30L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> releveNotesService.getReleveNotes(
                        1L, "S3", "student@test.local"));

        verifyNoInteractions(
                moduleCoursRepository,
                gradeRepository);
    }

    // Test 20 : Semestre null ou vide.

    @Test
    void shouldRejectMissingSemester() {

        mockEnrollment();

        for (String semester : Arrays.asList(null, "", "   ")) {

            assertThrows(
                    BadRequestException.class,
                    () -> releveNotesService.getReleveNotes(
                            1L, semester, "student@test.local"));
        }

        verifyNoInteractions(
                moduleCoursRepository,
                gradeRepository);
    }

    // Test 21 : Inscription sans filiere.

    @Test
    void shouldRejectEnrollmentWithoutFiliere() {

        enrollment.setFiliere(null);

        mockEnrollment();

        assertThrows(
                BadRequestException.class,
                () -> releveNotesService.getReleveNotes(
                        1L, "S3", "student@test.local"));

        verifyNoInteractions(
                moduleCoursRepository,
                gradeRepository);
    }

    // Test 22 : Aucun module pour le semestre.

    @Test
    void shouldRejectSemesterWithoutModules() {

        mockEnrollment();

        when(moduleCoursRepository
                .findByFiliereIdAndSemestre(5L, "S3"))
                .thenReturn(List.of());

        assertThrows(
                ResourceNotFoundException.class,
                () -> releveNotesService.getReleveNotes(
                        1L, "S3", "student@test.local"));

        verifyNoInteractions(gradeRepository);
    }

    // ==================================================
    // TESTS DES VALEURS LIMITES
    // ==================================================

    // Test 23 : Moyenne 10 et module a 5 exactement.

    @Test
    void shouldAdmitAtExactSemesterAndModuleThresholds() {

        ReleveNotesResponseDTO result =
                getReleve("15.00", "10.00", "5.00");

        assertEquals(0, new BigDecimal("10.00")
                .compareTo(result.semesterAverage()));

        assertEquals("ADMIS", result.decision());
        assertEquals("Passable", result.mention());

        assertEquals("VALIDE",
                result.modules().get(1).status());

        assertEquals("VALIDE_PAR_COMPENSATION",
                result.modules().get(2).status());
    }

    // Test 24 : Note eliminatoire a 4.99.

    @Test
    void shouldRejectModuleAtFourPointNinetyNine() {

        ReleveNotesResponseDTO result =
                getReleve("15.00", "15.00", "4.99");

        assertEquals(0, new BigDecimal("11.66")
                .compareTo(result.semesterAverage()));

        assertEquals("RATTRAPAGE", result.decision());
        assertNull(result.mention());

        assertEquals("NON_VALIDE",
                result.modules().get(2).status());
    }

    // Test 25 : Moyenne semestrielle a 9.99.

    @Test
    void shouldRequireRetakeAtNinePointNinetyNine() {

        ReleveNotesResponseDTO result =
                getReleve("9.99", "9.99", "9.99");

        assertEquals(0, new BigDecimal("9.99")
                .compareTo(result.semesterAverage()));

        assertEquals("RATTRAPAGE", result.decision());
        assertNull(result.mention());
    }

    // Test 26 : Normalisation du semestre.

    @Test
    void shouldNormalizeSemester() {

        mockStudentAndSemester();

        mockModuleAverage(101L, "12.00");
        mockModuleAverage(102L, "12.00");
        mockModuleAverage(103L, "12.00");

        ReleveNotesResponseDTO result =
                releveNotesService.getReleveNotes(
                        1L, " s3 ", "student@test.local");

        assertEquals("S3", result.semester());
        assertEquals("Assez Bien", result.mention());

        verify(moduleCoursRepository)
                .findByFiliereIdAndSemestre(5L, "S3");
    }

    // Test 27 : Compte etudiant manquant pour un releve admin.

    @Test
    void shouldRejectMissingStudentAccountForAdminTranscript() {

        studentUser.setRole(Role.ADMIN);
        student.setUser(null);

        mockStudentAndSemester();

        mockModuleAverage(101L, "12.00");
        mockModuleAverage(102L, "12.00");
        mockModuleAverage(103L, "12.00");

        assertThrows(
                BadRequestException.class,
                () -> releveNotesService.getReleveNotes(
                        1L, "S3", "student@test.local"));
    }

    // ==================================================
    // TEST AVEC LE VRAI CALCULATEUR
    // ==================================================

    // Test 28 : Calcul pondere et filtrage des notes.

    @Test
    void shouldCombineRealWeightedCalculatorAndRegularSessionFiltering() {

        mockStudentAndSemester();

        // JAVA : (12 x 1 + 18 x 2) / 3 = 16.
        // Rattrapage, brouillon et entree null sont exclus.

        mockGrades(101L, Arrays.asList(

                createGrade(
                        "12", "1",
                        GradeType.CONTROLE_CONTINU,
                        GradeStatus.PUBLISHED),

                createGrade(
                        "18", "2",
                        GradeType.EXAMEN_FINAL,
                        GradeStatus.PUBLISHED),

                createGrade(
                        "20", "1",
                        GradeType.RATTRAPAGE,
                        GradeStatus.PUBLISHED),

                createGrade(
                        "20", "1",
                        GradeType.TP,
                        GradeStatus.DRAFT),

                null
        ));

        mockGrades(102L, List.of(

                createGrade(
                        "9", "1",
                        GradeType.EXAMEN_FINAL,
                        GradeStatus.PUBLISHED)
        ));

        mockGrades(103L, List.of(

                createGrade(
                        "5", "1",
                        GradeType.EXAMEN_FINAL,
                        GradeStatus.PUBLISHED)
        ));

        ReleveNotesService serviceWithRealCalculator =
                new ReleveNotesService(
                        studentRepository,
                        userRepository,
                        academicYearRepository,
                        enrollmentRepository,
                        moduleCoursRepository,
                        gradeRepository,
                        new GradeAverageCalculator());

        ReleveNotesResponseDTO result =
                serviceWithRealCalculator.getReleveNotes(
                        1L, "S3", "student@test.local");

        assertEquals(0, new BigDecimal("16.00")
                .compareTo(result.modules().get(0).score()));

        assertEquals(0, new BigDecimal("9.00")
                .compareTo(result.modules().get(1).score()));

        assertEquals(0, new BigDecimal("5.00")
                .compareTo(result.modules().get(2).score()));

        assertEquals(0, new BigDecimal("10.00")
                .compareTo(result.semesterAverage()));

        assertEquals("ADMIS", result.decision());
        assertEquals("Passable", result.mention());

        assertEquals("VALIDE_PAR_COMPENSATION",
                result.modules().get(2).status());

        verifyNoInteractions(gradeAverageCalculator);
    }
}
