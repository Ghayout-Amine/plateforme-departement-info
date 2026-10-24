package ma.ump.deptinfo.backend.services;

import ma.ump.deptinfo.backend.dtos.FiliereRequestDTO;
import ma.ump.deptinfo.backend.dtos.FiliereResponseDTO;
import ma.ump.deptinfo.backend.entities.Filiere;
import ma.ump.deptinfo.backend.entities.User;
import ma.ump.deptinfo.backend.repositories.FiliereRepository;
import ma.ump.deptinfo.backend.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class FiliereService {

    private final FiliereRepository filiereRepository;
    private final UserRepository userRepository;

    public FiliereService(FiliereRepository filiereRepository, UserRepository userRepository) {
        this.filiereRepository = filiereRepository;
        this.userRepository = userRepository;
    }

    // Récupérer toutes les filières
    public List<FiliereResponseDTO> getAllFilieres() {
        return filiereRepository.findAll().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    // Récupérer une filière par son identifiant
    public FiliereResponseDTO getFiliereById(Long id) {
        Filiere filiere = filiereRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Filière introuvable avec l'id : " + id));
        return toResponseDTO(filiere);
    }

    // Créer une nouvelle filière
    public FiliereResponseDTO createFiliere(FiliereRequestDTO dto) {
        if (filiereRepository.findByCode(dto.getCode()).isPresent()) {
            throw new RuntimeException("Une filière avec ce code existe déjà : " + dto.getCode());
        }

        Filiere filiere = new Filiere();
        filiere.setCode(dto.getCode());
        filiere.setNom(dto.getNom());
        filiere.setDescription(dto.getDescription());
        filiere.setNiveau(dto.getNiveau());

        if (dto.getResponsableId() != null) {
            User responsable = userRepository.findById(dto.getResponsableId())
                    .orElseThrow(() -> new RuntimeException("Responsable introuvable avec l'id : " + dto.getResponsableId()));
            filiere.setResponsable(responsable);
        }

        Filiere saved = filiereRepository.save(filiere);
        return toResponseDTO(saved);
    }

    // Mettre à jour une filière existante
    public FiliereResponseDTO updateFiliere(Long id, FiliereRequestDTO dto) {
        Filiere filiere = filiereRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Filière introuvable avec l'id : " + id));

        filiereRepository.findByCode(dto.getCode()).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new RuntimeException("Une autre filière utilise déjà le code : " + dto.getCode());
            }
        });

        filiere.setCode(dto.getCode());
        filiere.setNom(dto.getNom());
        filiere.setDescription(dto.getDescription());
        filiere.setNiveau(dto.getNiveau());

        if (dto.getResponsableId() != null) {
            User responsable = userRepository.findById(dto.getResponsableId())
                    .orElseThrow(() -> new RuntimeException("Responsable introuvable avec l'id : " + dto.getResponsableId()));
            filiere.setResponsable(responsable);
        } else {
            filiere.setResponsable(null);
        }

        Filiere updated = filiereRepository.save(filiere);
        return toResponseDTO(updated);
    }

    // Supprimer une filière
    public void deleteFiliere(Long id) {
        if (!filiereRepository.existsById(id)) {
            throw new RuntimeException("Filière introuvable avec l'id : " + id);
        }
        filiereRepository.deleteById(id);
    }

    // Méthode de conversion entité -> DTO de réponse
    private FiliereResponseDTO toResponseDTO(Filiere filiere) {
        Long respId = null;
        String respNom = null;

        if (filiere.getResponsable() != null) {
            respId = filiere.getResponsable().getId();
            respNom = filiere.getResponsable().getFirstName() + " " + filiere.getResponsable().getLastName();
        }

        return new FiliereResponseDTO(
                filiere.getId(),
                filiere.getCode(),
                filiere.getNom(),
                filiere.getDescription(),
                filiere.getNiveau(),
                respId,
                respNom,
                filiere.getCreatedAt()
        );
    }
}