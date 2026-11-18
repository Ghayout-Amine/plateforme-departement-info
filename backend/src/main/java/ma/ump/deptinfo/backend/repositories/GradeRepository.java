package ma.ump.deptinfo.backend.repositories;

import ma.ump.deptinfo.backend.entities.Grade;
import ma.ump.deptinfo.backend.entities.GradeStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GradeRepository extends JpaRepository<Grade, Long> {

    List<Grade> findByStudentId(Long studentId);

    List<Grade> findByModuleId(Long moduleId);

    List<Grade> findByStudentIdAndStatus(
            Long studentId,
            GradeStatus status
    );

    List<Grade> findByStudentIdAndModuleId(
            Long studentId,
            Long moduleId
    );

    List<Grade> findByStudentIdAndModuleIdAndSessionDateBetween(
            Long studentId,
            Long moduleId,
            java.time.LocalDate startDate,
            java.time.LocalDate endDate
    );

    List<Grade> findByStudent_User_EmailAndStatus(
            String email,
            GradeStatus status
    );
}