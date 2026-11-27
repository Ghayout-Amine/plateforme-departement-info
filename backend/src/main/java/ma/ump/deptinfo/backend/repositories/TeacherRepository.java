package ma.ump.deptinfo.backend.repositories;

import ma.ump.deptinfo.backend.entities.Role;
import ma.ump.deptinfo.backend.entities.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TeacherRepository extends JpaRepository<Teacher, Long> {

    Optional<Teacher> findByEmployeeNumber(String employeeNumber);

    boolean existsByUserId(Long userId);

    Optional<Teacher> findByUserId(Long userId);

    // PGDI2026-92 : uniquement les fiches dont le compte a le rôle demandé (ENSEIGNANT) et n'est pas supprimé.
    // activeOnly = true écarte aussi les comptes désactivés (listes déroulantes de planification).
    @Query("select t from Teacher t join fetch t.user u "
            + "where u.role = :role and u.deletedAt is null "
            + "and (:activeOnly = false or u.enabled = true) "
            + "order by u.lastName, u.firstName")
    List<Teacher> findByUserRole(@Param("role") Role role, @Param("activeOnly") boolean activeOnly);
}
