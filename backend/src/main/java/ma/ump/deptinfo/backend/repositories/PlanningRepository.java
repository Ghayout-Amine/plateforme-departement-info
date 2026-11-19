package ma.ump.deptinfo.backend.repositories;

import ma.ump.deptinfo.backend.entities.EnrollmentStatus;
import ma.ump.deptinfo.backend.entities.Planning;
import ma.ump.deptinfo.backend.entities.PlanningStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Séances de l'emploi du temps (PGDI2026-75, US36).
 */
public interface PlanningRepository extends JpaRepository<Planning, Long> {

    // Séances d'une période, filtrées par groupe, enseignant ou salle (filtres facultatifs)
    @Query("""
            SELECT p FROM Planning p
            WHERE p.sessionDate BETWEEN :from AND :to
              AND (:groupeId IS NULL OR p.groupe.id = :groupeId)
              AND (:teacherId IS NULL OR p.teacher.id = :teacherId)
              AND (:salleId IS NULL OR p.salle.id = :salleId)
            ORDER BY p.sessionDate ASC, p.startTime ASC
            """)
    List<Planning> search(@Param("from") LocalDate from,
                          @Param("to") LocalDate to,
                          @Param("groupeId") Long groupeId,
                          @Param("teacherId") Long teacherId,
                          @Param("salleId") Long salleId);

    /*
     * Séances actives qui chevauchent le créneau [start, end[ le même jour.
     * Deux créneaux se chevauchent si l'un commence avant la fin de l'autre
     * et finit après son début. excludeId = la séance en cours de modification.
     */
    @Query("""
            SELECT p FROM Planning p
            WHERE p.status = :status
              AND p.sessionDate = :date
              AND p.startTime < :end
              AND p.endTime > :start
              AND (:excludeId IS NULL OR p.id <> :excludeId)
            """)
    List<Planning> findOverlapping(@Param("date") LocalDate date,
                                   @Param("start") LocalTime start,
                                   @Param("end") LocalTime end,
                                   @Param("excludeId") Long excludeId,
                                   @Param("status") PlanningStatus status);

    // Emploi du temps d'un étudiant : séances des groupes de ses inscriptions actives
    @Query("""
            SELECT p FROM Planning p
            WHERE p.sessionDate BETWEEN :from AND :to
              AND p.groupe.id IN (
                  SELECT e.groupe.id FROM Enrollment e
                  WHERE e.student.user.email = :email
                    AND e.status = :enrollmentStatus
                    AND e.groupe IS NOT NULL)
            ORDER BY p.sessionDate ASC, p.startTime ASC
            """)
    List<Planning> findForStudent(@Param("email") String email,
                                  @Param("from") LocalDate from,
                                  @Param("to") LocalDate to,
                                  @Param("enrollmentStatus") EnrollmentStatus enrollmentStatus);

    // Emploi du temps d'un enseignant (identifié par l'email de son compte)
    @Query("""
            SELECT p FROM Planning p
            WHERE p.sessionDate BETWEEN :from AND :to
              AND p.teacher.user.email = :email
            ORDER BY p.sessionDate ASC, p.startTime ASC
            """)
    List<Planning> findForTeacher(@Param("email") String email,
                                  @Param("from") LocalDate from,
                                  @Param("to") LocalDate to);
}
