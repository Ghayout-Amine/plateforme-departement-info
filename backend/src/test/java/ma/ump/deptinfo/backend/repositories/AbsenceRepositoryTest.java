package ma.ump.deptinfo.backend.repositories;

import jakarta.persistence.EntityManager;
import ma.ump.deptinfo.backend.entities.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class AbsenceRepositoryTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private AbsenceRepository absenceRepository;

    private Student student;
    private ModuleCours module;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setEmail("absence.student@test.local");
        user.setPasswordHash("test-hash");
        user.setFirstName("Test");
        user.setLastName("Student");
        user.setRole(Role.ETUDIANT);
        entityManager.persist(user);

        student = new Student();
        student.setStudentNumber("ABS-STUDENT-001");
        student.setUser(user);
        entityManager.persist(student);

        Filiere filiere = new Filiere();
        filiere.setCode("ABS-FIL-001");
        filiere.setNom("Filiere Test Absence");
        filiere.setNiveau("MASTER");
        entityManager.persist(filiere);

        module = new ModuleCours();
        module.setCode("ABS-MOD-001");
        module.setNom("Module Test Absence");
        module.setSemestre("S1");
        module.setFiliere(filiere);
        entityManager.persist(module);
    }

    @Test
    void shouldSaveAndFindAbsence() {
        LocalDate date = LocalDate.of(2026, 11, 18);
        String sessionTime = "08:30-10:30";

        Absence absence = new Absence();
        absence.setStudent(student);
        absence.setModule(module);
        absence.setDate(date);
        absence.setSessionTime(sessionTime);

        absenceRepository.saveAndFlush(absence);
        entityManager.clear();

        Optional<Absence> result =
                absenceRepository.findByStudentIdAndModuleIdAndDateAndSessionTime(
                        student.getId(), module.getId(), date, sessionTime
                );

        assertTrue(result.isPresent());
        assertEquals(Boolean.FALSE, result.get().getJustified());

        List<Absence> studentAbsences =
                absenceRepository.findByStudentId(student.getId());

        assertEquals(1, studentAbsences.size());
        assertEquals(module.getId(), result.get().getModule().getId());
    }

    @Test
    void shouldRejectDuplicateAbsence() {
        LocalDate date = LocalDate.of(2026, 11, 18);
        String sessionTime = "08:30-10:30";

        Absence first = new Absence();
        first.setStudent(student);
        first.setModule(module);
        first.setDate(date);
        first.setSessionTime(sessionTime);
        absenceRepository.saveAndFlush(first);

        Absence duplicate = new Absence();
        duplicate.setStudent(student);
        duplicate.setModule(module);
        duplicate.setDate(date);
        duplicate.setSessionTime(sessionTime);

        assertThrows(
                org.springframework.dao.DataIntegrityViolationException.class,
                () -> absenceRepository.saveAndFlush(duplicate)
        );
    }
}