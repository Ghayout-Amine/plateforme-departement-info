package ma.ump.deptinfo.backend.repositories;

import ma.ump.deptinfo.backend.entities.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    // Une seule inscription par étudiant et par année universitaire
    Optional<Enrollment> findByStudentIdAndAcademicYearId(Long studentId, Long academicYearId);

    // Historique des inscriptions d'un étudiant (année la plus récente d'abord)
    List<Enrollment> findByStudentIdOrderByAcademicYearStartDateDesc(Long studentId);

    // Nombre d'étudiants déjà affectés à un groupe (contrôle de l'effectif maximal)
    long countByGroupeId(Long groupeId);

    // Liste filtrée : chaque filtre est facultatif (null = ignoré)
    @Query("""
            SELECT e FROM Enrollment e
            JOIN e.student s JOIN s.user u
            WHERE (:academicYearId IS NULL OR e.academicYear.id = :academicYearId)
              AND (:filiereId IS NULL OR e.filiere.id = :filiereId)
              AND (:groupeId IS NULL OR e.groupe.id = :groupeId)
            ORDER BY u.lastName ASC, u.firstName ASC
            """)
    List<Enrollment> search(@Param("academicYearId") Long academicYearId,
                            @Param("filiereId") Long filiereId,
                            @Param("groupeId") Long groupeId);

}
