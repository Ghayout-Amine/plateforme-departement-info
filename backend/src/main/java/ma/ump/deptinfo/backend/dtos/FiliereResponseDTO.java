package ma.ump.deptinfo.backend.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FiliereResponseDTO {

    private Long id;
    private String code;
    private String nom;
    private String description;
    private String niveau;
    private Long responsableId;
    private String responsableNomComplet;
    private LocalDateTime createdAt;
}