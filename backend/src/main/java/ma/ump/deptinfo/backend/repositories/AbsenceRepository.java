
package ma.ump.deptinfo.backend.repositories;

import jakarta.persistence.LockModeType;

import ma.ump.deptinfo.backend.entities.Absence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AbsenceRepository extends JpaRepository<Absence, Long> {

    List<Absence> findByStudentId(Long studentId);

    List<Absence> findByModuleId(Long moduleId);

    List<Absence> findByStudentIdAndModuleId(
            Long studentId,
            Long moduleId
    );

    Optional<Absence> findByStudentIdAndModuleIdAndDateAndSessionTime(
            Long studentId,
            Long moduleId,
            LocalDate date,
            String sessionTime
    );

    List<Absence> findByStudent_User_Email(String email);

    // PGDI2026-91 : verrouillage pendant le depot
    // ou la validation du justificatif medical.

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Absence a WHERE a.id = :id")
    Optional<Absence> findByIdForUpdate(@Param("id") Long id);
}
