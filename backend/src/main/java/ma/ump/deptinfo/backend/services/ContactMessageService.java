package ma.ump.deptinfo.backend.services;

import ma.ump.deptinfo.backend.dtos.ContactMessageDTO;
import ma.ump.deptinfo.backend.dtos.ContactMessageResponseDTO;
import ma.ump.deptinfo.backend.entities.ContactMessage;
import ma.ump.deptinfo.backend.entities.MessageStatus;
import ma.ump.deptinfo.backend.exceptions.ResourceNotFoundException;
import ma.ump.deptinfo.backend.repositories.ContactMessageRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ContactMessageService {

    private final ContactMessageRepository contactMessageRepository;

    public ContactMessageService(ContactMessageRepository contactMessageRepository) {
        this.contactMessageRepository = contactMessageRepository;
    }

    public ContactMessageResponseDTO saveMessage(ContactMessageDTO dto) {
        ContactMessage contactMessage = new ContactMessage(
                dto.getFullName().trim(),
                dto.getEmail().trim().toLowerCase(),
                dto.getSubject().trim(),
                dto.getMessage().trim()
        );
        ContactMessage saved = contactMessageRepository.save(contactMessage);
        return toResponseDTO(saved);
    }

    @Transactional(readOnly = true)
    public Page<ContactMessageResponseDTO> getMessages(MessageStatus status, Pageable pageable) {
        Page<ContactMessage> page = (status == null)
                ? contactMessageRepository.findAll(pageable)
                : contactMessageRepository.findByStatus(status, pageable);
        return page.map(this::toResponseDTO);
    }

    public ContactMessageResponseDTO markAsRead(Long id) {
        ContactMessage message = contactMessageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Message introuvable avec l'id : " + id));
        message.markAsRead();
        return toResponseDTO(contactMessageRepository.save(message));
    }

    private ContactMessageResponseDTO toResponseDTO(ContactMessage message) {
        return new ContactMessageResponseDTO(
                message.getId(),
                message.getFullName(),
                message.getEmail(),
                message.getSubject(),
                message.getMessage(),
                message.getStatus(),
                message.getCreatedAt()
        );
    }
}
