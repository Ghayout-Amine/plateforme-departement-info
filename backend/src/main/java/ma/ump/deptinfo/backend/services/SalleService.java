package ma.ump.deptinfo.backend.services;

import ma.ump.deptinfo.backend.dtos.SalleRequestDTO;
import ma.ump.deptinfo.backend.dtos.SalleResponseDTO;
import ma.ump.deptinfo.backend.entities.RoomType;
import ma.ump.deptinfo.backend.entities.Salle;
import ma.ump.deptinfo.backend.exceptions.ConflictException;
import ma.ump.deptinfo.backend.exceptions.ResourceNotFoundException;
import ma.ump.deptinfo.backend.repositories.SalleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/**
 * Règles métier des salles (PGDI2026-44, US21).
 * - le code est unique (comparaison sans tenir compte des majuscules) et stocké en majuscules ;
 * - une salle créée sans "isActive" est active par défaut.
 */
@Service
@Transactional
public class SalleService {

    private final SalleRepository salleRepository;

    public SalleService(SalleRepository salleRepository) {
        this.salleRepository = salleRepository;
    }

    // Lister les salles, avec filtres facultatifs par type et par état (active / hors service)
    @Transactional(readOnly = true)
    public List<SalleResponseDTO> getAllSalles(RoomType roomType, Boolean active) {
        return salleRepository.findAllByOrderByCodeAsc().stream()
                .filter(salle -> roomType == null || salle.getRoomType() == roomType)
                .filter(salle -> active == null || salle.isActive() == active)
                .map(this::toResponseDTO)
                .toList();
    }

    // Récupérer une salle par son identifiant
    @Transactional(readOnly = true)
    public SalleResponseDTO getSalleById(Long id) {
        return toResponseDTO(findSalle(id));
    }

    // Créer une salle
    public SalleResponseDTO createSalle(SalleRequestDTO dto) {
        String code = normalizeCode(dto.getCode());

        if (salleRepository.findByCodeIgnoreCase(code).isPresent()) {
            throw new ConflictException("Une salle avec ce code existe déjà : " + code);
        }

        Salle salle = new Salle();
        applyDto(salle, dto, code);
        salle.setActive(dto.getIsActive() == null || dto.getIsActive());

        return toResponseDTO(salleRepository.save(salle));
    }

    // Mettre à jour une salle
    public SalleResponseDTO updateSalle(Long id, SalleRequestDTO dto) {
        Salle salle = findSalle(id);
        String code = normalizeCode(dto.getCode());

        salleRepository.findByCodeIgnoreCase(code).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new ConflictException("Une autre salle utilise déjà le code : " + code);
            }
        });

        applyDto(salle, dto, code);
        if (dto.getIsActive() != null) {
            salle.setActive(dto.getIsActive());
        }

        return toResponseDTO(salleRepository.save(salle));
    }

    // Supprimer une salle
    public void deleteSalle(Long id) {
        if (!salleRepository.existsById(id)) {
            throw new ResourceNotFoundException("Salle introuvable avec l'id : " + id);
        }
        salleRepository.deleteById(id);
    }

    private Salle findSalle(Long id) {
        return salleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Salle introuvable avec l'id : " + id));
    }

    private void applyDto(Salle salle, SalleRequestDTO dto, String code) {
        salle.setCode(code);
        salle.setName(dto.getName().trim());
        salle.setBuilding(dto.getBuilding() == null || dto.getBuilding().isBlank()
                ? null : dto.getBuilding().trim());
        salle.setCapacity(dto.getCapacity());
        salle.setRoomType(dto.getRoomType());
    }

    // "amphi_a " -> "AMPHI_A"
    private String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }

    // Conversion entité -> DTO de réponse
    private SalleResponseDTO toResponseDTO(Salle salle) {
        return new SalleResponseDTO(
                salle.getId(),
                salle.getCode(),
                salle.getName(),
                salle.getBuilding(),
                salle.getCapacity(),
                salle.getRoomType(),
                salle.isActive(),
                salle.getCreatedAt(),
                salle.getUpdatedAt()
        );
    }
}
