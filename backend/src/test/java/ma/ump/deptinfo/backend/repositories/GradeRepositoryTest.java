package ma.ump.deptinfo.backend.repositories;

import jakarta.persistence.EntityManager;
import ma.ump.deptinfo.backend.entities.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class GradeRepositoryTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private GradeRepository gradeRepository;

    @Test
    void shouldSaveAndFindGradeByStudentModuleAndStatus() {

        User user = new User();
        user.setEmail("grade.student@test.local");
        user.setPasswordHash("test-hash");
        user.setFirstName("Test");
        user.setLastName("Student");
        user.setRole(Role.ETUDIANT);
        entityManager.persist(user);

        Student student = new Student();
        student.setStudentNumber("GRADE-STUDENT-001");
        student.setUser(user);
        entityManager.persist(student);

        Filiere filiere = new Filiere();
        filiere.setCode("GRADE-FIL-001");
        filiere.setNom("Filiere Test Grade");
        filiere.setNiveau("MASTER");
        entityManager.persist(filiere);

        ModuleCours module = new ModuleCours();
        module.setCode("GRADE-MOD-001");
        module.setNom("Module Test Grade");
        module.setSemestre("S1");
        module.setFiliere(filiere);
        entityManager.persist(module);

        Grade grade = new Grade();
        grade.setStudent(student);
        grade.setModule(module);
        grade.setScore(new BigDecimal("15.50"));
        grade.setType(GradeType.CONTROLE_CONTINU);
        grade.setCoefficient(BigDecimal.ONE);
        grade.setSessionDate(LocalDate.of(2026, 11, 18));
        grade.setStatus(GradeStatus.DRAFT);

        gradeRepository.saveAndFlush(grade);
        entityManager.clear();

        List<Grade> studentGrades =
                gradeRepository.findByStudentId(student.getId());

        List<Grade> moduleGrades =
                gradeRepository.findByStudentIdAndModuleId(
                        student.getId(), module.getId());

        List<Grade> publishedGrades =
                gradeRepository.findByStudentIdAndStatus(
                        student.getId(), GradeStatus.PUBLISHED);

        assertEquals(1, studentGrades.size());
        assertEquals(1, moduleGrades.size());
        assertTrue(publishedGrades.isEmpty());

        Grade saved = studentGrades.get(0);

        assertEquals(0, new BigDecimal("15.50")
                .compareTo(saved.getScore()));
        assertEquals(GradeType.CONTROLE_CONTINU, saved.getType());
        assertEquals(GradeStatus.DRAFT, saved.getStatus());
    }

    @Test
    void shouldFilterGradesByAcademicYearDates() {
        User user = new User();
        user.setEmail("year.grade.student@test.local");
        user.setPasswordHash("test-hash");
        user.setFirstName("Year");
        user.setLastName("Student");
        user.setRole(Role.ETUDIANT);
        entityManager.persist(user);

        Student student = new Student();
        student.setStudentNumber("YEAR-GRADE-STUDENT-001");
        student.setUser(user);
        entityManager.persist(student);

        Filiere filiere = new Filiere();
        filiere.setCode("YEAR-GRADE-FIL-001");
        filiere.setNom("Filiere Test Annee");
        filiere.setNiveau("MASTER");
        entityManager.persist(filiere);

        ModuleCours module = new ModuleCours();
        module.setCode("YEAR-GRADE-MOD-001");
        module.setNom("Module Test Annee");
        module.setSemestre("S1");
        module.setFiliere(filiere);
        entityManager.persist(module);

        Grade oldGrade = new Grade();
        oldGrade.setStudent(student);
        oldGrade.setModule(module);
        oldGrade.setScore(new BigDecimal("10.00"));
        oldGrade.setType(GradeType.EXAMEN_FINAL);
        oldGrade.setCoefficient(BigDecimal.ONE);
        oldGrade.setSessionDate(LocalDate.of(2026, 3, 15));
        oldGrade.setStatus(GradeStatus.PUBLISHED);
        gradeRepository.saveAndFlush(oldGrade);

        Grade currentGrade = new Grade();
        currentGrade.setStudent(student);
        currentGrade.setModule(module);
        currentGrade.setScore(new BigDecimal("16.00"));
        currentGrade.setType(GradeType.EXAMEN_FINAL);
        currentGrade.setCoefficient(BigDecimal.ONE);
        currentGrade.setSessionDate(LocalDate.of(2026, 11, 18));
        currentGrade.setStatus(GradeStatus.PUBLISHED);
        gradeRepository.saveAndFlush(currentGrade);

        entityManager.clear();

        List<Grade> results = gradeRepository
                .findByStudentIdAndModuleIdAndSessionDateBetween(
                        student.getId(),
                        module.getId(),
                        LocalDate.of(2026, 9, 1),
                        LocalDate.of(2027, 8, 31)
                );

        assertEquals(1, results.size());
        assertEquals(
                0,
                new BigDecimal("16.00")
                        .compareTo(results.get(0).getScore())
        );
    }

    @Test
    void shouldReturnOnlyPublishedGradesOfAuthenticatedStudent() {
        Filiere filiere = new Filiere();
        filiere.setCode("PRIV-FIL-001");
        filiere.setNom("Filiere Confidentialite");
        filiere.setNiveau("MASTER");
        entityManager.persist(filiere);

        ModuleCours module = new ModuleCours();
        module.setCode("PRIV-MOD-001");
        module.setNom("Module Confidentialite");
        module.setSemestre("S1");
        module.setFiliere(filiere);
        entityManager.persist(module);

        User userA = new User();
        userA.setEmail("student.a@test.local");
        userA.setPasswordHash("test-hash");
        userA.setFirstName("Student");
        userA.setLastName("A");
        userA.setRole(Role.ETUDIANT);
        entityManager.persist(userA);

        Student studentA = new Student();
        studentA.setStudentNumber("PRIV-STUDENT-A");
        studentA.setUser(userA);
        entityManager.persist(studentA);

        User userB = new User();
        userB.setEmail("student.b@test.local");
        userB.setPasswordHash("test-hash");
        userB.setFirstName("Student");
        userB.setLastName("B");
        userB.setRole(Role.ETUDIANT);
        entityManager.persist(userB);

        Student studentB = new Student();
        studentB.setStudentNumber("PRIV-STUDENT-B");
        studentB.setUser(userB);
        entityManager.persist(studentB);

        Grade publishedA = new Grade();
        publishedA.setStudent(studentA);
        publishedA.setModule(module);
        publishedA.setScore(new BigDecimal("15.50"));
        publishedA.setType(GradeType.CONTROLE_CONTINU);
        publishedA.setCoefficient(BigDecimal.ONE);
        publishedA.setSessionDate(LocalDate.of(2026, 11, 18));
        publishedA.setStatus(GradeStatus.PUBLISHED);
        gradeRepository.saveAndFlush(publishedA);

        Grade draftA = new Grade();
        draftA.setStudent(studentA);
        draftA.setModule(module);
        draftA.setScore(new BigDecimal("18.00"));
        draftA.setType(GradeType.TP);
        draftA.setCoefficient(BigDecimal.ONE);
        draftA.setSessionDate(LocalDate.of(2026, 11, 19));
        draftA.setStatus(GradeStatus.DRAFT);
        gradeRepository.saveAndFlush(draftA);

        Grade publishedB = new Grade();
        publishedB.setStudent(studentB);
        publishedB.setModule(module);
        publishedB.setScore(new BigDecimal("12.00"));
        publishedB.setType(GradeType.EXAMEN_FINAL);
        publishedB.setCoefficient(BigDecimal.ONE);
        publishedB.setSessionDate(LocalDate.of(2026, 11, 18));
        publishedB.setStatus(GradeStatus.PUBLISHED);
        gradeRepository.saveAndFlush(publishedB);

        entityManager.clear();

        List<Grade> results =
                gradeRepository.findByStudent_User_EmailAndStatus(
                        "student.a@test.local",
                        GradeStatus.PUBLISHED
                );

        assertEquals(1, results.size());
        assertEquals(
                0,
                new BigDecimal("15.50")
                        .compareTo(results.get(0).getScore())
        );
        assertEquals(studentA.getId(), results.get(0).getStudent().getId());
        assertEquals(GradeStatus.PUBLISHED, results.get(0).getStatus());
    }
}