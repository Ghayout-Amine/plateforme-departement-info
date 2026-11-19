package ma.ump.deptinfo.backend.dtos;

import ma.ump.deptinfo.backend.entities.ExamType;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Examen renvoyé par l'API (PGDI2026-75, US49).
 */
public class ExamenResponseDTO {

    private Long id;

    private String title;

    private ExamType examType;

    private LocalDate examDate;

    private LocalTime startTime;

    private LocalTime endTime;

    private String notes;

    private Long moduleId;

    private String moduleCode;

    private String moduleNom;

    private Long groupeId;

    private String groupeName;

    private Integer groupeCapacity;

    private String filiereCode;

    private Long academicYearId;

    private String academicYearLabel;

    private List<Long> salleIds;

    private List<String> salleCodes;

    private int totalCapacity; // places dans toutes les salles

    public ExamenResponseDTO() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public ExamType getExamType() {
        return examType;
    }

    public void setExamType(ExamType examType) {
        this.examType = examType;
    }

    public LocalDate getExamDate() {
        return examDate;
    }

    public void setExamDate(LocalDate examDate) {
        this.examDate = examDate;
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

    public Integer getGroupeCapacity() {
        return groupeCapacity;
    }

    public void setGroupeCapacity(Integer groupeCapacity) {
        this.groupeCapacity = groupeCapacity;
    }

    public String getFiliereCode() {
        return filiereCode;
    }

    public void setFiliereCode(String filiereCode) {
        this.filiereCode = filiereCode;
    }

    public Long getAcademicYearId() {
        return academicYearId;
    }

    public void setAcademicYearId(Long academicYearId) {
        this.academicYearId = academicYearId;
    }

    public String getAcademicYearLabel() {
        return academicYearLabel;
    }

    public void setAcademicYearLabel(String academicYearLabel) {
        this.academicYearLabel = academicYearLabel;
    }

    public List<Long> getSalleIds() {
        return salleIds;
    }

    public void setSalleIds(List<Long> salleIds) {
        this.salleIds = salleIds;
    }

    public List<String> getSalleCodes() {
        return salleCodes;
    }

    public void setSalleCodes(List<String> salleCodes) {
        this.salleCodes = salleCodes;
    }

    public int getTotalCapacity() {
        return totalCapacity;
    }

    public void setTotalCapacity(int totalCapacity) {
        this.totalCapacity = totalCapacity;
    }
}
