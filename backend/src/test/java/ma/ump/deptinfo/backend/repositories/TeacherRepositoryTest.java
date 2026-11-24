package ma.ump.deptinfo.backend.repositories;

import jakarta.persistence.EntityManager;
import ma.ump.deptinfo.backend.entities.Role;
import ma.ump.deptinfo.backend.entities.Teacher;
import ma.ump.deptinfo.backend.entities.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PGDI2026-92 : GET /api/teachers ne doit renvoyer que de vrais enseignants.
 * Même si une fiche « teachers » est rattachée par erreur à un compte étudiant (import, script SQL…),
 * elle n'apparaît pas dans la liste.
 */
@SpringBootTest
@Transactional
class TeacherRepositoryTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TeacherRepository teacherRepository;

    @BeforeEach
    void setUp() {
        teacher("ENS-T-01", user("nadia.alami@t.local", "Nadia", "Alami", Role.ENSEIGNANT, true, null));
        teacher("ENS-T-02", user("youssef.bennani@t.local", "Youssef", "Bennani", Role.ENSEIGNANT, true, null));
        teacher("ENS-T-03", user("desactive@t.local", "Hanae", "Desactive", Role.ENSEIGNANT, false, null));
        teacher("ENS-T-04", user("supprime@t.local", "Samir", "Supprime", Role.ENSEIGNANT, true, LocalDateTime.now()));
        // fiche enseignant rattachée par erreur à un compte étudiant : ne doit jamais sortir
        teacher("ENS-T-05", user("omar.tazi@etu.t.local", "Omar", "Tazi", Role.ETUDIANT, true, null));
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("seuls les comptes ENSEIGNANT non supprimés sont renvoyés, triés par nom")
    void onlyTeacherAccounts() {
        List<String> emails = teacherRepository.findByUserRole(Role.ENSEIGNANT, false).stream()
                .map(t -> t.getUser().getEmail())
                .filter(e -> e.endsWith("t.local"))   // ignore les données d'autres tests éventuelles
                .toList();

        assertEquals(List.of("nadia.alami@t.local", "youssef.bennani@t.local", "desactive@t.local"), emails);
        assertFalse(emails.contains("omar.tazi@etu.t.local"), "aucun étudiant");
        assertFalse(emails.contains("supprime@t.local"), "aucun compte supprimé");
    }

    @Test
    @DisplayName("activeOnly : les comptes désactivés sont aussi écartés (formulaire de séance)")
    void activeOnlyExcludesDisabledAccounts() {
        List<String> emails = teacherRepository.findByUserRole(Role.ENSEIGNANT, true).stream()
                .map(t -> t.getUser().getEmail())
                .filter(e -> e.endsWith("t.local"))   // ignore les données d'autres tests éventuelles
                .toList();

        assertEquals(List.of("nadia.alami@t.local", "youssef.bennani@t.local"), emails);
    }

    private User user(String email, String firstName, String lastName, Role role, boolean enabled, LocalDateTime deletedAt) {
        User u = new User();
        u.setEmail(email);
        u.setPasswordHash("test-hash");
        u.setFirstName(firstName);
        u.setLastName(lastName);
        u.setRole(role);
        u.setEnabled(enabled);
        u.setDeletedAt(deletedAt);
        entityManager.persist(u);
        return u;
    }

    private void teacher(String employeeNumber, User user) {
        Teacher t = new Teacher();
        t.setEmployeeNumber(employeeNumber);
        t.setUser(user);
        entityManager.persist(t);
    }
}
