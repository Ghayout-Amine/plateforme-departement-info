
package ma.ump.deptinfo.backend.services;

import ma.ump.deptinfo.backend.dtos.AbsenceResponseDTO;
import ma.ump.deptinfo.backend.entities.Absence;
import ma.ump.deptinfo.backend.entities.AbsenceStatus;
import ma.ump.deptinfo.backend.entities.ModuleCours;
import ma.ump.deptinfo.backend.entities.Role;
import ma.ump.deptinfo.backend.entities.Student;
import ma.ump.deptinfo.backend.entities.User;

import ma.ump.deptinfo.backend.exceptions.ConflictException;
import ma.ump.deptinfo.backend.exceptions.ResourceNotFoundException;

import ma.ump.deptinfo.backend.repositories.AbsenceRepository;
import ma.ump.deptinfo.backend.repositories.ModuleCoursRepository;
import ma.ump.deptinfo.backend.repositories.StudentRepository;
import ma.ump.deptinfo.backend.repositories.UserRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AbsenceServiceTest {

    private AbsenceRepository absenceRepository;
    private UserRepository userRepository;
    private JustificatifStorageService storageService;

    private AbsenceService service;

    private User user;
    private Student student;
    private Absence absence;
    private MockMultipartFile file;

    private static final String EMAIL =
            "student@example.com";

    private static final String FILENAME =
            "550e8400-e29b-41d4-a716-446655440000.pdf";

    @BeforeEach
    void setUp() {

        absenceRepository = mock(AbsenceRepository.class);
        userRepository = mock(UserRepository.class);
        storageService = mock(JustificatifStorageService.class);

        service = new AbsenceService(
                absenceRepository,
                mock(StudentRepository.class),
                mock(ModuleCoursRepository.class),
                userRepository,
                mock(GradeService.class),
                mock(EnrollmentEligibilityValidator.class),
                storageService
        );

        user = mock(User.class);

        when(user.getId()).thenReturn(10L);
        when(user.getRole()).thenReturn(Role.ETUDIANT);
        when(user.getEnabled()).thenReturn(true);

        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(user));

        student = mock(Student.class);

        when(student.getId()).thenReturn(5L);
        when(student.getUser()).thenReturn(user);

        ModuleCours module = mock(ModuleCours.class);

        when(module.getId()).thenReturn(3L);

        absence = new Absence();
        absence.setStudent(student);
        absence.setModule(module);
        absence.setDate(LocalDate.of(2026, 11, 25));
        absence.setSessionTime("08:30-10:30");
        absence.setJustified(false);

        when(absenceRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(absence));

        file = new MockMultipartFile(
                "file",
                "certificat.pdf",
                "application/pdf",
                "%PDF-1.7".getBytes(StandardCharsets.US_ASCII)
        );

        TransactionSynchronizationManager
                .setActualTransactionActive(true);

        TransactionSynchronizationManager
                .initSynchronization();
    }

    @AfterEach
    void tearDown() {

        if (TransactionSynchronizationManager
                .isSynchronizationActive()) {

            TransactionSynchronizationManager
                    .clearSynchronization();
        }

        TransactionSynchronizationManager
                .setActualTransactionActive(false);
    }

    @Test
    void shouldUploadJustificatifAndUpdateStatus()
            throws Exception {

        when(storageService.store(file))
                .thenReturn(FILENAME);

        when(absenceRepository.save(absence))
                .thenReturn(absence);

        AbsenceResponseDTO response =
                service.uploadJustificatif(
                        1L, file, EMAIL);

        assertEquals(
                AbsenceStatus.PENDING_JUSTIFICATION,
                response.status()
        );

        assertFalse(response.justified());

        assertEquals(
                FILENAME,
                absence.getJustificatifFilename()
        );

        verify(storageService).store(file);

        verify(absenceRepository).save(absence);
    }

    @Test
    void shouldRejectAnotherStudentsAbsence()
            throws Exception {

        User anotherUser = mock(User.class);

        when(anotherUser.getId()).thenReturn(99L);

        when(student.getUser()).thenReturn(anotherUser);

        assertThrows(
                ResourceNotFoundException.class,
                () -> service.uploadJustificatif(
                        1L, file, EMAIL)
        );

        verify(storageService, never()).store(any());

        verify(absenceRepository, never()).save(any());
    }

    @Test
    void shouldRejectAlreadyJustifiedAbsence()
            throws Exception {

        absence.setJustified(true);

        assertThrows(
                ConflictException.class,
                () -> service.uploadJustificatif(
                        1L, file, EMAIL)
        );

        verify(storageService, never()).store(any());
    }

    @Test
    void shouldRejectDuplicateJustificatif()
            throws Exception {

        absence.markPendingJustification(FILENAME);

        assertThrows(
                ConflictException.class,
                () -> service.uploadJustificatif(
                        1L, file, EMAIL)
        );

        verify(storageService, never()).store(any());
    }

    @Test
    void shouldDeleteFileAfterRollback()
            throws Exception {

        when(storageService.store(file))
                .thenReturn(FILENAME);

        when(absenceRepository.save(absence))
                .thenThrow(new RuntimeException(
                        "Database error"));

        assertThrows(
                RuntimeException.class,
                () -> service.uploadJustificatif(
                        1L, file, EMAIL)
        );

        for (TransactionSynchronization synchronization :
                TransactionSynchronizationManager
                        .getSynchronizations()) {

            synchronization.afterCompletion(
                    TransactionSynchronization.STATUS_ROLLED_BACK
            );
        }

        verify(storageService).deleteQuietly(FILENAME);
    }
}
