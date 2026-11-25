package ma.ump.deptinfo.backend.dtos;

import jakarta.validation.constraints.NotNull;

/** Corps de PUT /api/internships/{id}/supervisor : l'enseignant choisi comme encadrant académique. */
public class SupervisorAssignmentDTO {

    @NotNull(message = "L'enseignant encadrant est obligatoire")
    private Long teacherId;

    public Long getTeacherId() { return teacherId; }
    public void setTeacherId(Long teacherId) { this.teacherId = teacherId; }
}
