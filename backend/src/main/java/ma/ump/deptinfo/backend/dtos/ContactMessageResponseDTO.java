package ma.ump.deptinfo.backend.dtos;

import ma.ump.deptinfo.backend.entities.MessageStatus;

import java.time.LocalDateTime;

public class ContactMessageResponseDTO {

    private Long id;
    private String fullName;
    private String email;
    private String subject;
    private String message;
    private MessageStatus status;
    private LocalDateTime createdAt;

    public ContactMessageResponseDTO() {
    }

    public ContactMessageResponseDTO(Long id, String fullName, String email, String subject,
                                     String message, MessageStatus status, LocalDateTime createdAt) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.subject = subject;
        this.message = message;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getSubject() {
        return subject;
    }

    public String getMessage() {
        return message;
    }

    public MessageStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
