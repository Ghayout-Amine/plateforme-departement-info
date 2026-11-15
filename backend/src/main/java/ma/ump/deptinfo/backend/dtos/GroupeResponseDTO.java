package ma.ump.deptinfo.backend.dtos;

import java.time.LocalDateTime;

/**
 * Réponse renvoyée par /api/groupes (un groupe, avec le code et le nom de sa filière
 * pour que l'interface n'ait pas besoin d'un second appel).
 */
public class GroupeResponseDTO {

    private Long id;
    private String name;
    private Integer capacity;
    private Long filiereId;
    private String filiereCode;
    private String filiereNom;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public GroupeResponseDTO() {
    }

    public GroupeResponseDTO(Long id, String name, Integer capacity,
                             Long filiereId, String filiereCode, String filiereNom,
                             LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.capacity = capacity;
        this.filiereId = filiereId;
        this.filiereCode = filiereCode;
        this.filiereNom = filiereNom;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public Long getFiliereId() {
        return filiereId;
    }

    public String getFiliereCode() {
        return filiereCode;
    }

    public String getFiliereNom() {
        return filiereNom;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
