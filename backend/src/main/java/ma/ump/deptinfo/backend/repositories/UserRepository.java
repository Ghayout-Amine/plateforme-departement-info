
package ma.ump.deptinfo.backend.repositories;

import ma.ump.deptinfo.backend.entities.Role;
import ma.ump.deptinfo.backend.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    List<User> findByRole(Role role);

    List<User> findByEnabled(Boolean enabled);

    Page<User> findAll(Pageable pageable);

    @Query("SELECT u FROM User u WHERE " +
           "(:role IS NULL OR u.role = :role) AND " +
           "(:enabled IS NULL OR u.enabled = :enabled) AND " +
           "(:search IS NULL OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           " OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           " OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<User> findWithFilters(@Param("role") Role role,
                                @Param("enabled") Boolean enabled,
                                @Param("search") String search,
                                Pageable pageable);
}
