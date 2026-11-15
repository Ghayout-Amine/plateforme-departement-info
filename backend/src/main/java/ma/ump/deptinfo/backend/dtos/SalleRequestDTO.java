package ma.ump.deptinfo.backend.dtos;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ma.ump.deptinfo.backend.entities.RoomType;

/**
 * Corps des requêtes POST /api/salles et PUT /api/salles/{id}.
 * Exemple JSON :
 * { "code": "AMPHI_A", "name": "Amphithéâtre Al-Khawarizmi", "building": "Bloc Informatique",
 *   "capacity": 150, "roomType": "AMPHI", "isActive": true }
 */
public class SalleRequestDTO {

    @NotBlank(message = "Le code de la salle est obligatoire")
    @Size(max = 20, message = "Le code ne doit pas dépasser 20 caractères")
    private String code;

    @NotBlank(message = "Le nom de la salle est obligatoire")
    @Size(max = 100, message = "Le nom ne doit pas dépasser 100 caractères")
    private String name;

    @Size(max = 100, message = "Le bâtiment ne doit pas dépasser 100 caractères")
    private String building;

    @NotNull(message = "La capacité est obligatoire")
    @Min(value = 1, message = "La capacité doit être au moins de 1 place")
    @Max(value = 2000, message = "La capacité ne doit pas dépasser 2000 places")
    private Integer capacity;

    @NotNull(message = "Le type de salle est obligatoire (AMPHI, CLASSROOM, LAB, OTHER)")
    private RoomType roomType;

    /** Facultatif : true par défaut à la création. */
    private Boolean isActive;

    public SalleRequestDTO() {
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBuilding() {
        return building;
    }

    public void setBuilding(String building) {
        this.building = building;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public RoomType getRoomType() {
        return roomType;
    }

    public void setRoomType(RoomType roomType) {
        this.roomType = roomType;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}
