package ma.ump.deptinfo.backend.services;

import ma.ump.deptinfo.backend.dtos.*;
import ma.ump.deptinfo.backend.entities.Role;
import ma.ump.deptinfo.backend.entities.User;
import ma.ump.deptinfo.backend.entities.Teacher;
import ma.ump.deptinfo.backend.exceptions.ConflictException;
import ma.ump.deptinfo.backend.exceptions.ResourceNotFoundException;
import ma.ump.deptinfo.backend.repositories.TeacherRepository;
import ma.ump.deptinfo.backend.repositories.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TeacherRepository teacherRepository;
    private final TeacherPhotoService teacherPhotoService;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       TeacherRepository teacherRepository,
                       TeacherPhotoService teacherPhotoService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.teacherRepository = teacherRepository;
        this.teacherPhotoService = teacherPhotoService;
    }

    // ─── ADMIN : Lister tous les utilisateurs (avec filtres + pagination) ────
    public List<UserResponseDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    public Map<String, Object> getUsersWithFilters(Role role, Boolean enabled, String search, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("lastName").ascending());
        Page<User> userPage = userRepository.findWithFilters(role, enabled, search, pageable);

        Map<String, Object> response = new HashMap<>();
        response.put("users", userPage.getContent().stream().map(this::toResponseDTO).collect(Collectors.toList()));
        response.put("currentPage", userPage.getNumber());
        response.put("totalItems", userPage.getTotalElements());
        response.put("totalPages", userPage.getTotalPages());
        return response;
    }

    // ─── ADMIN : Récupérer un utilisateur par son identifiant ────────────────
    public UserResponseDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable avec l'id : " + id));
        return toResponseDTO(user);
    }

    // ─── ADMIN : Créer un utilisateur ────────────────────────────────────────
    public UserResponseDTO createUser(UserCreateDTO dto) {
        if (userRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new ConflictException("Un utilisateur avec cet email existe déjà !");
        }

        User user = new User();
        user.setEmail(dto.getEmail());
        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setPhone(dto.getPhone());
        user.setRole(dto.getRole() != null ? dto.getRole() : Role.ENSEIGNANT);
        user.setEnabled(true);

        User saved = userRepository.save(user);
        return toResponseDTO(saved);
    }

    // ─── ADMIN : Mettre à jour un utilisateur ────────────────────────────────
    public UserResponseDTO updateUser(Long id, UserUpdateDTO dto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable avec l'id : " + id));

        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setPhone(dto.getPhone());

        if (dto.getRole() != null) {
            user.setRole(dto.getRole());
        }
        if (dto.getEnabled() != null) {
            user.setEnabled(dto.getEnabled());
        }

        User updated = userRepository.save(user);
        return toResponseDTO(updated);
    }

    // ─── ADMIN : Activer / Désactiver un compte ───────────────────────────────
    public UserResponseDTO toggleUserStatus(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable avec l'id : " + id));
        user.setEnabled(!user.getEnabled());
        return toResponseDTO(userRepository.save(user));
    }

    // ─── ADMIN : Changer le rôle d'un utilisateur ────────────────────────────
    public UserResponseDTO changeUserRole(Long id, Role newRole) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable avec l'id : " + id));
        user.setRole(newRole);
        return toResponseDTO(userRepository.save(user));
    }

    // ─── ADMIN : Supprimer un utilisateur ────────────────────────────────────
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("Utilisateur introuvable avec l'id : " + id);
        }
        userRepository.deleteById(id);
    }

    // ─── PROFIL : Consulter mon profil (utilisateur connecté) ────────────────
    public UserResponseDTO getMyProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable : " + email));
        return toResponseDTO(user);
    }

    // ─── PROFIL : Mettre à jour mon profil ───────────────────────────────────
    public UserResponseDTO updateMyProfile(String email, UpdateProfileDTO dto) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable : " + email));
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setPhone(dto.getPhone());
        if (dto.getAvatarUrl() != null) {
            user.setAvatarUrl(dto.getAvatarUrl());
        }
        return toResponseDTO(userRepository.save(user));
    }

    // ─── PROFIL : Changer mon mot de passe ───────────────────────────────────
    public void changePassword(String email, ChangePasswordDTO dto) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable : " + email));

        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPasswordHash())) {
            throw new ConflictException("L'ancien mot de passe est incorrect.");
        }

        user.setPasswordHash(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(user);
    }

    // ─── PROFIL : Téléverser ma photo de profil ──────────────────────────────
    public String uploadProfilePhoto(String email, MultipartFile file) throws IOException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable : " + email));

        Optional<Teacher> teacherOpt = teacherRepository.findByUserId(user.getId());
        if (teacherOpt.isPresent()) {
            return teacherPhotoService.upload(teacherOpt.get().getId(), file, email);
        }

        return teacherPhotoService.uploadUserPhoto(user, file);
    }

    // ─── PROFIL : Consulter la photo d'un utilisateur ────────────────────────
    public TeacherPhotoService.PhotoData getUserPhoto(Long userId) throws IOException {
        return teacherPhotoService.getUserPhoto(userId);
    }

    // ─── Conversion entité → DTO de réponse ──────────────────────────────────
    public UserResponseDTO toResponseDTO(User user) {
        return new UserResponseDTO(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                user.getRole(),
                user.getEnabled(),
                user.getAvatarUrl(),
                user.getCreatedAt(),
                user.getLastLoginAt()
        );
    }
}
