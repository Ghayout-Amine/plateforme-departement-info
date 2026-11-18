package ma.ump.deptinfo.backend.services;

import ma.ump.deptinfo.backend.dtos.EnrollmentRequestDTO;
import ma.ump.deptinfo.backend.dtos.EnrollmentResponseDTO;
import ma.ump.deptinfo.backend.entities.*;
import ma.ump.deptinfo.backend.exceptions.BadRequestException;
import ma.ump.deptinfo.backend.exceptions.ConflictException;
import ma.ump.deptinfo.backend.exceptions.ResourceNotFoundException;
import ma.ump.deptinfo.backend.repositories.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private FiliereRepository filiereRepository;
    @Mock
    private AcademicYearRepository academicYearRepository;
    @Mock
    private GroupeRepository groupeRepository;

    @InjectMocks
    private EnrollmentService enrollmentService;

    private Student student;
    private Filiere filiereGL;
    private Filiere filiereISI;
    private AcademicYear year;
    private Groupe groupeGL;
    private EnrollmentRequestDTO dto;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setFirstName("Fatima");
        user.setLastName("Benali");

        student = new Student();
        ReflectionTestUtils.setField(student, "id", 1L);
        student.setStudentNumber("ETU2026-001");
        student.setUser(user);

        filiereGL = filiere(2L, "M2I-GL");
        filiereISI = filiere(3L, "M2I-ISI");

        year = new AcademicYear();
        ReflectionTestUtils.setField(year, "id", 10L);
        year.setLabel("2026-2027");
        year.setStartDate(LocalDate.of(2026, 9, 1));
        year.setEndDate(LocalDate.of(2027, 7, 15));

        groupeGL = new Groupe();
        ReflectionTestUtils.setField(groupeGL, "id", 5L);
        groupeGL.setName("Groupe 1 - GL");
        groupeGL.setCapacity(30);
        groupeGL.setFiliere(filiereGL);

        dto = new EnrollmentRequestDTO(1L, 2L, 10L, 5L, null);
    }

    private Filiere filiere(Long id, String code) {
        Filiere f = new Filiere();
        f.setId(id);
        f.setCode(code);
        f.setNom("Filière " + code);
        f.setNiveau("MASTER");
        return f;
    }

    private void mockStudentFiliereYear() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(filiereRepository.findById(2L)).thenReturn(Optional.of(filiereGL));
        when(academicYearRepository.findById(10L)).thenReturn(Optional.of(year));
    }

    private Enrollment enrollment(Long id, Groupe groupe) {
        Enrollment e = new Enrollment();
        ReflectionTestUtils.setField(e, "id", id);
        e.setStudent(student);
        e.setFiliere(filiereGL);
        e.setAcademicYear(year);
        e.setGroupe(groupe);
        return e;
    }

    @Test
    @DisplayName("createEnrollment - Succès : inscription ACTIVE avec filière, année et groupe")
    void createEnrollment_Success() {
        mockStudentFiliereYear();
        when(enrollmentRepository.findByStudentIdAndAcademicYearId(1L, 10L)).thenReturn(Optional.empty());
        when(groupeRepository.findById(5L)).thenReturn(Optional.of(groupeGL));
        when(enrollmentRepository.countByGroupeId(5L)).thenReturn(12L);
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(invocation -> {
            Enrollment toSave = invocation.getArgument(0);
            ReflectionTestUtils.setField(toSave, "id", 100L);
            return toSave;
        });

        EnrollmentResponseDTO response = enrollmentService.createEnrollment(dto);

        assertEquals(100L, response.getId());
        assertEquals(EnrollmentStatus.ACTIVE, response.getStatus());
        assertEquals("ETU2026-001", response.getStudentNumber());
        assertEquals("Benali", response.getStudentLastName());
        assertEquals("M2I-GL", response.getFiliereCode());
        assertEquals("2026-2027", response.getAcademicYearLabel());
        assertEquals("Groupe 1 - GL", response.getGroupeName());
    }

    @Test
    @DisplayName("createEnrollment - Succès sans groupe : l'étudiant pourra être affecté plus tard")
    void createEnrollment_WithoutGroupe() {
        dto.setGroupeId(null);
        mockStudentFiliereYear();
        when(enrollmentRepository.findByStudentIdAndAcademicYearId(1L, 10L)).thenReturn(Optional.empty());
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EnrollmentResponseDTO response = enrollmentService.createEnrollment(dto);

        assertNull(response.getGroupeId());
        assertNull(response.getGroupeName());
        verify(groupeRepository, never()).findById(any());
    }

    @Test
    @DisplayName("createEnrollment - Échec 409 : déjà inscrit pour cette année")
    void createEnrollment_AlreadyEnrolledThisYear() {
        mockStudentFiliereYear();
        when(enrollmentRepository.findByStudentIdAndAcademicYearId(1L, 10L))
                .thenReturn(Optional.of(enrollment(99L, null)));

        ConflictException ex = assertThrows(ConflictException.class,
                () -> enrollmentService.createEnrollment(dto));

        assertTrue(ex.getMessage().contains("2026-2027"));
        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("createEnrollment - Échec 400 : le groupe n'appartient pas à la filière")
    void createEnrollment_GroupeOfAnotherFiliere() {
        groupeGL.setFiliere(filiereISI);
        mockStudentFiliereYear();
        when(enrollmentRepository.findByStudentIdAndAcademicYearId(1L, 10L)).thenReturn(Optional.empty());
        when(groupeRepository.findById(5L)).thenReturn(Optional.of(groupeGL));

        assertThrows(BadRequestException.class, () -> enrollmentService.createEnrollment(dto));
        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("createEnrollment - Échec 409 : le groupe est complet")
    void createEnrollment_GroupeFull() {
        mockStudentFiliereYear();
        when(enrollmentRepository.findByStudentIdAndAcademicYearId(1L, 10L)).thenReturn(Optional.empty());
        when(groupeRepository.findById(5L)).thenReturn(Optional.of(groupeGL));
        when(enrollmentRepository.countByGroupeId(5L)).thenReturn(30L);

        ConflictException ex = assertThrows(ConflictException.class,
                () -> enrollmentService.createEnrollment(dto));

        assertTrue(ex.getMessage().contains("complet"));
        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("createEnrollment - Échec 404 : étudiant inexistant")
    void createEnrollment_StudentNotFound() {
        when(studentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> enrollmentService.createEnrollment(dto));
        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateEnrollment - Rester dans un groupe complet ne compte pas l'étudiant deux fois")
    void updateEnrollment_SameGroupeWhenFull() {
        Enrollment existing = enrollment(100L, groupeGL);
        when(enrollmentRepository.findById(100L)).thenReturn(Optional.of(existing));
        mockStudentFiliereYear();
        when(enrollmentRepository.findByStudentIdAndAcademicYearId(1L, 10L)).thenReturn(Optional.of(existing));
        when(groupeRepository.findById(5L)).thenReturn(Optional.of(groupeGL));
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        dto.setStatus(EnrollmentStatus.REPEATING);

        EnrollmentResponseDTO response = enrollmentService.updateEnrollment(100L, dto);

        assertEquals(EnrollmentStatus.REPEATING, response.getStatus());
        assertEquals(5L, response.getGroupeId());
        verify(enrollmentRepository, never()).countByGroupeId(any());
    }

    @Test
    @DisplayName("getEnrollments - Les filtres sont transmis au repository")
    void getEnrollments_WithFilters() {
        when(enrollmentRepository.search(10L, 2L, null)).thenReturn(List.of(enrollment(100L, groupeGL)));

        List<EnrollmentResponseDTO> result = enrollmentService.getEnrollments(10L, 2L, null);

        assertEquals(1, result.size());
        assertEquals("Fatima", result.get(0).getStudentFirstName());
    }

    @Test
    @DisplayName("deleteEnrollment - Échec 404 : inscription inexistante")
    void deleteEnrollment_NotFound() {
        when(enrollmentRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> enrollmentService.deleteEnrollment(404L));
        verify(enrollmentRepository, never()).delete(any());
    }
}
