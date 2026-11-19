package ma.ump.deptinfo.backend.dtos;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ma.ump.deptinfo.backend.entities.SessionType;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Données envoyées pour créer ou modifier une séance (PGDI2026-75, US36).
 * Les heures acceptent le format "08:30" ou "08:30:00".
 */
public class PlanningRequestDTO {

    @NotNull(message = "L'enseignant est obligatoire")
    private Long teacherId;

    @NotNull(message = "Le module est obligatoire")
    private Long moduleId;

    @NotNull(message = "Le groupe est obligatoire")
    private Long groupeId;

    @NotNull(message = "La salle est obligatoire")
    private Long salleId;

    @NotNull(message = "Le type de séance est obligatoire (CM, TD ou TP)")
    private SessionType sessionType;

    @NotNull(message = "La date est obligatoire")
    private LocalDate sessionDate;

    @NotNull(message = "L'heure de début est obligatoire")
    private LocalTime startTime;

    @NotNull(message = "L'heure de fin est obligatoire")
    private LocalTime endTime;

    @Size(max = 255, message = "Les remarques ne dépassent pas 255 caractères")
    private String notes;

    @Min(value = 1, message = "Au moins 1 semaine")
    @Max(value = 16, message = "Au plus 16 semaines (un semestre)")
    private Integer weeks; // POST /api/plannings/serie : nombre de semaines (1 par défaut)

    public PlanningRequestDTO() {
    }

    public Long getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(Long teacherId) {
        this.teacherId = teacherId;
    }

    public Long getModuleId() {
        return moduleId;
    }

    public void setModuleId(Long moduleId) {
        this.moduleId = moduleId;
    }

    public Long getGroupeId() {
        return groupeId;
    }

    public void setGroupeId(Long groupeId) {
        this.groupeId = groupeId;
    }

    public Long getSalleId() {
        return salleId;
    }

    public void setSalleId(Long salleId) {
        this.salleId = salleId;
    }

    public SessionType getSessionType() {
        return sessionType;
    }

    public void setSessionType(SessionType sessionType) {
        this.sessionType = sessionType;
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

    public Integer getWeeks() {
        return weeks;
    }

    public void setWeeks(Integer weeks) {
        this.weeks = weeks;
    }
}
