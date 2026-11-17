package ma.ump.deptinfo.backend.repositories;

import ma.ump.deptinfo.backend.entities.AcademicYear;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AcademicYearRepository extends JpaRepository<AcademicYear, Long> {

    List<AcademicYear> findAllByOrderByStartDateDesc();

    Optional<AcademicYear> findByLabel(String label);

    Optional<AcademicYear> findFirstByCurrentTrue();

    List<AcademicYear> findByCurrentTrue();
}
