package ma.ump.deptinfo.backend.services;

import ma.ump.deptinfo.backend.dtos.GroupeRequestDTO;
import ma.ump.deptinfo.backend.dtos.GroupeResponseDTO;
import ma.ump.deptinfo.backend.entities.Filiere;
import ma.ump.deptinfo.backend.entities.Groupe;
import ma.ump.deptinfo.backend.exceptions.ConflictException;
import ma.ump.deptinfo.backend.exceptions.ResourceNotFoundException;
import ma.ump.deptinfo.backend.repositories.FiliereRepository;
import ma.ump.deptinfo.backend.repositories.GroupeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroupeServiceTest {

    @Mock
    private GroupeRepository groupeRepository;

    @Mock
    private FiliereRepository filiereRepository;

    @InjectMocks
    private GroupeService groupeService;

    private Filiere filiereGL;
    private GroupeRequestDTO validDTO;

    @BeforeEach
    void setUp() {
        filiereGL = new Filiere();
        filiereGL.setId(1L);
        filiereGL.setCode("M2I-ISI");
        filiereGL.setNom("Ingénierie des Systèmes d'Information");
        filiereGL.setNiveau("MASTER");

        validDTO = new GroupeRequestDTO();
        validDTO.setName(" Groupe 1 - GL ");
        validDTO.setCapacity(30);
        validDTO.setFiliereId(1L);
    }

    private Groupe groupe(Long id, String name) {
        Groupe groupe = new Groupe();
        ReflectionTestUtils.setField(groupe, "id", id);
        groupe.setName(name);
        groupe.setCapacity(30);
        groupe.setFiliere(filiereGL);
        return groupe;
    }

    @Test
    @DisplayName("createGroupe - Succès : nom nettoyé, filière renvoyée dans la réponse")
    void createGroupe_Success() {
        when(filiereRepository.findById(1L)).thenReturn(Optional.of(filiereGL));
        when(groupeRepository.findByFiliereIdAndNameIgnoreCase(1L, "Groupe 1 - GL")).thenReturn(Optional.empty());
        when(groupeRepository.save(any(Groupe.class))).thenAnswer(invocation -> {
            Groupe toSave = invocation.getArgument(0);
            ReflectionTestUtils.setField(toSave, "id", 5L);
            return toSave;
        });

        GroupeResponseDTO response = groupeService.createGroupe(validDTO);

        assertEquals(5L, response.getId());
        assertEquals("Groupe 1 - GL", response.getName());
        assertEquals(30, response.getCapacity());
        assertEquals(1L, response.getFiliereId());
        assertEquals("M2I-ISI", response.getFiliereCode());
    }

    @Test
    @DisplayName("createGroupe - Filière inexistante (404)")
    void createGroupe_FiliereNotFound() {
        validDTO.setFiliereId(99L);
        when(filiereRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> groupeService.createGroupe(validDTO));
        verify(groupeRepository, never()).save(any());
    }

    @Test
    @DisplayName("createGroupe - Conflit : même nom dans la même filière (409)")
    void createGroupe_DuplicateNameInFiliere() {
        when(filiereRepository.findById(1L)).thenReturn(Optional.of(filiereGL));
        when(groupeRepository.findByFiliereIdAndNameIgnoreCase(1L, "Groupe 1 - GL"))
                .thenReturn(Optional.of(groupe(3L, "Groupe 1 - GL")));

        assertThrows(ConflictException.class, () -> groupeService.createGroupe(validDTO));
        verify(groupeRepository, never()).save(any());
    }

    @Test
    @DisplayName("getAllGroupes - Sans filtre : tous les groupes")
    void getAllGroupes_All() {
        when(groupeRepository.findAllByOrderByNameAsc())
                .thenReturn(List.of(groupe(1L, "Groupe 1 - GL"), groupe(2L, "Groupe 2 - GL")));

        assertEquals(2, groupeService.getAllGroupes(null).size());
        verify(filiereRepository, never()).findById(any());
    }

    @Test
    @DisplayName("getAllGroupes - Filtre par filière")
    void getAllGroupes_ByFiliere() {
        when(filiereRepository.findById(1L)).thenReturn(Optional.of(filiereGL));
        when(groupeRepository.findByFiliereIdOrderByNameAsc(1L)).thenReturn(List.of(groupe(1L, "Groupe 1 - GL")));

        List<GroupeResponseDTO> result = groupeService.getAllGroupes(1L);

        assertEquals(1, result.size());
        assertEquals("M2I-ISI", result.get(0).getFiliereCode());
    }

    @Test
    @DisplayName("getAllGroupes - Filtre sur une filière inexistante (404)")
    void getAllGroupes_FiliereNotFound() {
        when(filiereRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> groupeService.getAllGroupes(99L));
    }

    @Test
    @DisplayName("updateGroupe - Succès : renommer en gardant son propre nom libre")
    void updateGroupe_Success() {
        Groupe existing = groupe(1L, "Groupe 1 - GL");
        validDTO.setCapacity(35);
        when(groupeRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(filiereRepository.findById(1L)).thenReturn(Optional.of(filiereGL));
        when(groupeRepository.findByFiliereIdAndNameIgnoreCase(1L, "Groupe 1 - GL")).thenReturn(Optional.of(existing));
        when(groupeRepository.save(any(Groupe.class))).thenAnswer(invocation -> invocation.getArgument(0));

        GroupeResponseDTO response = groupeService.updateGroupe(1L, validDTO);

        assertEquals(35, response.getCapacity());
    }

    @Test
    @DisplayName("updateGroupe - Conflit : le nom est déjà pris par un autre groupe de la filière")
    void updateGroupe_NameUsedByAnother() {
        when(groupeRepository.findById(1L)).thenReturn(Optional.of(groupe(1L, "Groupe 2 - GL")));
        when(filiereRepository.findById(1L)).thenReturn(Optional.of(filiereGL));
        when(groupeRepository.findByFiliereIdAndNameIgnoreCase(1L, "Groupe 1 - GL"))
                .thenReturn(Optional.of(groupe(7L, "Groupe 1 - GL")));

        assertThrows(ConflictException.class, () -> groupeService.updateGroupe(1L, validDTO));
    }

    @Test
    @DisplayName("deleteGroupe - Introuvable (404)")
    void deleteGroupe_NotFound() {
        when(groupeRepository.existsById(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> groupeService.deleteGroupe(99L));
        verify(groupeRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("deleteGroupe - Succès")
    void deleteGroupe_Success() {
        when(groupeRepository.existsById(1L)).thenReturn(true);

        groupeService.deleteGroupe(1L);

        verify(groupeRepository).deleteById(1L);
    }
}
