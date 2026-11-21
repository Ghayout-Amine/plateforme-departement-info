package ma.ump.deptinfo.backend.services;

import ma.ump.deptinfo.backend.dtos.InternshipRequestDTO;
import ma.ump.deptinfo.backend.dtos.InternshipResponseDTO;
import ma.ump.deptinfo.backend.dtos.InternshipValidationDTO;
import ma.ump.deptinfo.backend.entities.*;
import ma.ump.deptinfo.backend.exceptions.BadRequestException;
import ma.ump.deptinfo.backend.exceptions.ConflictException;
import ma.ump.deptinfo.backend.exceptions.ResourceNotFoundException;
import ma.ump.deptinfo.backend.repositories.InternshipRepository;
import ma.ump.deptinfo.backend.repositories.StudentRepository;
import ma.ump.deptinfo.backend.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

// PGDI2026-80 (US66/US21) : déclaration et validation des conventions de stage
@ExtendWith(MockitoExtension.class)
class InternshipServiceTest {

    @Mock
    private InternshipRepository internshipRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AgreementStorageService storage;

    @InjectMocks
    private InternshipService internshipService;

    private Student student;
    private User studentUser;
    private User chef;
    private InternshipRequestDTO dto;
    private MockMultipartFile pdf;

    @BeforeEach
    void setUp() {
        studentUser = new User();
        ReflectionTestUtils.setField(studentUser, "id", 10L);
        studentUser.setFirstName("Fatima");
        studentUser.setLastName("Benali");
        studentUser.setRole(Role.ETUDIANT);

        student = new Student();
        ReflectionTestUtils.setField(student, "id", 1L);
        student.setStudentNumber("ETU2026-001");
        student.setUser(studentUser);

        chef = new User();
        ReflectionTestUtils.setField(chef, "id", 20L);
        chef.setRole(Role.CHEF_DEPARTEMENT);

        dto = new InternshipRequestDTO();
        dto.setSubject("Plateforme de gestion de stocks");
        dto.setInternshipType(InternshipType.PFE);
        dto.setCompanyName("TechMaroc");
        dto.setCompanyCountry("ma");
        dto.setStartDate(LocalDate.of(2027, 2, 1));
        dto.setEndDate(LocalDate.of(2027, 6, 30));

        pdf = new MockMultipartFile("file", "convention.pdf", "application/pdf", "%PDF-1.4 test".getBytes());
    }

    private Internship pendingInternship() {
        Internship i = new Internship();
        ReflectionTestUtils.setField(i, "id", 5L);
        i.setStudent(student);
        i.setSubject("Sujet");
        i.setCompanyName("TechMaroc");
        i.setStartDate(LocalDate.of(2027, 2, 1));
        i.setEndDate(LocalDate.of(2027, 6, 30));
        i.setAgreementFilePath("2b1f4c0e-4c3a-4a55-9d0e-1a2b3c4d5e6f.pdf");
        return i;
    }

    private InternshipValidationDTO decision(boolean approved, String comment) {
        InternshipValidationDTO v = new InternshipValidationDTO();
        v.setApproved(approved);
        v.setComment(comment);
        return v;
    }

    @Test
    @DisplayName("declare : enregistre le stage en attente de validation avec la convention")
    void declare_ok() throws IOException {
        when(studentRepository.findByUserEmail("fatima@etu.test")).thenReturn(Optional.of(student));
        when(internshipRepository.existsByStudentIdAndInternshipTypeAndStatusIn(eq(1L), eq(InternshipType.PFE), anyCollection()))
                .thenReturn(false);
        when(storage.store(pdf)).thenReturn("2b1f4c0e-4c3a-4a55-9d0e-1a2b3c4d5e6f.pdf");
        when(internshipRepository.save(any(Internship.class))).thenAnswer(inv -> inv.getArgument(0));

        InternshipResponseDTO result = internshipService.declare("fatima@etu.test", dto, pdf);

        assertEquals(InternshipStatus.PENDING_VALIDATION, result.getStatus());
        assertEquals("MA", result.getCompanyCountry());
        assertEquals("Fatima Benali", result.getStudentName());
        assertEquals("ETU2026-001", result.getStudentNumber());
    }

    @Test
    @DisplayName("declare : refuse une date de fin avant la date de début")
    void declare_datesInvalides() throws IOException {
        when(studentRepository.findByUserEmail("fatima@etu.test")).thenReturn(Optional.of(student));
        dto.setEndDate(dto.getStartDate().minusDays(1));

        assertThrows(BadRequestException.class, () -> internshipService.declare("fatima@etu.test", dto, pdf));
        verify(storage, never()).store(any());
        verify(internshipRepository, never()).save(any());
    }

    @Test
    @DisplayName("declare : refuse un deuxième dossier actif du même type (409)")
    void declare_doublon() throws IOException {
        when(studentRepository.findByUserEmail("fatima@etu.test")).thenReturn(Optional.of(student));
        when(internshipRepository.existsByStudentIdAndInternshipTypeAndStatusIn(eq(1L), eq(InternshipType.PFE), anyCollection()))
                .thenReturn(true);

        assertThrows(ConflictException.class, () -> internshipService.declare("fatima@etu.test", dto, pdf));
        verify(storage, never()).store(any());
    }

    @Test
    @DisplayName("declare : compte sans profil étudiant -> introuvable")
    void declare_sansProfilEtudiant() throws IOException {
        when(studentRepository.findByUserEmail("admin@pgdi.test")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> internshipService.declare("admin@pgdi.test", dto, pdf));
    }

    @Test
    @DisplayName("declare : supprime le fichier si l'enregistrement en base échoue")
    void declare_nettoieLeFichierSiEchec() throws IOException {
        when(studentRepository.findByUserEmail("fatima@etu.test")).thenReturn(Optional.of(student));
        when(internshipRepository.existsByStudentIdAndInternshipTypeAndStatusIn(any(), any(), anyCollection()))
                .thenReturn(false);
        when(storage.store(pdf)).thenReturn("2b1f4c0e-4c3a-4a55-9d0e-1a2b3c4d5e6f.pdf");
        when(internshipRepository.save(any(Internship.class))).thenThrow(new IllegalStateException("base indisponible"));

        assertThrows(IllegalStateException.class, () -> internshipService.declare("fatima@etu.test", dto, pdf));
        verify(storage).deleteQuietly("2b1f4c0e-4c3a-4a55-9d0e-1a2b3c4d5e6f.pdf");
    }

    @Test
    @DisplayName("validate : le département valide une convention en attente")
    void validate_approuve() {
        Internship internship = pendingInternship();
        when(internshipRepository.findById(5L)).thenReturn(Optional.of(internship));
        when(userRepository.findByEmail("chef@pgdi.test")).thenReturn(Optional.of(chef));
        when(internshipRepository.save(any(Internship.class))).thenAnswer(inv -> inv.getArgument(0));

        InternshipResponseDTO result = internshipService.validate(5L, decision(true, null), "chef@pgdi.test");

        assertEquals(InternshipStatus.VALIDATED, result.getStatus());
        assertNotNull(result.getValidatedAt());
        assertSame(chef, internship.getValidatedBy());
    }

    @Test
    @DisplayName("validate : un refus exige un motif (400)")
    void validate_refusSansMotif() {
        when(internshipRepository.findById(5L)).thenReturn(Optional.of(pendingInternship()));

        assertThrows(BadRequestException.class,
                () -> internshipService.validate(5L, decision(false, "  "), "chef@pgdi.test"));
        verify(internshipRepository, never()).save(any());
    }

    @Test
    @DisplayName("validate : refus avec motif")
    void validate_refusAvecMotif() {
        when(internshipRepository.findById(5L)).thenReturn(Optional.of(pendingInternship()));
        when(userRepository.findByEmail("chef@pgdi.test")).thenReturn(Optional.of(chef));
        when(internshipRepository.save(any(Internship.class))).thenAnswer(inv -> inv.getArgument(0));

        InternshipResponseDTO result = internshipService.validate(5L, decision(false, "Dates incohérentes"), "chef@pgdi.test");

        assertEquals(InternshipStatus.REJECTED, result.getStatus());
        assertEquals("Dates incohérentes", result.getValidationComment());
    }

    @Test
    @DisplayName("validate : un dossier déjà traité ne peut plus être modifié (409)")
    void validate_dejaTraite() {
        Internship internship = pendingInternship();
        internship.setStatus(InternshipStatus.VALIDATED);
        when(internshipRepository.findById(5L)).thenReturn(Optional.of(internship));

        assertThrows(ConflictException.class,
                () -> internshipService.validate(5L, decision(true, null), "chef@pgdi.test"));
    }

    @Test
    @DisplayName("getAgreement : le propriétaire peut lire sa convention")
    void getAgreement_proprietaire() throws IOException {
        Internship internship = pendingInternship();
        when(internshipRepository.findById(5L)).thenReturn(Optional.of(internship));
        when(userRepository.findByEmail("fatima@etu.test")).thenReturn(Optional.of(studentUser));
        when(storage.load(internship.getAgreementFilePath())).thenReturn("%PDF-".getBytes());

        assertArrayEquals("%PDF-".getBytes(), internshipService.getAgreement(5L, "fatima@etu.test"));
    }

    @Test
    @DisplayName("getAgreement : un autre étudiant n'a pas accès (403)")
    void getAgreement_autreEtudiant() throws IOException {
        User autre = new User();
        ReflectionTestUtils.setField(autre, "id", 99L);
        autre.setRole(Role.ETUDIANT);
        when(internshipRepository.findById(5L)).thenReturn(Optional.of(pendingInternship()));
        when(userRepository.findByEmail("autre@etu.test")).thenReturn(Optional.of(autre));

        assertThrows(AccessDeniedException.class, () -> internshipService.getAgreement(5L, "autre@etu.test"));
        verify(storage, never()).load(any());
    }

    @Test
    @DisplayName("getAgreement : le département peut lire toutes les conventions")
    void getAgreement_departement() throws IOException {
        Internship internship = pendingInternship();
        when(internshipRepository.findById(5L)).thenReturn(Optional.of(internship));
        when(userRepository.findByEmail("chef@pgdi.test")).thenReturn(Optional.of(chef));
        when(storage.load(internship.getAgreementFilePath())).thenReturn(new byte[]{1});

        assertEquals(1, internshipService.getAgreement(5L, "chef@pgdi.test").length);
    }
}
