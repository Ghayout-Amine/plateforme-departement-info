package ma.ump.deptinfo.backend.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Décision du département sur une convention (PGDI2026-80). */
public class InternshipValidationDTO {

    @NotNull(message = "La décision (approved) est obligatoire")
    private Boolean approved;

    @Size(max = 500, message = "Le commentaire ne doit pas dépasser 500 caractères")
    private String comment;

    public Boolean getApproved() { return approved; }
    public void setApproved(Boolean approved) { this.approved = approved; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
