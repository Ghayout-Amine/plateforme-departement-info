package ma.ump.deptinfo.backend.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

/** Clé composite (stage, enseignant) de la table internship_supervisors. */
@Embeddable
public class InternshipSupervisorId implements Serializable {

    @Column(name = "internship_id")
    private Long internshipId;

    @Column(name = "teacher_id")
    private Long teacherId;

    public InternshipSupervisorId() {
    }

    public InternshipSupervisorId(Long internshipId, Long teacherId) {
        this.internshipId = internshipId;
        this.teacherId = teacherId;
    }

    public Long getInternshipId() { return internshipId; }
    public Long getTeacherId() { return teacherId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InternshipSupervisorId other)) return false;
        return Objects.equals(internshipId, other.internshipId) && Objects.equals(teacherId, other.teacherId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(internshipId, teacherId);
    }
}
