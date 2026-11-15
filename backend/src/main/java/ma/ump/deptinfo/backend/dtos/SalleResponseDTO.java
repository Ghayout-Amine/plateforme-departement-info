package ma.ump.deptinfo.backend.dtos;

import ma.ump.deptinfo.backend.entities.RoomType;

import java.time.LocalDateTime;

/**
 * Réponse renvoyée par /api/salles (une salle).
 */
public class SalleResponseDTO {

    private Long id;
    private String code;
    private String name;
    private String building;
    private Integer capacity;
    private RoomType roomType;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public SalleResponseDTO() {
    }

    public SalleResponseDTO(Long id, String code, String name, String building, Integer capacity,
                            RoomType roomType, Boolean isActive,
                            LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.building = building;
        this.capacity = capacity;
        this.roomType = roomType;
        this.isActive = isActive;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getBuilding() {
        return building;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public RoomType getRoomType() {
        return roomType;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
