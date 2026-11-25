package ma.ump.deptinfo.backend.dtos;

import ma.ump.deptinfo.backend.entities.InternshipStatus;
import ma.ump.deptinfo.backend.entities.InternshipType;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class InternshipResponseDTO {
    private Long id;
    private String subject;
    private InternshipType internshipType;
    private String companyName;
    private String companyCity;
    private String companyCountry;
    private LocalDate startDate;
    private LocalDate endDate;
    private InternshipStatus status;
    private String companyTutorName;
    private String companyTutorEmail;
    private String companyTutorPhone;
    private LocalDateTime declaredAt;
    private String validationComment;
    private LocalDateTime validatedAt;
    private Long studentId;
    private String studentName;
    private String studentNumber;
    private Long supervisorId;
    private String supervisorName;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
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
    public LocalDateTime getDeclaredAt() { return declaredAt; }
    public void setDeclaredAt(LocalDateTime declaredAt) { this.declaredAt = declaredAt; }
    public String getValidationComment() { return validationComment; }
    public void setValidationComment(String validationComment) { this.validationComment = validationComment; }
    public LocalDateTime getValidatedAt() { return validatedAt; }
    public void setValidatedAt(LocalDateTime validatedAt) { this.validatedAt = validatedAt; }
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public String getStudentNumber() { return studentNumber; }
    public void setStudentNumber(String studentNumber) { this.studentNumber = studentNumber; }
    public Long getSupervisorId() { return supervisorId; }
    public void setSupervisorId(Long supervisorId) { this.supervisorId = supervisorId; }
    public String getSupervisorName() { return supervisorName; }
    public void setSupervisorName(String supervisorName) { this.supervisorName = supervisorName; }
}
