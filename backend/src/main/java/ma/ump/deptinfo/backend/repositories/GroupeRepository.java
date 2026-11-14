package ma.ump.deptinfo.backend.repositories;

import ma.ump.deptinfo.backend.entities.Groupe;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GroupeRepository extends JpaRepository<Groupe, Long> {

    List<Groupe> findAllByOrderByNameAsc();

    List<Groupe> findByFiliereIdOrderByNameAsc(Long filiereId);

    Optional<Groupe> findByFiliereIdAndNameIgnoreCase(Long filiereId, String name);

    boolean existsByFiliereId(Long filiereId);
}
