package ma.ump.deptinfo.backend.repositories;

import ma.ump.deptinfo.backend.entities.ContactMessage;
import ma.ump.deptinfo.backend.entities.MessageStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactMessageRepository extends JpaRepository<ContactMessage, Long> {

    Page<ContactMessage> findByStatus(MessageStatus status, Pageable pageable);
}
