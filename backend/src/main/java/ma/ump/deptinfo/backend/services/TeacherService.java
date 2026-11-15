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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class TeacherService {

    private final TeacherRepository teacherRepository;
    private final UserRepository userRepository;

    public TeacherService(TeacherRepository teacherRepository,
                          UserRepository userRepository) {
        this.teacherRepository = teacherRepository;
        this.userRepository = userRepository;
    }

    // Récupérer tous les enseignants
    public List<TeacherResponseDTO> getAllTeachers() {
        return teacherRepository.findAll().stream()
                .map(this::toResponseDTO)
                .toList();
    }

    // Récupérer un enseignant par son identifiant
    public TeacherResponseDTO getTeacherById(Long id) {
        Teacher teacher = teacherRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Enseignant introuvable avec l'id : " + id));

        return toResponseDTO(teacher);
    }

    // Créer un nouvel enseignant
    public TeacherResponseDTO createTeacher(TeacherRequestDTO dto) {

        if (teacherRepository.findByEmployeeNumber(dto.getEmployeeNumber()).isPresent()) {
            throw new ConflictException(
                    "Un enseignant avec ce matricule existe déjà : " + dto.getEmployeeNumber());
        }

        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Utilisateur introuvable avec l'id : " + dto.getUserId()));

        if (user.getRole() != Role.ENSEIGNANT) {
            throw new ConflictException(
                    "Le compte associé doit avoir le rôle ENSEIGNANT.");
        }

        if (teacherRepository.existsByUserId(dto.getUserId())) {
            throw new ConflictException(
                    "Ce compte est déjà associé à un enseignant.");
        }

        Teacher teacher = new Teacher();
        teacher.setEmployeeNumber(dto.getEmployeeNumber());
        teacher.setAcademicTitle(dto.getAcademicTitle());
        teacher.setSpecialty(dto.getSpecialty());
        teacher.setOffice(dto.getOffice());
        teacher.setHireDate(dto.getHireDate());
        teacher.setBio(dto.getBio());
        teacher.setUser(user);

        Teacher saved = teacherRepository.save(teacher);
        return toResponseDTO(saved);
    }

    // Mettre à jour un enseignant
    public TeacherResponseDTO updateTeacher(Long id, TeacherRequestDTO dto) {

        Teacher teacher = teacherRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Enseignant introuvable avec l'id : " + id));

        teacherRepository.findByEmployeeNumber(dto.getEmployeeNumber())
                .ifPresent(existing -> {
                    if (!existing.getId().equals(id)) {
                        throw new ConflictException(
                                "Un autre enseignant utilise déjà ce matricule.");
                    }
                });

        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Utilisateur introuvable avec l'id : " + dto.getUserId()));

        if (user.getRole() != Role.ENSEIGNANT) {
            throw new ConflictException(
                    "Le compte associé doit avoir le rôle ENSEIGNANT.");
        }

        if (teacherRepository.existsByUserId(dto.getUserId())
                && !teacher.getUser().getId().equals(dto.getUserId())) {
            throw new ConflictException(
                    "Ce compte est déjà associé à un autre enseignant.");
        }

        teacher.setEmployeeNumber(dto.getEmployeeNumber());
        teacher.setAcademicTitle(dto.getAcademicTitle());
        teacher.setSpecialty(dto.getSpecialty());
        teacher.setOffice(dto.getOffice());
        teacher.setHireDate(dto.getHireDate());
        teacher.setBio(dto.getBio());
        teacher.setUser(user);

        Teacher updated = teacherRepository.save(teacher);
        return toResponseDTO(updated);
    }

    // Supprimer un enseignant
    public void deleteTeacher(Long id) {
        if (!teacherRepository.existsById(id)) {
            throw new ResourceNotFoundException(
                    "Enseignant introuvable avec l'id : " + id);
        }

        teacherRepository.deleteById(id);
    }

    // Conversion entité vers DTO de réponse
    private TeacherResponseDTO toResponseDTO(Teacher teacher) {
        User user = teacher.getUser();

        return new TeacherResponseDTO(
                teacher.getId(),
                teacher.getEmployeeNumber(),
                teacher.getAcademicTitle(),
                teacher.getSpecialty(),
                teacher.getOffice(),
                teacher.getHireDate(),
                teacher.getBio(),
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                teacher.getCreatedAt()
        );
    }
}
