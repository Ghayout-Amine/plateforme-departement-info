package ma.ump.deptinfo.backend.dtos;

import jakarta.validation.constraints.*;
import ma.ump.deptinfo.backend.entities.InternshipType;

import java.time.LocalDate;

/** Déclaration d'un stage/PFE par l'étudiant (PGDI2026-80). */
public class InternshipRequestDTO {

    @NotBlank(message = "Le sujet est obligatoire")
    @Size(min = 5, max = 255, message = "Le sujet doit contenir entre 5 et 255 caractères")
    private String subject;

    @NotNull(message = "Le type (stage ou PFE) est obligatoire")
    private InternshipType internshipType;

    @NotBlank(message = "L'entreprise est obligatoire")
    @Size(max = 200)
    private String companyName;

    @Size(max = 100)
    private String companyCity;

    @Pattern(regexp = "^$|^[A-Za-z]{2}$", message = "Le pays doit être un code à 2 lettres (ex : MA)")
    private String companyCountry;

    @NotNull(message = "La date de début est obligatoire")
    private LocalDate startDate;

    @NotNull(message = "La date de fin est obligatoire")
    private LocalDate endDate;

    @Size(max = 150)
    private String companyTutorName;

    @Email(message = "Format d'email du tuteur invalide")
    @Size(max = 150)
    private String companyTutorEmail;

    @Size(max = 30)
    private String companyTutorPhone;

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public InternshipType getInternshipType() { return internshipType; }
    public void setInternshipType(InternshipType internshipType) { this.internshipType = internshipType; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getCompanyCity() { return companyCity; }
    public void setCompanyCity(String companyCity) { this.companyCity = companyCity; }
    public String getCompanyCountry() { return companyCountry; }
    public void setCompanyCountry(String companyCountry) { this.companyCountry = companyCountry; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public String getCompanyTutorName() { return companyTutorName; }
    public void setCompanyTutorName(String companyTutorName) { this.companyTutorName = companyTutorName; }
    public String getCompanyTutorEmail() { return companyTutorEmail; }
    public void setCompanyTutorEmail(String companyTutorEmail) { this.companyTutorEmail = companyTutorEmail; }
    public String getCompanyTutorPhone() { return companyTutorPhone; }
    public void setCompanyTutorPhone(String companyTutorPhone) { this.companyTutorPhone = companyTutorPhone; }
}
