package ma.ump.deptinfo.backend.services;

import ma.ump.deptinfo.backend.dtos.UserCreateDTO;
import ma.ump.deptinfo.backend.dtos.UserResponseDTO;
import ma.ump.deptinfo.backend.entities.User;
import ma.ump.deptinfo.backend.repositories.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Récupérer tous les utilisateurs
    public List<UserResponseDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    // Récupérer un utilisateur par son identifiant
    public UserResponseDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable avec l'id : " + id));
        return toResponseDTO(user);
    }

    // Créer un utilisateur avec vérification et hachage du mot de passe
    public UserResponseDTO createUser(UserCreateDTO dto) {
        if (userRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new RuntimeException("Un utilisateur avec cet email existe déjà !");
        }

        User user = new User();
        user.setEmail(dto.getEmail());
        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setPhone(dto.getPhone());
        user.setEnabled(true);

        User saved = userRepository.save(user);
        return toResponseDTO(saved);
    }

    // Supprimer un utilisateur
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("Utilisateur introuvable avec l'id : " + id);
        }
        userRepository.deleteById(id);
    }

    // Méthode de conversion entité -> DTO de réponse
    private UserResponseDTO toResponseDTO(User user) {
        return new UserResponseDTO(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                user.getEnabled(),
                user.getCreatedAt()
        );
    }
}