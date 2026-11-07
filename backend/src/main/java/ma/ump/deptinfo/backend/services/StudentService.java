package ma.ump.deptinfo.backend.services;

import ma.ump.deptinfo.backend.dtos.StudentCreateDTO;
import ma.ump.deptinfo.backend.dtos.StudentResponseDTO;
import ma.ump.deptinfo.backend.dtos.StudentUpdateDTO;
import ma.ump.deptinfo.backend.entities.Role;
import ma.ump.deptinfo.backend.entities.Student;
import ma.ump.deptinfo.backend.entities.User;
import ma.ump.deptinfo.backend.exceptions.ConflictException;
import ma.ump.deptinfo.backend.exceptions.ResourceNotFoundException;
import ma.ump.deptinfo.backend.repositories.StudentRepository;
import ma.ump.deptinfo.backend.repositories.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class StudentService {

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public StudentService(StudentRepository studentRepository,
                          UserRepository userRepository,
                          PasswordEncoder passwordEncoder) {
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Liste paginée, avec recherche optionnelle
    @Transactional(readOnly = true)
    public Page<StudentResponseDTO> getStudents(String search, Pageable pageable) {
        Page<Student> students = (search == null || search.isBlank())
                ? studentRepository.findAll(pageable)
                : studentRepository.search(search.trim(), pageable);
        return students.map(this::toResponseDTO);
    }

    // Récupérer un étudiant par son identifiant
    @Transactional(readOnly = true)
    public StudentResponseDTO getStudentById(Long id) {
        return toResponseDTO(findStudent(id));
    }

    // Créer un étudiant ET son compte utilisateur (rôle ETUDIANT)
    public StudentResponseDTO createStudent(StudentCreateDTO dto) {
        if (userRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new ConflictException("Un utilisateur avec cet email existe déjà !");
        }
        checkStudentNumberAvailable(dto.getStudentNumber(), null);
        String cne = normalize(dto.getCne());
        checkCneAvailable(cne, null);

        User user = new User();
        user.setEmail(dto.getEmail());
        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setPhone(dto.getPhone());
        user.setRole(Role.ETUDIANT);
        user.setEnabled(true);
        User savedUser = userRepository.save(user);

        Student student = new Student();
        student.setStudentNumber(dto.getStudentNumber());
        student.setCne(cne);
        student.setBirthDate(dto.getBirthDate());
        student.setBirthPlace(dto.getBirthPlace());
        student.setGender(dto.getGender());
        student.setAddress(dto.getAddress());
        student.setCity(dto.getCity());
        student.setUser(savedUser);

        return toResponseDTO(studentRepository.save(student));
    }

    // Mettre à jour un étudiant (et le nom / téléphone de son compte)
    public StudentResponseDTO updateStudent(Long id, StudentUpdateDTO dto) {
        Student student = findStudent(id);

        checkStudentNumberAvailable(dto.getStudentNumber(), id);
        String cne = normalize(dto.getCne());
        checkCneAvailable(cne, id);

        student.setStudentNumber(dto.getStudentNumber());
        student.setCne(cne);
        student.setBirthDate(dto.getBirthDate());
        student.setBirthPlace(dto.getBirthPlace());
        student.setGender(dto.getGender());
        student.setAddress(dto.getAddress());
        student.setCity(dto.getCity());

        User user = student.getUser();
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setPhone(dto.getPhone());
        if (dto.getEnabled() != null) {
            user.setEnabled(dto.getEnabled());
        }

        return toResponseDTO(studentRepository.save(student));
    }

    // Supprimer un étudiant et son compte utilisateur
    public void deleteStudent(Long id) {
        Student student = findStudent(id);
        User user = student.getUser();
        studentRepository.delete(student);
        studentRepository.flush(); // la FK students.user_id est en ON DELETE RESTRICT
        userRepository.delete(user);
    }

    // ---------- méthodes utilitaires ----------

    private Student findStudent(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Étudiant introuvable avec l'id : " + id));
    }

    private void checkStudentNumberAvailable(String studentNumber, Long currentId) {
        studentRepository.findByStudentNumber(studentNumber).ifPresent(existing -> {
            if (!existing.getId().equals(currentId)) {
                throw new ConflictException(
                        "Un étudiant avec ce numéro existe déjà : " + studentNumber);
            }
        });
    }

    private void checkCneAvailable(String cne, Long currentId) {
        if (cne == null) {
            return;
        }
        studentRepository.findByCne(cne).ifPresent(existing -> {
            if (!existing.getId().equals(currentId)) {
                throw new ConflictException("Un étudiant avec ce CNE existe déjà : " + cne);
            }
        });
    }

    // Un CNE vide est enregistré comme NULL (sinon la contrainte UNIQUE bloque)
    private String normalize(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    private StudentResponseDTO toResponseDTO(Student student) {
        User user = student.getUser();
        return new StudentResponseDTO(
                student.getId(),
                student.getStudentNumber(),
                student.getCne(),
                student.getBirthDate(),
                student.getBirthPlace(),
                student.getGender(),
                student.getAddress(),
                student.getCity(),
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                user.getEnabled(),
                student.getCreatedAt()
        );
    }
}
