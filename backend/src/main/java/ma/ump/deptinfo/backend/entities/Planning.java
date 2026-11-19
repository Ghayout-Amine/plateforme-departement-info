package ma.ump.deptinfo.backend.entities;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Séance de l'emploi du temps (PGDI2026-75, US36) : table plannings.
 * Une séance réunit un enseignant, un module, un groupe et une salle,
 * à une date et sur un créneau horaire.
 * Simplification par rapport à la conception v2 : l'affectation
 * (TeachingAssignment) est portée directement par la séance.
 */
@Entity
@Table(name = "plannings",
        indexes = {
                @Index(name = "idx_planning_date", columnList = "session_date"),
                @Index(name = "idx_planning_salle_date", columnList = "salle_id, session_date")
        })
public class Planning extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "session_type", nullable = false, length = 10)
    private SessionType sessionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PlanningStatus status = PlanningStatus.PLANNED;

    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(length = 255)
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "module_id", nullable = false)
    private ModuleCours module;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "groupe_id", nullable = false)
    private Groupe groupe;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "salle_id", nullable = false)
    private Salle salle;

    public Planning() {
    }

    /**
     * Vrai si les deux créneaux se chevauchent le même jour.
     * Des créneaux bout à bout (10h00-12h00 puis 12h00-14h00) ne se chevauchent pas.
     */
    public boolean overlaps(LocalDate date, LocalTime start, LocalTime end) {
        return sessionDate.equals(date) && startTime.isBefore(end) && endTime.isAfter(start);
    }

    public void cancel() {
        this.status = PlanningStatus.CANCELLED;
    }

    public SessionType getSessionType() {
        return sessionType;
    }

    public void setSessionType(SessionType sessionType) {
        this.sessionType = sessionType;
    }

    public PlanningStatus getStatus() {
        return status;
    }

    public void setStatus(PlanningStatus status) {
        this.status = status;
    }

    public LocalDate getSessionDate() {
        return sessionDate;
    }

    public void setSessionDate(LocalDate sessionDate) {
        this.sessionDate = sessionDate;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Teacher getTeacher() {
        return teacher;
    }

    public void setTeacher(Teacher teacher) {
        this.teacher = teacher;
    }

    public ModuleCours getModule() {
        return module;
    }

    public void setModule(ModuleCours module) {
        this.module = module;
    }

    public Groupe getGroupe() {
        return groupe;
    }

    public void setGroupe(Groupe groupe) {
        this.groupe = groupe;
    }

    public Salle getSalle() {
        return salle;
    }

    public void setSalle(Salle salle) {
        this.salle = salle;
    }
}
