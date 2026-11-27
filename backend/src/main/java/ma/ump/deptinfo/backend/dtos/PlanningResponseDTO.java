package ma.ump.deptinfo.backend.dtos;

import ma.ump.deptinfo.backend.entities.PlanningStatus;
import ma.ump.deptinfo.backend.entities.SessionType;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Séance renvoyée par l'API (PGDI2026-75, US36).
 * Contient les libellés utiles à l'affichage pour éviter des appels supplémentaires.
 */
public class PlanningResponseDTO {

    private Long id;

    private SessionType sessionType;

    private PlanningStatus status;

    private LocalDate sessionDate;

    private String dayOfWeek; // MONDAY ... SUNDAY

    private LocalTime startTime;

    private LocalTime endTime;

    private String notes;

    private Long teacherId;

    private String teacherName;

    private Long moduleId;

    private String moduleCode;

    private String moduleNom;

    private Long groupeId;

    private String groupeName;

    private String filiereCode;

    private Long salleId;

    private String salleCode;

    private String salleName;

    public PlanningResponseDTO() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(String dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
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

    public Long getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(Long teacherId) {
        this.teacherId = teacherId;
    }

    public String getTeacherName() {
        return teacherName;
    }

    public void setTeacherName(String teacherName) {
        this.teacherName = teacherName;
    }

    public Long getModuleId() {
        return moduleId;
    }

    public void setModuleId(Long moduleId) {
        this.moduleId = moduleId;
    }

    public String getModuleCode() {
        return moduleCode;
    }

    public void setModuleCode(String moduleCode) {
        this.moduleCode = moduleCode;
    }

    public String getModuleNom() {
        return moduleNom;
    }

    public void setModuleNom(String moduleNom) {
        this.moduleNom = moduleNom;
    }

    public Long getGroupeId() {
        return groupeId;
    }

    public void setGroupeId(Long groupeId) {
        this.groupeId = groupeId;
    }

    public String getGroupeName() {
        return groupeName;
    }

    public void setGroupeName(String groupeName) {
        this.groupeName = groupeName;
    }

    public String getFiliereCode() {
        return filiereCode;
    }

    public void setFiliereCode(String filiereCode) {
        this.filiereCode = filiereCode;
    }

    public Long getSalleId() {
        return salleId;
    }

    public void setSalleId(Long salleId) {
        this.salleId = salleId;
    }

    public String getSalleCode() {
        return salleCode;
    }

    public void setSalleCode(String salleCode) {
        this.salleCode = salleCode;
    }

    public String getSalleName() {
        return salleName;
    }

    public void setSalleName(String salleName) {
        this.salleName = salleName;
    }
}
