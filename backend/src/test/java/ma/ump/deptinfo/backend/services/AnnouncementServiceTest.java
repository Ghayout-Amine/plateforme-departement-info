package ma.ump.deptinfo.backend.services;

import ma.ump.deptinfo.backend.dtos.AnnouncementRequestDTO;
import ma.ump.deptinfo.backend.dtos.AnnouncementResponseDTO;
import ma.ump.deptinfo.backend.entities.Announcement;
import ma.ump.deptinfo.backend.entities.AnnouncementPriority;
import ma.ump.deptinfo.backend.entities.User;
import ma.ump.deptinfo.backend.exceptions.ResourceNotFoundException;
import ma.ump.deptinfo.backend.repositories.AnnouncementRepository;
import ma.ump.deptinfo.backend.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnnouncementServiceTest {

    @Mock
    private AnnouncementRepository announcementRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AnnouncementService announcementService;

    private User chef;

    @BeforeEach
    void setUp() {
        chef = new User();
        chef.setEmail("chef@pgdi.test");
        chef.setFirstName("Karim");
        chef.setLastName("Alaoui");
    }

    private Announcement announcement(Long id, String title, boolean pinned) {
        Announcement a = new Announcement();
        ReflectionTestUtils.setField(a, "id", id);
        a.setTitle(title);
        a.setContent("Contenu de l'annonce");
        a.setPinned(pinned);
        a.setPublishedAt(LocalDateTime.of(2026, 11, 20, 9, 0));
        a.setAuthor(chef);
        return a;
    }

    @Test
    @DisplayName("create - Succès : auteur connecté, priorité NORMALE et date du jour par défaut")
    void create_DefaultsAndAuthor() {
        when(userRepository.findByEmail("chef@pgdi.test")).thenReturn(Optional.of(chef));
        when(announcementRepository.save(any(Announcement.class))).thenAnswer(invocation -> {
            Announcement saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 1L);
            return saved;
        });
        AnnouncementRequestDTO dto = new AnnouncementRequestDTO("  Soutenances PFE  ", "Les soutenances auront lieu du 15 au 19 juin.", null, true, null);

        AnnouncementResponseDTO response = announcementService.create(dto, "chef@pgdi.test");

        assertEquals(1L, response.getId());
        assertEquals("Soutenances PFE", response.getTitle());
        assertEquals(AnnouncementPriority.NORMALE, response.getPriority());
        assertTrue(response.isPinned());
        assertNotNull(response.getPublishedAt());
        assertEquals("Karim Alaoui", response.getAuthorName());
    }

    @Test
    @DisplayName("create - Échec 404 : auteur introuvable")
    void create_UnknownAuthor() {
        when(userRepository.findByEmail("inconnu@pgdi.test")).thenReturn(Optional.empty());
        AnnouncementRequestDTO dto = new AnnouncementRequestDTO("Titre", "Contenu", AnnouncementPriority.URGENTE, false, null);

        assertThrows(ResourceNotFoundException.class, () -> announcementService.create(dto, "inconnu@pgdi.test"));
        verify(announcementRepository, never()).save(any());
    }

    @Test
    @DisplayName("getAll - L'ordre du repository (épinglées d'abord) est conservé")
    void getAll_PinnedFirst() {
        when(announcementRepository.findAllByOrderByPinnedDescPublishedAtDesc())
                .thenReturn(List.of(announcement(2L, "Épinglée", true), announcement(1L, "Normale", false)));

        List<AnnouncementResponseDTO> result = announcementService.getAll();

        assertEquals(2, result.size());
        assertTrue(result.get(0).isPinned());
        assertEquals("Épinglée", result.get(0).getTitle());
    }

    @Test
    @DisplayName("togglePin - Une annonce non épinglée devient épinglée")
    void togglePin_PinsAnnouncement() {
        Announcement existing = announcement(5L, "Examens", false);
        when(announcementRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(announcementRepository.save(existing)).thenReturn(existing);

        AnnouncementResponseDTO response = announcementService.togglePin(5L);

        assertTrue(response.isPinned());
    }

    @Test
    @DisplayName("update - Sans date fournie, la date de publication d'origine est conservée")
    void update_KeepsPublishedDate() {
        Announcement existing = announcement(5L, "Examens", false);
        when(announcementRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(announcementRepository.save(existing)).thenReturn(existing);
        AnnouncementRequestDTO dto = new AnnouncementRequestDTO("Examens (nouvelle salle)", "Amphi B", AnnouncementPriority.IMPORTANTE, false, null);

        AnnouncementResponseDTO response = announcementService.update(5L, dto);

        assertEquals("Examens (nouvelle salle)", response.getTitle());
        assertEquals(AnnouncementPriority.IMPORTANTE, response.getPriority());
        assertEquals(LocalDateTime.of(2026, 11, 20, 9, 0), response.getPublishedAt());
    }

    @Test
    @DisplayName("delete - Échec 404 : annonce inexistante")
    void delete_NotFound() {
        when(announcementRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> announcementService.delete(404L));
        verify(announcementRepository, never()).delete(any());
    }
}
