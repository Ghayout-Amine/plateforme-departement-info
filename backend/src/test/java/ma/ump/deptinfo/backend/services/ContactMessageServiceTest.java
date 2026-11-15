package ma.ump.deptinfo.backend.services;

import ma.ump.deptinfo.backend.dtos.ContactMessageDTO;
import ma.ump.deptinfo.backend.dtos.ContactMessageResponseDTO;
import ma.ump.deptinfo.backend.entities.ContactMessage;
import ma.ump.deptinfo.backend.entities.MessageStatus;
import ma.ump.deptinfo.backend.exceptions.ResourceNotFoundException;
import ma.ump.deptinfo.backend.repositories.ContactMessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContactMessageServiceTest {

    @Mock
    private ContactMessageRepository contactMessageRepository;

    @InjectMocks
    private ContactMessageService contactMessageService;

    private ContactMessageDTO validDTO;

    @BeforeEach
    void setUp() {
        validDTO = new ContactMessageDTO(
                "Youssef Mansouri",
                "youssef@example.com",
                "Demande d'information Master GL",
                "Bonjour, je souhaite avoir des informations sur les modalités d'inscription au Master GL 2026."
        );
    }

    @Test
    @DisplayName("saveMessage - Succès : enregistre le message avec le statut NEW")
    void saveMessage_Success() {
        ContactMessage saved = new ContactMessage(
                validDTO.getFullName(),
                validDTO.getEmail(),
                validDTO.getSubject(),
                validDTO.getMessage()
        );
        ReflectionTestUtils.setField(saved, "id", 1L);

        when(contactMessageRepository.save(any(ContactMessage.class))).thenReturn(saved);

        ContactMessageResponseDTO response = contactMessageService.saveMessage(validDTO);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Youssef Mansouri", response.getFullName());
        assertEquals("youssef@example.com", response.getEmail());
        assertEquals(MessageStatus.NEW, response.getStatus());

        verify(contactMessageRepository).save(any(ContactMessage.class));
    }

    @Test
    @DisplayName("markAsRead - Succès : passe le statut à READ")
    void markAsRead_Success() {
        ContactMessage message = new ContactMessage(
                "Youssef Mansouri",
                "youssef@example.com",
                "Demande d'information",
                "Message de test"
        );
        ReflectionTestUtils.setField(message, "id", 10L);

        when(contactMessageRepository.findById(10L)).thenReturn(Optional.of(message));
        when(contactMessageRepository.save(any(ContactMessage.class))).thenReturn(message);

        ContactMessageResponseDTO response = contactMessageService.markAsRead(10L);

        assertNotNull(response);
        assertEquals(MessageStatus.READ, response.getStatus());
        verify(contactMessageRepository).save(message);
    }

    @Test
    @DisplayName("markAsRead - Erreur 404 si le message n'existe pas")
    void markAsRead_NotFound_Throws404() {
        when(contactMessageRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                contactMessageService.markAsRead(999L));
    }
}
