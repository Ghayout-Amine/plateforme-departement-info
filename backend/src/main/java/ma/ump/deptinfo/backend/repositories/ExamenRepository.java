package ma.ump.deptinfo.backend.repositories;

import ma.ump.deptinfo.backend.entities.EnrollmentStatus;
import ma.ump.deptinfo.backend.entities.Examen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Calendrier des examens (PGDI2026-75, US49).
 */
public interface ExamenRepository extends JpaRepository<Examen, Long> {

    // Examens d'une période, filtrés par groupe, salle ou module (filtres facultatifs)
    @Query("""
            SELECT DISTINCT e FROM Examen e
            LEFT JOIN e.salles s
            WHERE e.examDate BETWEEN :from AND :to
              AND (:groupeId IS NULL OR e.groupe.id = :groupeId)
              AND (:moduleId IS NULL OR e.module.id = :moduleId)
              AND (:salleId IS NULL OR s.id = :salleId)
            ORDER BY e.examDate ASC, e.startTime ASC
            """)
    List<Examen> search(@Param("from") LocalDate from,
                        @Param("to") LocalDate to,
                        @Param("groupeId") Long groupeId,
                        @Param("moduleId") Long moduleId,
                        @Param("salleId") Long salleId);

    // Examens qui chevauchent le créneau [start, end[ le même jour
    @Query("""
            SELECT e FROM Examen e
            WHERE e.examDate = :date
              AND e.startTime < :end
              AND e.endTime > :start
              AND (:excludeId IS NULL OR e.id <> :excludeId)
            """)
    List<Examen> findOverlapping(@Param("date") LocalDate date,
                                 @Param("start") LocalTime start,
                                 @Param("end") LocalTime end,
                                 @Param("excludeId") Long excludeId);

    // Examens d'un étudiant : ceux des groupes de ses inscriptions actives
    @Query("""
            SELECT e FROM Examen e
            WHERE e.examDate BETWEEN :from AND :to
              AND e.groupe.id IN (
                  SELECT enr.groupe.id FROM Enrollment enr
                  WHERE enr.student.user.email = :email
                    AND enr.status = :enrollmentStatus
                    AND enr.groupe IS NOT NULL)
            ORDER BY e.examDate ASC, e.startTime ASC
            """)
    List<Examen> findForStudent(@Param("email") String email,
                                @Param("from") LocalDate from,
                                @Param("to") LocalDate to,
                                @Param("enrollmentStatus") EnrollmentStatus enrollmentStatus);

    // Examens des modules dont l'enseignant est responsable
    @Query("""
            SELECT e FROM Examen e
            WHERE e.examDate BETWEEN :from AND :to
              AND e.module.enseignant.email = :email
            ORDER BY e.examDate ASC, e.startTime ASC
            """)
    List<Examen> findForTeacher(@Param("email") String email,
                                @Param("from") LocalDate from,
                                @Param("to") LocalDate to);
}
