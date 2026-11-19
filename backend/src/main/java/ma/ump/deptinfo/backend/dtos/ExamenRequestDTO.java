package ma.ump.deptinfo.backend.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ma.ump.deptinfo.backend.entities.ExamType;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Données envoyées pour planifier ou modifier un examen (PGDI2026-75, US49).
 * academicYearId est facultatif : l'année en cours est utilisée par défaut.
 */
public class ExamenRequestDTO {

    @NotBlank(message = "Le titre est obligatoire")
    @Size(max = 150, message = "Le titre ne dépasse pas 150 caractères")
    private String title;

    @NotNull(message = "Le type d'examen est obligatoire (CC, TP, FINAL, RATTRAPAGE)")
    private ExamType examType;

    @NotNull(message = "Le module est obligatoire")
    private Long moduleId;

    @NotNull(message = "Le groupe est obligatoire")
    private Long groupeId;

    private Long academicYearId; // facultatif

    @NotEmpty(message = "Au moins une salle est obligatoire")
    private List<Long> salleIds;

    @NotNull(message = "La date est obligatoire")
    private LocalDate examDate;

    @NotNull(message = "L'heure de début est obligatoire")
    private LocalTime startTime;

    @NotNull(message = "L'heure de fin est obligatoire")
    private LocalTime endTime;

    @Size(max = 255, message = "Les remarques ne dépassent pas 255 caractères")
    private String notes;

    public ExamenRequestDTO() {
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

    public Long getAcademicYearId() {
        return academicYearId;
    }

    public void setAcademicYearId(Long academicYearId) {
        this.academicYearId = academicYearId;
    }

    public List<Long> getSalleIds() {
        return salleIds;
    }

    public void setSalleIds(List<Long> salleIds) {
        this.salleIds = salleIds;
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
}
