package ma.ump.deptinfo.backend.entities;

import jakarta.persistence.*;
import java.time.LocalDate;

/**
 * Année universitaire (ex. "2026-2027").
 * Table academic_years du schéma V1. Une seule année est marquée "en cours".
 */
@Entity
@Table(name = "academic_years")
public class AcademicYear extends BaseEntity {

    @Column(nullable = false, unique = true, length = 9)
    private String label;           // ex. 2026-2027

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "is_current", nullable = false)
    private boolean current = false;

    public AcademicYear() {
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public boolean isCurrent() {
        return current;
    }

    public void setCurrent(boolean current) {
        this.current = current;
    }
}
