package ma.ump.deptinfo.backend.repositories;

import ma.ump.deptinfo.backend.entities.Absence;
import org.springframework.data.jpa.repository.JpaRepository;

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
}