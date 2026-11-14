package ma.ump.deptinfo.backend.dtos;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Corps des requêtes POST /api/groupes et PUT /api/groupes/{id}.
 * Exemple JSON : { "name": "Groupe 1 - GL", "capacity": 30, "filiereId": 1 }
 */
public class GroupeRequestDTO {

    @NotBlank(message = "Le nom du groupe est obligatoire")
    @Size(max = 50, message = "Le nom du groupe ne doit pas dépasser 50 caractères")
    private String name;

    @Min(value = 1, message = "L'effectif maximal doit être au moins de 1")
    @Max(value = 500, message = "L'effectif maximal ne doit pas dépasser 500")
    private Integer capacity;

    @NotNull(message = "La filière est obligatoire")
    private Long filiereId;

    public GroupeRequestDTO() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public Long getFiliereId() {
        return filiereId;
    }

    public void setFiliereId(Long filiereId) {
        this.filiereId = filiereId;
    }
}
