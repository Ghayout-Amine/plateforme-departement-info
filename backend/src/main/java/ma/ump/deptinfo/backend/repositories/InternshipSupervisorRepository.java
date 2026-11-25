package ma.ump.deptinfo.backend.repositories;

import ma.ump.deptinfo.backend.entities.InternshipSupervisor;
import ma.ump.deptinfo.backend.entities.InternshipSupervisorId;
import ma.ump.deptinfo.backend.entities.SupervisorRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InternshipSupervisorRepository extends JpaRepository<InternshipSupervisor, InternshipSupervisorId> {

    Optional<InternshipSupervisor> findFirstByInternshipIdAndSupervisorRole(Long internshipId, SupervisorRole role);
}
