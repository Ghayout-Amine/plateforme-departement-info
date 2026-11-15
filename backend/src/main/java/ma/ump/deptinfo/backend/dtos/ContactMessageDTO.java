package ma.ump.deptinfo.backend.dtos;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ContactMessageDTO {

    @NotBlank(message = "Le nom complet est obligatoire")
    @Size(min = 2, max = 100, message = "Le nom doit contenir entre 2 et 100 caractères")
    @JsonAlias({"nom", "fullName"})
    private String fullName;

    @NotBlank(message = "L'adresse email est obligatoire")
    @Email(message = "Format d'adresse email invalide")
    @Size(max = 150)
    private String email;

    @NotBlank(message = "Le sujet est obligatoire")
    @Size(min = 3, max = 200, message = "Le sujet doit contenir entre 3 et 200 caractères")
    @JsonAlias({"sujet", "subject"})
    private String subject;

    @NotBlank(message = "Le message est obligatoire")
    @Size(min = 10, max = 2000, message = "Le message doit contenir entre 10 et 2000 caractères")
    private String message;

    public ContactMessageDTO() {
    }

    public ContactMessageDTO(String fullName, String email, String subject, String message) {
        this.fullName = fullName;
        this.email = email;
        this.subject = subject;
        this.message = message;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
