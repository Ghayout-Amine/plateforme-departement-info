package ma.ump.deptinfo.backend.repositories;

import ma.ump.deptinfo.backend.entities.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TeacherRepository extends JpaRepository<Teacher, Long> {

    Optional<Teacher> findByEmployeeNumber(String employeeNumber);

    boolean existsByUserId(Long userId);
}
