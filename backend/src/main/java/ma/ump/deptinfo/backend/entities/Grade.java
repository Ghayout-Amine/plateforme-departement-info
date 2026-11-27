package ma.ump.deptinfo.backend.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "grades")
public class Grade extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "module_id", nullable = false)
    private ModuleCours module;

    @NotNull
    @DecimalMin("0.0")
    @DecimalMax("20.0")
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal score;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private GradeType type;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal coefficient = BigDecimal.ONE;

    @NotNull
    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GradeStatus status = GradeStatus.DRAFT;

    public Grade() {
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

    public BigDecimal getScore() {
        return score;
    }

    public void setScore(BigDecimal score) {
        this.score = score;
    }

    public GradeType getType() {
        return type;
    }

    public void setType(GradeType type) {
        this.type = type;
    }

    public BigDecimal getCoefficient() {
        return coefficient;
    }

    public void setCoefficient(BigDecimal coefficient) {
        this.coefficient = coefficient;
    }

    public LocalDate getSessionDate() {
        return sessionDate;
    }

    public void setSessionDate(LocalDate sessionDate) {
        this.sessionDate = sessionDate;
    }

    public GradeStatus getStatus() {
        return status;
    }

    public void setStatus(GradeStatus status) {
        this.status = status;
    }
}
