package ma.ump.deptinfo.backend.services;

import ma.ump.deptinfo.backend.dtos.SalleRequestDTO;
import ma.ump.deptinfo.backend.dtos.SalleResponseDTO;
import ma.ump.deptinfo.backend.entities.RoomType;
import ma.ump.deptinfo.backend.entities.Salle;
import ma.ump.deptinfo.backend.exceptions.ConflictException;
import ma.ump.deptinfo.backend.exceptions.ResourceNotFoundException;
import ma.ump.deptinfo.backend.repositories.SalleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
class SalleServiceTest {

    @Mock
    private SalleRepository salleRepository;

    @InjectMocks
    private SalleService salleService;

    private SalleRequestDTO validDTO;

    @BeforeEach
    void setUp() {
        validDTO = new SalleRequestDTO();
        validDTO.setCode(" amphi_a ");
        validDTO.setName("Amphithéâtre Al-Khawarizmi");
        validDTO.setBuilding("Bloc Informatique");
        validDTO.setCapacity(150);
        validDTO.setRoomType(RoomType.AMPHI);
    }

    private Salle salle(Long id, String code, RoomType type, boolean active) {
        Salle salle = new Salle();
        ReflectionTestUtils.setField(salle, "id", id);
        salle.setCode(code);
        salle.setName("Salle " + code);
        salle.setCapacity(30);
        salle.setRoomType(type);
        salle.setActive(active);
        return salle;
    }

    @Test
    @DisplayName("createSalle - Succès : code mis en majuscules, salle active par défaut")
    void createSalle_Success() {
        when(salleRepository.findByCodeIgnoreCase("AMPHI_A")).thenReturn(Optional.empty());
        when(salleRepository.save(any(Salle.class))).thenAnswer(invocation -> {
            Salle toSave = invocation.getArgument(0);
            ReflectionTestUtils.setField(toSave, "id", 1L);
            return toSave;
        });

        SalleResponseDTO response = salleService.createSalle(validDTO);

        assertEquals(1L, response.getId());
        assertEquals("AMPHI_A", response.getCode());
        assertEquals(RoomType.AMPHI, response.getRoomType());
        assertEquals(150, response.getCapacity());
        assertTrue(response.getIsActive());
    }

    @Test
    @DisplayName("createSalle - Conflit : code déjà utilisé (409)")
    void createSalle_DuplicateCode() {
        when(salleRepository.findByCodeIgnoreCase("AMPHI_A"))
                .thenReturn(Optional.of(salle(9L, "AMPHI_A", RoomType.AMPHI, true)));

        assertThrows(ConflictException.class, () -> salleService.createSalle(validDTO));
        verify(salleRepository, never()).save(any());
    }

    @Test
    @DisplayName("createSalle - isActive=false est respecté")
    void createSalle_Inactive() {
        validDTO.setIsActive(false);
        when(salleRepository.findByCodeIgnoreCase("AMPHI_A")).thenReturn(Optional.empty());
        when(salleRepository.save(any(Salle.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SalleResponseDTO response = salleService.createSalle(validDTO);

        assertFalse(response.getIsActive());
    }

    @Test
    @DisplayName("getAllSalles - Filtre par type et par état")
    void getAllSalles_Filters() {
        when(salleRepository.findAllByOrderByCodeAsc()).thenReturn(List.of(
                salle(1L, "AMPHI_A", RoomType.AMPHI, true),
                salle(2L, "TP1", RoomType.LAB, true),
                salle(3L, "TP2", RoomType.LAB, false)));

        assertEquals(3, salleService.getAllSalles(null, null).size());
        assertEquals(2, salleService.getAllSalles(RoomType.LAB, null).size());
        assertEquals(1, salleService.getAllSalles(RoomType.LAB, true).size());
        assertEquals(1, salleService.getAllSalles(null, false).size());
    }

    @Test
    @DisplayName("updateSalle - Conflit : le code appartient à une autre salle")
    void updateSalle_CodeUsedByAnother() {
        when(salleRepository.findById(1L)).thenReturn(Optional.of(salle(1L, "TP1", RoomType.LAB, true)));
        when(salleRepository.findByCodeIgnoreCase("AMPHI_A"))
                .thenReturn(Optional.of(salle(2L, "AMPHI_A", RoomType.AMPHI, true)));

        assertThrows(ConflictException.class, () -> salleService.updateSalle(1L, validDTO));
    }

    @Test
    @DisplayName("updateSalle - Succès : garder son propre code et désactiver la salle")
    void updateSalle_Success() {
        Salle existing = salle(1L, "AMPHI_A", RoomType.AMPHI, true);
        validDTO.setIsActive(false);
        validDTO.setCapacity(180);
        when(salleRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(salleRepository.findByCodeIgnoreCase("AMPHI_A")).thenReturn(Optional.of(existing));
        when(salleRepository.save(any(Salle.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SalleResponseDTO response = salleService.updateSalle(1L, validDTO);

        ArgumentCaptor<Salle> captor = ArgumentCaptor.forClass(Salle.class);
        verify(salleRepository).save(captor.capture());
        assertEquals(180, captor.getValue().getCapacity());
        assertFalse(response.getIsActive());
    }

    @Test
    @DisplayName("getSalleById - Introuvable (404)")
    void getSalleById_NotFound() {
        when(salleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> salleService.getSalleById(99L));
    }

    @Test
    @DisplayName("deleteSalle - Introuvable (404)")
    void deleteSalle_NotFound() {
        when(salleRepository.existsById(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> salleService.deleteSalle(99L));
        verify(salleRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("deleteSalle - Succès")
    void deleteSalle_Success() {
        when(salleRepository.existsById(1L)).thenReturn(true);

        salleService.deleteSalle(1L);

        verify(salleRepository).deleteById(1L);
    }
}
