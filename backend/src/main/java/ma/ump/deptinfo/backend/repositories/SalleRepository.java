package ma.ump.deptinfo.backend.repositories;

import ma.ump.deptinfo.backend.entities.Salle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SalleRepository extends JpaRepository<Salle, Long> {

    Optional<Salle> findByCodeIgnoreCase(String code);

    List<Salle> findAllByOrderByCodeAsc();
}
