package ma.ump.deptinfo.backend.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

@Entity
@Table(
    name = "absences",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_absence_student_module_session",
        columnNames = {"student_id", "module_id", "date", "session_time"}
    )
)
public class Absence extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "module_id", nullable = false)
    private ModuleCours module;

    @NotNull
    @Column(nullable = false)
    private LocalDate date;

    @NotBlank
    @Column(name = "session_time", nullable = false, length = 50)
    private String sessionTime;

    @NotNull
    @Column(nullable = false)
    private Boolean justified = false;

    @Column(length = 500)
    private String motif;

    public Absence() {
    }

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }

    public ModuleCours getModule() {
        return module;
    }

    public void setModule(ModuleCours module) {
        this.module = module;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getSessionTime() {
        return sessionTime;
    }

    public void setSessionTime(String sessionTime) {
        this.sessionTime = sessionTime;
    }

    public Boolean getJustified() {
        return justified;
    }

    public void setJustified(Boolean justified) {
        this.justified = justified;
    }

    public String getMotif() {
        return motif;
    }

    public void setMotif(String motif) {
        this.motif = motif;
    }
}