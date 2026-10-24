package ma.ump.deptinfo.backend.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FiliereRequestDTO {

    @NotBlank(message = "Le code de la filière est obligatoire")
    private String code;

    @NotBlank(message = "Le nom de la filière est obligatoire")
    private String nom;

    private String description;

    @NotBlank(message = "Le niveau est obligatoire (ex: LICENCE, MASTER)")
    private String niveau;

    private Long responsableId;
}