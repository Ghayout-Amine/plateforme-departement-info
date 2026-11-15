package ma.ump.deptinfo.backend.services;

import ma.ump.deptinfo.backend.dtos.TeacherRequestDTO;
import ma.ump.deptinfo.backend.dtos.TeacherResponseDTO;
import ma.ump.deptinfo.backend.entities.Role;
import ma.ump.deptinfo.backend.entities.Teacher;
import ma.ump.deptinfo.backend.entities.User;
import ma.ump.deptinfo.backend.exceptions.ConflictException;
import ma.ump.deptinfo.backend.exceptions.ResourceNotFoundException;
import ma.ump.deptinfo.backend.repositories.TeacherRepository;
import ma.ump.deptinfo.backend.repositories.UserRepository;
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
class TeacherServiceTest {

    @Mock
    private TeacherRepository teacherRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TeacherService teacherService;

    private TeacherRequestDTO validDTO;
    private User teacherUser;

    @BeforeEach
    void setUp() {
        teacherUser = new User();
        teacherUser.setId(20L);
        teacherUser.setEmail("prof.tazi@pgdi.test");
        teacherUser.setRole(Role.ENSEIGNANT);
        teacherUser.setFirstName("Ahmed");
        teacherUser.setLastName("Tazi");

        validDTO = new TeacherRequestDTO();
        validDTO.setEmployeeNumber("ENS-2026-001");
        validDTO.setAcademicTitle("Professeur Habilité");
        validDTO.setSpecialty("Génie Logiciel");
        validDTO.setOffice("Bureau B-12");
        validDTO.setHireDate(LocalDate.of(2018, 9, 1));
        validDTO.setBio("Enseignant-chercheur en GL");
        validDTO.setUserId(20L);
    }

    @Test
    @DisplayName("createTeacher - Succès : associe l'utilisateur ENSEIGNANT et sauvegarde")
    void createTeacher_Success() {
        when(teacherRepository.findByEmployeeNumber(validDTO.getEmployeeNumber())).thenReturn(Optional.empty());
        when(userRepository.findById(validDTO.getUserId())).thenReturn(Optional.of(teacherUser));
        when(teacherRepository.existsByUserId(validDTO.getUserId())).thenReturn(false);

        Teacher saved = new Teacher();
        ReflectionTestUtils.setField(saved, "id", 1L);
        saved.setEmployeeNumber(validDTO.getEmployeeNumber());
        saved.setAcademicTitle(validDTO.getAcademicTitle());
        saved.setSpecialty(validDTO.getSpecialty());
        saved.setUser(teacherUser);

        when(teacherRepository.save(any(Teacher.class))).thenReturn(saved);

        TeacherResponseDTO response = teacherService.createTeacher(validDTO);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("ENS-2026-001", response.getEmployeeNumber());
        assertEquals(20L, response.getUserId());
        verify(teacherRepository).save(any(Teacher.class));
    }

    @Test
    @DisplayName("createTeacher - Erreur 409 si le matricule existe déjà")
    void createTeacher_EmployeeNumberExists_ThrowsConflict() {
        when(teacherRepository.findByEmployeeNumber(validDTO.getEmployeeNumber()))
                .thenReturn(Optional.of(new Teacher()));

        ConflictException ex = assertThrows(ConflictException.class, () ->
                teacherService.createTeacher(validDTO));

        assertTrue(ex.getMessage().contains("matricule existe déjà"));
        verify(teacherRepository, never()).save(any());
    }

    @Test
    @DisplayName("createTeacher - Erreur 409 si le compte User n'a pas le rôle ENSEIGNANT")
    void createTeacher_WrongRole_ThrowsConflict() {
        teacherUser.setRole(Role.ETUDIANT);
        when(teacherRepository.findByEmployeeNumber(validDTO.getEmployeeNumber())).thenReturn(Optional.empty());
        when(userRepository.findById(validDTO.getUserId())).thenReturn(Optional.of(teacherUser));

        ConflictException ex = assertThrows(ConflictException.class, () ->
                teacherService.createTeacher(validDTO));

        assertTrue(ex.getMessage().contains("doit avoir le rôle ENSEIGNANT"));
    }

    @Test
    @DisplayName("getTeacherById - Erreur 404 si l'enseignant n'existe pas")
    void getTeacherById_NotFound_Throws404() {
        when(teacherRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                teacherService.getTeacherById(999L));
    }

    @Test
    @DisplayName("deleteTeacher - Succès : supprime l'enseignant")
    void deleteTeacher_Success() {
        when(teacherRepository.existsById(15L)).thenReturn(true);

        teacherService.deleteTeacher(15L);

        verify(teacherRepository).deleteById(15L);
    }
}
