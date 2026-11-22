package ma.ump.deptinfo.backend.repositories;

import ma.ump.deptinfo.backend.entities.Internship;
import ma.ump.deptinfo.backend.entities.InternshipStatus;
import ma.ump.deptinfo.backend.entities.InternshipType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface InternshipRepository extends JpaRepository<Internship, Long> {

    // Stages d'un étudiant, du plus récent au plus ancien
    List<Internship> findByStudentIdOrderByDeclaredAtDesc(Long studentId);

    // Un étudiant a-t-il déjà un dossier actif de ce type ?
    boolean existsByStudentIdAndInternshipTypeAndStatusIn(
            Long studentId, InternshipType type, Collection<InternshipStatus> statuses);

    // Liste du département : tous les dossiers, ou ceux d'un statut
    List<Internship> findAllByOrderByDeclaredAtDesc();

    List<Internship> findByStatusOrderByDeclaredAtDesc(InternshipStatus status);
}
