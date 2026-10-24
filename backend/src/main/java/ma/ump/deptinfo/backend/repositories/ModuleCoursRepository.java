package ma.ump.deptinfo.backend.repositories;

import ma.ump.deptinfo.backend.entities.ModuleCours;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ModuleCoursRepository extends JpaRepository<ModuleCours, Long> {

    Optional<ModuleCours> findByCode(String code);

    List<ModuleCours> findByFiliereId(Long filiereId);
}