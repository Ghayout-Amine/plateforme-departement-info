package ma.ump.deptinfo.backend.repositories;

import ma.ump.deptinfo.backend.entities.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByStudentNumber(String studentNumber);

    Optional<Student> findByCne(String cne);

    // Recherche par numéro, CNE, nom, prénom ou email (insensible à la casse)
    @Query("""
            SELECT s FROM Student s JOIN s.user u
            WHERE LOWER(s.studentNumber) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(s.cne)           LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(u.firstName)     LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(u.lastName)      LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(u.email)         LIKE LOWER(CONCAT('%', :keyword, '%'))
            """)
    Page<Student> search(@Param("keyword") String keyword, Pageable pageable);
}
