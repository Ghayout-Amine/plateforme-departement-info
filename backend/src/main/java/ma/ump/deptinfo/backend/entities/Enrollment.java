package ma.ump.deptinfo.backend.entities;

import jakarta.persistence.*;

/**
 * Inscription pédagogique annuelle d'un étudiant (PGDI2026-60, US26).
 * Un étudiant est inscrit dans une filière pour une année universitaire,
 * et affecté à un groupe de TD/TP de cette filière (facultatif au départ).
 * Table enrollments du schéma V1 : une seule inscription par étudiant et par année.
 */
@Entity
@Table(name = "enrollments",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_enrollment_student_year",
                columnNames = {"student_id", "academic_year_id"}))
public class Enrollment extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EnrollmentStatus status = EnrollmentStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "filiere_id", nullable = false)
    private Filiere filiere;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "academic_year_id", nullable = false)
    private AcademicYear academicYear;

    // NULL tant que l'étudiant n'est pas affecté à un groupe
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "groupe_id")
    private Groupe groupe;

    public Enrollment() {
    }

    public EnrollmentStatus getStatus() {
        return status;
    }

    public void setStatus(EnrollmentStatus status) {
        this.status = status;
    }

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }

    public Filiere getFiliere() {
        return filiere;
    }

    public void setFiliere(Filiere filiere) {
        this.filiere = filiere;
    }

    public AcademicYear getAcademicYear() {
        return academicYear;
    }

    public void setAcademicYear(AcademicYear academicYear) {
        this.academicYear = academicYear;
    }

    public Groupe getGroupe() {
        return groupe;
    }

    public void setGroupe(Groupe groupe) {
        this.groupe = groupe;
    }
}
