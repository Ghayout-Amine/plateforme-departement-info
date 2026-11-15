package ma.ump.deptinfo.backend.entities;

import jakarta.persistence.*;

/**
 * Salle du département (amphi, salle de cours, salle de TP...).
 * UML v02 : CL_2 Structure pédagogique — table "salles".
 * Les colonnes id, created_at et updated_at viennent de BaseEntity.
 */
@Entity
@Table(name = "salles")
public class Salle extends BaseEntity {

    @Column(nullable = false, unique = true, length = 20)
    private String code;            // ex. AMPHI_A

    @Column(nullable = false, length = 100)
    private String name;            // ex. Amphithéâtre Al-Khawarizmi

    @Column(length = 100)
    private String building;        // ex. Bloc Informatique

    @Column(nullable = false)
    private Integer capacity;       // nombre de places (> 0)

    @Enumerated(EnumType.STRING)
    @Column(name = "room_type", nullable = false, length = 20)
    private RoomType roomType;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;  // false = salle hors service (travaux...)

    public Salle() {
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

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
