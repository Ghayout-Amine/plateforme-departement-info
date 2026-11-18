package ma.ump.deptinfo.backend.services;

import ma.ump.deptinfo.backend.dtos.AcademicYearRequestDTO;
import ma.ump.deptinfo.backend.dtos.AcademicYearResponseDTO;
import ma.ump.deptinfo.backend.entities.AcademicYear;
import ma.ump.deptinfo.backend.exceptions.BadRequestException;
import ma.ump.deptinfo.backend.exceptions.ConflictException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import ma.ump.deptinfo.backend.repositories.AcademicYearRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AcademicYearServiceTest {

    @Mock
    private AcademicYearRepository academicYearRepository;

    @InjectMocks
    private AcademicYearService academicYearService;

    private AcademicYearRequestDTO dto(String label, boolean current) {
        return new AcademicYearRequestDTO(label, LocalDate.of(2026, 9, 1), LocalDate.of(2027, 7, 15), current);
    }

    @Test
    @DisplayName("create - Succès : la nouvelle année en cours remplace l'ancienne")
    void create_CurrentReplacesPrevious() {
        AcademicYear previous = new AcademicYear();
        previous.setLabel("2025-2026");
        previous.setCurrent(true);

        when(academicYearRepository.findByLabel("2026-2027")).thenReturn(Optional.empty());
        when(academicYearRepository.findByCurrentTrue()).thenReturn(List.of(previous));
        when(academicYearRepository.save(any(AcademicYear.class))).thenAnswer(invocation -> {
            AcademicYear toSave = invocation.getArgument(0);
            ReflectionTestUtils.setField(toSave, "id", 2L);
            return toSave;
        });

        AcademicYearResponseDTO response = academicYearService.create(dto("2026-2027", true));

        assertEquals(2L, response.getId());
        assertTrue(response.isCurrent());
        assertFalse(previous.isCurrent());
    }

    @Test
    @DisplayName("create - Échec 400 : années non consécutives")
    void create_NonConsecutiveYears() {
        assertThrows(BadRequestException.class, () -> academicYearService.create(dto("2026-2028", false)));
        verify(academicYearRepository, never()).save(any());
    }

    @Test
    @DisplayName("create - Échec 400 : date de fin avant la date de début")
    void create_EndBeforeStart() {
        AcademicYearRequestDTO bad = new AcademicYearRequestDTO("2026-2027",
                LocalDate.of(2027, 7, 15), LocalDate.of(2026, 9, 1), false);

        assertThrows(BadRequestException.class, () -> academicYearService.create(bad));
        verify(academicYearRepository, never()).save(any());
    }

    @Test
    @DisplayName("create - Échec 409 : libellé déjà existant")
    void create_DuplicateLabel() {
        when(academicYearRepository.findByLabel("2026-2027")).thenReturn(Optional.of(new AcademicYear()));

        assertThrows(ConflictException.class, () -> academicYearService.create(dto("2026-2027", false)));
        verify(academicYearRepository, never()).save(any());
    }
}
