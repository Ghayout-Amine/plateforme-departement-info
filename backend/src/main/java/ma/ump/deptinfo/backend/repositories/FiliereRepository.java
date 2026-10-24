package ma.ump.deptinfo.backend.repositories;

import ma.ump.deptinfo.backend.entities.Filiere;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FiliereRepository extends JpaRepository<Filiere, Long> {

    Optional<Filiere> findByCode(String code);
}