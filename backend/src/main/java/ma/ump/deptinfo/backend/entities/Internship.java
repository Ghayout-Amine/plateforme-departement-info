package ma.ump.deptinfo.backend.entities;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Stage ou PFE déclaré par un étudiant avec sa convention (PGDI2026-80, US66/US21).
 * Table internships du schéma V1. Le département valide ou refuse la convention.
 */
@Entity
@Table(name = "internships")
public class Internship extends BaseEntity {

    @Column(nullable = false, length = 255)
    private String subject;

    @Enumerated(EnumType.STRING)
    @Column(name = "internship_type", nullable = false, length = 20)
    private InternshipType internshipType = InternshipType.PFE;

    @Column(name = "company_name", nullable = false, length = 200)
    private String companyName;

    @Column(name = "company_city", length = 100)
    private String companyCity;

    @Column(name = "company_country", length = 2)
    private String companyCountry;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private InternshipStatus status = InternshipStatus.PENDING_VALIDATION;

    @Column(name = "company_tutor_name", length = 150)
    private String companyTutorName;

    @Column(name = "company_tutor_email", length = 150)
    private String companyTutorEmail;

    @Column(name = "company_tutor_phone", length = 30)
    private String companyTutorPhone;

    // Nom du fichier PDF de la convention (stocké hors base, voir AgreementStorageService)
    @Column(name = "agreement_file_path", nullable = false, length = 500)
    private String agreementFilePath;

    @Column(name = "declared_at", nullable = false)
    private LocalDateTime declaredAt = LocalDateTime.now();

    @Column(name = "validation_comment", length = 500)
    private String validationComment;

    @Column(name = "validated_at")
    private LocalDateTime validatedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    // Responsable du département qui a pris la décision
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "validated_by")
    private User validatedBy;

    public Internship() {
    }

    /** Décision du département : valide ou refuse la convention. */
    public void decide(boolean approved, String comment, User by) {
        this.status = approved ? InternshipStatus.VALIDATED : InternshipStatus.REJECTED;
        this.validationComment = comment;
        this.validatedBy = by;
        this.validatedAt = LocalDateTime.now();
    }

    /** Affectation d'un encadrant : la convention validée passe automatiquement « en cours ». */
    public void startSupervision() {
        if (status == InternshipStatus.VALIDATED) {
            this.status = InternshipStatus.IN_PROGRESS;
        }
    }

    /** Vrai tant que le dossier occupe la place « un stage actif par type et par étudiant ». */
    public boolean isActive() {
        return status == InternshipStatus.PENDING_VALIDATION
                || status == InternshipStatus.VALIDATED
                || status == InternshipStatus.IN_PROGRESS;
    }

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
    public InternshipStatus getStatus() { return status; }
    public void setStatus(InternshipStatus status) { this.status = status; }
    public String getCompanyTutorName() { return companyTutorName; }
    public void setCompanyTutorName(String companyTutorName) { this.companyTutorName = companyTutorName; }
    public String getCompanyTutorEmail() { return companyTutorEmail; }
    public void setCompanyTutorEmail(String companyTutorEmail) { this.companyTutorEmail = companyTutorEmail; }
    public String getCompanyTutorPhone() { return companyTutorPhone; }
    public void setCompanyTutorPhone(String companyTutorPhone) { this.companyTutorPhone = companyTutorPhone; }
    public String getAgreementFilePath() { return agreementFilePath; }
    public void setAgreementFilePath(String agreementFilePath) { this.agreementFilePath = agreementFilePath; }
    public LocalDateTime getDeclaredAt() { return declaredAt; }
    public void setDeclaredAt(LocalDateTime declaredAt) { this.declaredAt = declaredAt; }
    public String getValidationComment() { return validationComment; }
    public LocalDateTime getValidatedAt() { return validatedAt; }
    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }
    public User getValidatedBy() { return validatedBy; }
}
