package ma.ump.deptinfo.backend.repositories;

import jakarta.persistence.EntityManager;
import ma.ump.deptinfo.backend.entities.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PGDI2026-75 : requêtes JPQL de l'emploi du temps et des examens,
 * exécutées sur la base H2 des tests (chevauchement, filtres, "mon emploi du temps").
 */
@SpringBootTest
@Transactional
class PlanningExamenRepositoryTest {

    private static final LocalDate MONDAY = LocalDate.of(2026, 11, 23);

    @Autowired
    private EntityManager entityManager;
    @Autowired
    private PlanningRepository planningRepository;
    @Autowired
    private ExamenRepository examenRepository;

    private Teacher teacher;
    private ModuleCours module;
    private Groupe groupe;
    private Salle salle;
    private Salle amphi;
    private AcademicYear year;

    @BeforeEach
    void setUp() {
        User teacherUser = user("planning.teacher@test.local", Role.ENSEIGNANT);
        teacher = new Teacher();
        teacher.setEmployeeNumber("PLN-ENS-001");
        teacher.setUser(teacherUser);
        entityManager.persist(teacher);

        Filiere filiere = new Filiere();
        filiere.setCode("PLN-FIL-001");
        filiere.setNom("Filière Test Planning");
        filiere.setNiveau("MASTER");
        entityManager.persist(filiere);

        module = new ModuleCours();
        module.setCode("PLN-MOD-001");
        module.setNom("Module Test Planning");
        module.setSemestre("S3");
        module.setFiliere(filiere);
        module.setEnseignant(teacherUser);
        entityManager.persist(module);

        groupe = new Groupe();
        groupe.setName("Groupe Test Planning");
        groupe.setCapacity(30);
        groupe.setFiliere(filiere);
        entityManager.persist(groupe);

        salle = salle("PLN-S1", 40);
        amphi = salle("PLN-AMPHI", 150);

        year = new AcademicYear();
        year.setLabel("2097-2098");
        year.setStartDate(LocalDate.of(2026, 9, 1));
        year.setEndDate(LocalDate.of(2027, 7, 15));
        entityManager.persist(year);

        // Un étudiant inscrit dans le groupe
        Student student = new Student();
        student.setStudentNumber("PLN-ETU-001");
        student.setUser(user("planning.student@test.local", Role.ETUDIANT));
        entityManager.persist(student);
        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setFiliere(filiere);
        enrollment.setAcademicYear(year);
        enrollment.setGroupe(groupe);
        entityManager.persist(enrollment);

        // Séance de référence : lundi 08:30-10:30
        planningRepository.saveAndFlush(planning(MONDAY, LocalTime.of(8, 30), LocalTime.of(10, 30)));
    }

    private User user(String email, Role role) {
        User u = new User();
        u.setEmail(email);
        u.setPasswordHash("test-hash");
        u.setFirstName("Test");
        u.setLastName(role.name());
        u.setRole(role);
        entityManager.persist(u);
        return u;
    }

    private Salle salle(String code, int capacity) {
        Salle s = new Salle();
        s.setCode(code);
        s.setName("Salle " + code);
        s.setCapacity(capacity);
        s.setRoomType(RoomType.CLASSROOM);
        s.setActive(true);
        entityManager.persist(s);
        return s;
    }

    private Planning planning(LocalDate date, LocalTime start, LocalTime end) {
        Planning p = new Planning();
        p.setTeacher(teacher);
        p.setModule(module);
        p.setGroupe(groupe);
        p.setSalle(salle);
        p.setSessionType(SessionType.CM);
        p.setSessionDate(date);
        p.setStartTime(start);
        p.setEndTime(end);
        return p;
    }

    @Test
    void findOverlapping_detectsOverlapButNotAdjacentSlots() {
        List<Planning> overlap = planningRepository.findOverlapping(
                MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0), null, PlanningStatus.PLANNED);
        List<Planning> adjacent = planningRepository.findOverlapping(
                MONDAY, LocalTime.of(10, 30), LocalTime.of(12, 30), null, PlanningStatus.PLANNED);
        List<Planning> otherDay = planningRepository.findOverlapping(
                MONDAY.plusDays(1), LocalTime.of(9, 0), LocalTime.of(11, 0), null, PlanningStatus.PLANNED);

        assertEquals(1, overlap.size());
        assertTrue(adjacent.isEmpty(), "10:30-12:30 suit la séance sans la chevaucher");
        assertTrue(otherDay.isEmpty());
    }

    @Test
    void findOverlapping_ignoresCancelledSessionsAndExcludedId() {
        Planning cancelled = planning(MONDAY, LocalTime.of(14, 0), LocalTime.of(16, 0));
        cancelled.cancel();
        planningRepository.saveAndFlush(cancelled);
        Planning planned = planningRepository.findOverlapping(
                MONDAY, LocalTime.of(8, 30), LocalTime.of(10, 30), null, PlanningStatus.PLANNED).get(0);

        assertTrue(planningRepository.findOverlapping(
                MONDAY, LocalTime.of(15, 0), LocalTime.of(17, 0), null, PlanningStatus.PLANNED).isEmpty());
        assertTrue(planningRepository.findOverlapping(
                MONDAY, LocalTime.of(8, 30), LocalTime.of(10, 30), planned.getId(), PlanningStatus.PLANNED).isEmpty());
    }

    @Test
    void searchAndPersonalTimetables() {
        LocalDate sunday = MONDAY.plusDays(6);
        planningRepository.saveAndFlush(planning(MONDAY.plusDays(2), LocalTime.of(14, 0), LocalTime.of(16, 0)));
        planningRepository.saveAndFlush(planning(MONDAY.plusWeeks(1), LocalTime.of(8, 30), LocalTime.of(10, 30)));

        List<Planning> week = planningRepository.search(MONDAY, sunday, groupe.getId(), null, null);
        assertEquals(2, week.size(), "la séance de la semaine suivante est exclue");
        assertEquals(MONDAY, week.get(0).getSessionDate(), "tri par date puis par heure");

        assertEquals(2, planningRepository.search(MONDAY, sunday, null, teacher.getId(), salle.getId()).size());
        assertEquals(2, planningRepository.findForStudent(
                "planning.student@test.local", MONDAY, sunday, EnrollmentStatus.ACTIVE).size());
        assertEquals(2, planningRepository.findForTeacher("planning.teacher@test.local", MONDAY, sunday).size());
        assertTrue(planningRepository.findForStudent(
                "inconnu@test.local", MONDAY, sunday, EnrollmentStatus.ACTIVE).isEmpty());
    }

    @Test
    void examenQueries() {
        Examen exam = new Examen();
        exam.setTitle("Examen test");
        exam.setExamType(ExamType.FINAL);
        exam.setModule(module);
        exam.setGroupe(groupe);
        exam.setAcademicYear(year);
        exam.setSalles(new java.util.LinkedHashSet<>(Set.of(salle, amphi)));
        exam.setExamDate(LocalDate.of(2027, 1, 18));
        exam.setStartTime(LocalTime.of(9, 0));
        exam.setEndTime(LocalTime.of(11, 0));
        examenRepository.saveAndFlush(exam);
        entityManager.clear();

        LocalDate from = LocalDate.of(2027, 1, 1);
        LocalDate to = LocalDate.of(2027, 1, 31);
        List<Examen> bySalle = examenRepository.search(from, to, null, null, amphi.getId());
        assertEquals(1, bySalle.size());
        assertEquals(190, bySalle.get(0).totalCapacity());
        assertEquals(1, examenRepository.search(from, to, null, null, null).size(), "DISTINCT : un examen dans 2 salles compte une fois");
        assertEquals(1, examenRepository.findOverlapping(
                LocalDate.of(2027, 1, 18), LocalTime.of(10, 0), LocalTime.of(12, 0), null).size());
        assertEquals(1, examenRepository.findForStudent(
                "planning.student@test.local", from, to, EnrollmentStatus.ACTIVE).size());
        assertEquals(1, examenRepository.findForTeacher("planning.teacher@test.local", from, to).size());
    }
}
