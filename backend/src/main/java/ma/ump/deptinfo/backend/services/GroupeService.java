package ma.ump.deptinfo.backend.services;

import ma.ump.deptinfo.backend.dtos.GroupeRequestDTO;
import ma.ump.deptinfo.backend.dtos.GroupeResponseDTO;
import ma.ump.deptinfo.backend.entities.Filiere;
import ma.ump.deptinfo.backend.entities.Groupe;
import ma.ump.deptinfo.backend.exceptions.ConflictException;
import ma.ump.deptinfo.backend.exceptions.ResourceNotFoundException;
import ma.ump.deptinfo.backend.repositories.FiliereRepository;
import ma.ump.deptinfo.backend.repositories.GroupeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Règles métier des groupes (PGDI2026-44, US22).
 * - un groupe appartient obligatoirement à une filière existante ;
 * - deux groupes d'une même filière ne peuvent pas avoir le même nom.
 */
@Service
@Transactional
public class GroupeService {

    private final GroupeRepository groupeRepository;
    private final FiliereRepository filiereRepository;

    public GroupeService(GroupeRepository groupeRepository, FiliereRepository filiereRepository) {
        this.groupeRepository = groupeRepository;
        this.filiereRepository = filiereRepository;
    }

    // Lister tous les groupes, ou ceux d'une filière si filiereId est fourni
    @Transactional(readOnly = true)
    public List<GroupeResponseDTO> getAllGroupes(Long filiereId) {
        List<Groupe> groupes;
        if (filiereId == null) {
            groupes = groupeRepository.findAllByOrderByNameAsc();
        } else {
            findFiliere(filiereId); // 404 si la filière n'existe pas
            groupes = groupeRepository.findByFiliereIdOrderByNameAsc(filiereId);
        }
        return groupes.stream().map(this::toResponseDTO).toList();
    }

    // Récupérer un groupe par son identifiant
    @Transactional(readOnly = true)
    public GroupeResponseDTO getGroupeById(Long id) {
        return toResponseDTO(findGroupe(id));
    }

    // Créer un groupe dans une filière
    public GroupeResponseDTO createGroupe(GroupeRequestDTO dto) {
        Filiere filiere = findFiliere(dto.getFiliereId());
        String name = dto.getName().trim();

        if (groupeRepository.findByFiliereIdAndNameIgnoreCase(filiere.getId(), name).isPresent()) {
            throw new ConflictException(
                    "Le groupe \"" + name + "\" existe déjà dans la filière " + filiere.getCode());
        }

        Groupe groupe = new Groupe();
        groupe.setName(name);
        groupe.setCapacity(dto.getCapacity());
        groupe.setFiliere(filiere);

        return toResponseDTO(groupeRepository.save(groupe));
    }

    // Mettre à jour un groupe (y compris le changer de filière)
    public GroupeResponseDTO updateGroupe(Long id, GroupeRequestDTO dto) {
        Groupe groupe = findGroupe(id);
        Filiere filiere = findFiliere(dto.getFiliereId());
        String name = dto.getName().trim();

        groupeRepository.findByFiliereIdAndNameIgnoreCase(filiere.getId(), name).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new ConflictException(
                        "Le groupe \"" + name + "\" existe déjà dans la filière " + filiere.getCode());
            }
        });

        groupe.setName(name);
        groupe.setCapacity(dto.getCapacity());
        groupe.setFiliere(filiere);

        return toResponseDTO(groupeRepository.save(groupe));
    }

    // Supprimer un groupe
    public void deleteGroupe(Long id) {
        if (!groupeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Groupe introuvable avec l'id : " + id);
        }
        groupeRepository.deleteById(id);
    }

    private Groupe findGroupe(Long id) {
        return groupeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Groupe introuvable avec l'id : " + id));
    }

    private Filiere findFiliere(Long filiereId) {
        return filiereRepository.findById(filiereId)
                .orElseThrow(() -> new ResourceNotFoundException("Filière introuvable avec l'id : " + filiereId));
    }

    // Conversion entité -> DTO de réponse (avec le code et le nom de la filière)
    private GroupeResponseDTO toResponseDTO(Groupe groupe) {
        Filiere filiere = groupe.getFiliere();
        return new GroupeResponseDTO(
                groupe.getId(),
                groupe.getName(),
                groupe.getCapacity(),
                filiere.getId(),
                filiere.getCode(),
                filiere.getNom(),
                groupe.getCreatedAt(),
                groupe.getUpdatedAt()
        );
    }
}
