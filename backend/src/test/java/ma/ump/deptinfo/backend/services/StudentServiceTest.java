package ma.ump.deptinfo.backend.services;

import ma.ump.deptinfo.backend.dtos.StudentCreateDTO;
import ma.ump.deptinfo.backend.dtos.StudentResponseDTO;
import ma.ump.deptinfo.backend.entities.Gender;
import ma.ump.deptinfo.backend.entities.Role;
import ma.ump.deptinfo.backend.entities.Student;
import ma.ump.deptinfo.backend.entities.User;
import ma.ump.deptinfo.backend.exceptions.ConflictException;
import ma.ump.deptinfo.backend.exceptions.ResourceNotFoundException;
import ma.ump.deptinfo.backend.repositories.StudentRepository;
import ma.ump.deptinfo.backend.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private StudentService studentService;

    private StudentCreateDTO validDTO;

    @BeforeEach
    void setUp() {
        validDTO = new StudentCreateDTO();
        validDTO.setEmail("karim.alami@etu.pgdi.test");
        validDTO.setPassword("Password123!");
        validDTO.setFirstName("Karim");
        validDTO.setLastName("Alami");
        validDTO.setStudentNumber("ETU2026-101");
        validDTO.setCne("R130123456");
        validDTO.setBirthDate(LocalDate.of(2002, 5, 14));
        validDTO.setBirthPlace("Oujda");
        validDTO.setGender(Gender.MALE);
        validDTO.setCity("Oujda");
    }

    @Test
    @DisplayName("createStudent - Succès : crée l'utilisateur et l'étudiant avec rôle ETUDIANT")
    void createStudent_Success() {
        when(userRepository.findByEmail(validDTO.getEmail())).thenReturn(Optional.empty());
        when(studentRepository.findByStudentNumber(validDTO.getStudentNumber())).thenReturn(Optional.empty());
        when(studentRepository.findByCne(validDTO.getCne())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(validDTO.getPassword())).thenReturn("hashed_password_xyz");

        User savedUser = new User();
        savedUser.setId(10L);
        savedUser.setEmail(validDTO.getEmail());
        savedUser.setRole(Role.ETUDIANT);
        savedUser.setFirstName(validDTO.getFirstName());
        savedUser.setLastName(validDTO.getLastName());
        savedUser.setEnabled(true);

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        Student savedStudent = new Student();
        ReflectionTestUtils.setField(savedStudent, "id", 50L);
        savedStudent.setStudentNumber(validDTO.getStudentNumber());
        savedStudent.setCne(validDTO.getCne());
        savedStudent.setUser(savedUser);

        when(studentRepository.save(any(Student.class))).thenReturn(savedStudent);

        StudentResponseDTO response = studentService.createStudent(validDTO);

        assertNotNull(response);
        assertEquals(50L, response.getId());
        assertEquals("ETU2026-101", response.getStudentNumber());
        assertEquals("karim.alami@etu.pgdi.test", response.getEmail());

        verify(passwordEncoder).encode("Password123!");
        verify(userRepository).save(any(User.class));
        verify(studentRepository).save(any(Student.class));
    }

    @Test
    @DisplayName("createStudent - Erreur 409 si l'email existe déjà")
    void createStudent_EmailExists_ThrowsConflict() {
        when(userRepository.findByEmail(validDTO.getEmail()))
                .thenReturn(Optional.of(new User()));

        ConflictException ex = assertThrows(ConflictException.class, () ->
                studentService.createStudent(validDTO));

        assertTrue(ex.getMessage().contains("email existe déjà"));
        verify(studentRepository, never()).save(any());
    }

    @Test
    @DisplayName("createStudent - Erreur 409 si le numéro d'étudiant existe déjà")
    void createStudent_StudentNumberExists_ThrowsConflict() {
        when(userRepository.findByEmail(validDTO.getEmail())).thenReturn(Optional.empty());
        Student existing = new Student();
        ReflectionTestUtils.setField(existing, "id", 99L);
        when(studentRepository.findByStudentNumber(validDTO.getStudentNumber()))
                .thenReturn(Optional.of(existing));

        ConflictException ex = assertThrows(ConflictException.class, () ->
                studentService.createStudent(validDTO));

        assertTrue(ex.getMessage().contains("ce numéro existe déjà"));
        verify(studentRepository, never()).save(any());
    }

    @Test
    @DisplayName("createStudent - Erreur 409 si le CNE existe déjà")
    void createStudent_CneExists_ThrowsConflict() {
        when(userRepository.findByEmail(validDTO.getEmail())).thenReturn(Optional.empty());
        when(studentRepository.findByStudentNumber(validDTO.getStudentNumber())).thenReturn(Optional.empty());
        Student existing = new Student();
        ReflectionTestUtils.setField(existing, "id", 88L);
        when(studentRepository.findByCne(validDTO.getCne())).thenReturn(Optional.of(existing));

        ConflictException ex = assertThrows(ConflictException.class, () ->
                studentService.createStudent(validDTO));

        assertTrue(ex.getMessage().contains("ce CNE existe déjà"));
        verify(studentRepository, never()).save(any());
    }

    @Test
    @DisplayName("getStudentById - Erreur 404 si l'étudiant n'existe pas")
    void getStudentById_NotFound_Throws404() {
        when(studentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                studentService.getStudentById(999L));
    }

    @Test
    @DisplayName("deleteStudent - Supprime l'étudiant puis son compte utilisateur")
    void deleteStudent_Success() {
        User user = new User();
        user.setId(5L);
        Student student = new Student();
        ReflectionTestUtils.setField(student, "id", 12L);
        student.setUser(user);

        when(studentRepository.findById(12L)).thenReturn(Optional.of(student));

        studentService.deleteStudent(12L);

        verify(studentRepository).delete(student);
        verify(studentRepository).flush();
        verify(userRepository).delete(user);
    }
}
