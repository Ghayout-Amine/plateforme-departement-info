package ma.ump.deptinfo.backend.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModuleCoursRequestDTO {

    @NotBlank(message = "Le code du module est obligatoire")
    private String code;

    @NotBlank(message = "Le nom du module est obligatoire")
    private String nom;

    @NotBlank(message = "Le semestre est obligatoire (ex: S1, S2, S3)")
    private String semestre;

    private Integer volumeHoraire;

    @NotNull(message = "L'identifiant de la filière est obligatoire")
    private Long filiereId;

    private Long enseignantId;
}