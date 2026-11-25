package ma.ump.deptinfo.backend.entities;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Encadrement académique d'un stage/PFE (PGDI2026-87, US51).
 * Table internship_supervisors : association stage - enseignant porteuse du rôle.
 */
@Entity
@Table(name = "internship_supervisors")
public class InternshipSupervisor {

    @EmbeddedId
    private InternshipSupervisorId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("internshipId")
    @JoinColumn(name = "internship_id")
    private Internship internship;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("teacherId")
    @JoinColumn(name = "teacher_id")
    private Teacher teacher;

    @Enumerated(EnumType.STRING)
    @Column(name = "supervisor_role", nullable = false, length = 20)
    private SupervisorRole supervisorRole = SupervisorRole.MAIN;

    @Column(name = "assigned_at", nullable = false)
    private LocalDateTime assignedAt = LocalDateTime.now();

    public InternshipSupervisor() {
    }

    public InternshipSupervisor(Internship internship, Teacher teacher, SupervisorRole role) {
        this.id = new InternshipSupervisorId(internship.getId(), teacher.getId());
        this.internship = internship;
        this.teacher = teacher;
        this.supervisorRole = role;
    }

    public InternshipSupervisorId getId() { return id; }
    public Internship getInternship() { return internship; }
    public Teacher getTeacher() { return teacher; }
    public SupervisorRole getSupervisorRole() { return supervisorRole; }
    public LocalDateTime getAssignedAt() { return assignedAt; }
}
