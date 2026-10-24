package ma.ump.deptinfo.backend.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModuleCoursResponseDTO {

    private Long id;
    private String code;
    private String nom;
    private String semestre;
    private Integer volumeHoraire;
    private Long filiereId;
    private String filiereCode;
    private String filiereNom;
    private Long enseignantId;
    private String enseignantNomComplet;
    private LocalDateTime createdAt;
}