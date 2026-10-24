package ma.ump.deptinfo.backend.services;

import ma.ump.deptinfo.backend.dtos.ModuleCoursRequestDTO;
import ma.ump.deptinfo.backend.dtos.ModuleCoursResponseDTO;
import ma.ump.deptinfo.backend.entities.Filiere;
import ma.ump.deptinfo.backend.entities.ModuleCours;
import ma.ump.deptinfo.backend.entities.User;
import ma.ump.deptinfo.backend.repositories.FiliereRepository;
import ma.ump.deptinfo.backend.repositories.ModuleCoursRepository;
import ma.ump.deptinfo.backend.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ModuleCoursService {

    private final ModuleCoursRepository moduleCoursRepository;
    private final FiliereRepository filiereRepository;
    private final UserRepository userRepository;

    public ModuleCoursService(ModuleCoursRepository moduleCoursRepository,
                              FiliereRepository filiereRepository,
                              UserRepository userRepository) {
        this.moduleCoursRepository = moduleCoursRepository;
        this.filiereRepository = filiereRepository;
        this.userRepository = userRepository;
    }

    // Récupérer tous les modules
    public List<ModuleCoursResponseDTO> getAllModules() {
        return moduleCoursRepository.findAll().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    // Récupérer les modules d'une filière spécifique
    public List<ModuleCoursResponseDTO> getModulesByFiliereId(Long filiereId) {
        return moduleCoursRepository.findByFiliereId(filiereId).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    // Récupérer un module par son identifiant
    public ModuleCoursResponseDTO getModuleById(Long id) {
        ModuleCours module = moduleCoursRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Module introuvable avec l'id : " + id));
        return toResponseDTO(module);
    }

    // Créer un nouveau module
    public ModuleCoursResponseDTO createModule(ModuleCoursRequestDTO dto) {
        if (moduleCoursRepository.findByCode(dto.getCode()).isPresent()) {
            throw new RuntimeException("Un module avec ce code existe déjà : " + dto.getCode());
        }

        Filiere filiere = filiereRepository.findById(dto.getFiliereId())
                .orElseThrow(() -> new RuntimeException("Filière introuvable avec l'id : " + dto.getFiliereId()));

        ModuleCours module = new ModuleCours();
        module.setCode(dto.getCode());
        module.setNom(dto.getNom());
        module.setSemestre(dto.getSemestre());
        module.setVolumeHoraire(dto.getVolumeHoraire());
        module.setFiliere(filiere);

        if (dto.getEnseignantId() != null) {
            User enseignant = userRepository.findById(dto.getEnseignantId())
                    .orElseThrow(() -> new RuntimeException("Enseignant introuvable avec l'id : " + dto.getEnseignantId()));
            module.setEnseignant(enseignant);
        }

        ModuleCours saved = moduleCoursRepository.save(module);
        return toResponseDTO(saved);
    }

    // Mettre à jour un module existant
    public ModuleCoursResponseDTO updateModule(Long id, ModuleCoursRequestDTO dto) {
        ModuleCours module = moduleCoursRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Module introuvable avec l'id : " + id));

        moduleCoursRepository.findByCode(dto.getCode()).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new RuntimeException("Un autre module utilise déjà le code : " + dto.getCode());
            }
        });

        Filiere filiere = filiereRepository.findById(dto.getFiliereId())
                .orElseThrow(() -> new RuntimeException("Filière introuvable avec l'id : " + dto.getFiliereId()));

        module.setCode(dto.getCode());
        module.setNom(dto.getNom());
        module.setSemestre(dto.getSemestre());
        module.setVolumeHoraire(dto.getVolumeHoraire());
        module.setFiliere(filiere);

        if (dto.getEnseignantId() != null) {
            User enseignant = userRepository.findById(dto.getEnseignantId())
                    .orElseThrow(() -> new RuntimeException("Enseignant introuvable avec l'id : " + dto.getEnseignantId()));
            module.setEnseignant(enseignant);
        } else {
            module.setEnseignant(null);
        }

        ModuleCours updated = moduleCoursRepository.save(module);
        return toResponseDTO(updated);
    }

    // Supprimer un module
    public void deleteModule(Long id) {
        if (!moduleCoursRepository.existsById(id)) {
            throw new RuntimeException("Module introuvable avec l'id : " + id);
        }
        moduleCoursRepository.deleteById(id);
    }

    // Méthode de conversion entité -> DTO de réponse
    private ModuleCoursResponseDTO toResponseDTO(ModuleCours module) {
        Long ensId = null;
        String ensNom = null;

        if (module.getEnseignant() != null) {
            ensId = module.getEnseignant().getId();
            ensNom = module.getEnseignant().getFirstName() + " " + module.getEnseignant().getLastName();
        }

        return new ModuleCoursResponseDTO(
                module.getId(),
                module.getCode(),
                module.getNom(),
                module.getSemestre(),
                module.getVolumeHoraire(),
                module.getFiliere().getId(),
                module.getFiliere().getCode(),
                module.getFiliere().getNom(),
                ensId,
                ensNom,
                module.getCreatedAt()
        );
    }
}