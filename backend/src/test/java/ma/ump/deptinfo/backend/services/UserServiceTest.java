package ma.ump.deptinfo.backend.services;

import ma.ump.deptinfo.backend.dtos.*;
import ma.ump.deptinfo.backend.entities.Role;
import ma.ump.deptinfo.backend.entities.User;
import ma.ump.deptinfo.backend.exceptions.ConflictException;
import ma.ump.deptinfo.backend.exceptions.ResourceNotFoundException;
import ma.ump.deptinfo.backend.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService - Tests unitaires [PGDI2026-50]")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User existingUser;

    @BeforeEach
    void setUp() {
        existingUser = new User();
        existingUser.setId(1L);
        existingUser.setEmail("amine@pgdi.test");
        existingUser.setFirstName("Amine");
        existingUser.setLastName("Ghayout");
        existingUser.setPasswordHash("$2a$10$hashedpassword");
        existingUser.setRole(Role.ADMIN);
        existingUser.setEnabled(true);
    }

    // ─── Test 1 : Lister tous les utilisateurs ────────────────────────────────
    @Test
    @DisplayName("getAllUsers - doit retourner la liste de tous les utilisateurs")
    void getAllUsers_shouldReturnList() {
        when(userRepository.findAll()).thenReturn(List.of(existingUser));

        List<UserResponseDTO> result = userService.getAllUsers();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEmail()).isEqualTo("amine@pgdi.test");
        verify(userRepository, times(1)).findAll();
    }

    // ─── Test 2 : Récupérer un utilisateur par ID ────────────────────────────
    @Test
    @DisplayName("getUserById - doit retourner le bon utilisateur")
    void getUserById_shouldReturnUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));

        UserResponseDTO result = userService.getUserById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getFirstName()).isEqualTo("Amine");
    }

    // ─── Test 3 : Utilisateur introuvable par ID ─────────────────────────────
    @Test
    @DisplayName("getUserById - doit lancer ResourceNotFoundException si introuvable")
    void getUserById_shouldThrowIfNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // ─── Test 4 : Toggle statut d'un utilisateur (activer/désactiver) ─────────
    @Test
    @DisplayName("toggleUserStatus - doit inverser le statut enabled")
    void toggleUserStatus_shouldFlipEnabled() {
        existingUser.setEnabled(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponseDTO result = userService.toggleUserStatus(1L);

        assertThat(result.getEnabled()).isFalse();
    }

    // ─── Test 5 : Changer le rôle d'un utilisateur ───────────────────────────
    @Test
    @DisplayName("changeUserRole - doit changer le rôle correctement")
    void changeUserRole_shouldUpdateRole() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponseDTO result = userService.changeUserRole(1L, Role.ENSEIGNANT);

        assertThat(result.getRole()).isEqualTo(Role.ENSEIGNANT);
    }

    // ─── Test 6 : Changer mot de passe avec mauvais ancien mot de passe ───────
    @Test
    @DisplayName("changePassword - doit lancer ConflictException si ancien mdp incorrect")
    void changePassword_shouldThrowIfOldPasswordWrong() {
        ChangePasswordDTO dto = new ChangePasswordDTO();
        dto.setOldPassword("wrongPassword");
        dto.setNewPassword("newPassword123");

        when(userRepository.findByEmail("amine@pgdi.test")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("wrongPassword", existingUser.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> userService.changePassword("amine@pgdi.test", dto))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("incorrect");
    }

    // ─── Test 7 : Changer mot de passe avec succès ───────────────────────────
    @Test
    @DisplayName("changePassword - doit changer le mot de passe avec succès")
    void changePassword_shouldSucceedWithCorrectOldPassword() {
        ChangePasswordDTO dto = new ChangePasswordDTO();
        dto.setOldPassword("correctPassword");
        dto.setNewPassword("newPassword123");

        when(userRepository.findByEmail("amine@pgdi.test")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("correctPassword", existingUser.getPasswordHash())).thenReturn(true);
        when(passwordEncoder.encode("newPassword123")).thenReturn("$2a$10$newhashedpassword");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThatCode(() -> userService.changePassword("amine@pgdi.test", dto))
                .doesNotThrowAnyException();

        verify(userRepository, times(1)).save(any(User.class));
    }

    // ─── Test 8 : Consulter mon profil ───────────────────────────────────────
    @Test
    @DisplayName("getMyProfile - doit retourner le profil de l'utilisateur connecté")
    void getMyProfile_shouldReturnProfile() {
        when(userRepository.findByEmail("amine@pgdi.test")).thenReturn(Optional.of(existingUser));

        UserResponseDTO result = userService.getMyProfile("amine@pgdi.test");

        assertThat(result.getEmail()).isEqualTo("amine@pgdi.test");
        assertThat(result.getRole()).isEqualTo(Role.ADMIN);
    }

    // ─── Test 9 : Supprimer un utilisateur ───────────────────────────────────
    @Test
    @DisplayName("deleteUser - doit supprimer l'utilisateur s'il existe")
    void deleteUser_shouldDeleteIfExists() {
        when(userRepository.existsById(1L)).thenReturn(true);

        assertThatCode(() -> userService.deleteUser(1L))
                .doesNotThrowAnyException();

        verify(userRepository, times(1)).deleteById(1L);
    }

    // ─── Test 10 : Créer un utilisateur avec email déjà existant ─────────────
    @Test
    @DisplayName("createUser - doit lancer ConflictException si l'email existe déjà")
    void createUser_shouldThrowIfEmailAlreadyExists() {
        UserCreateDTO dto = new UserCreateDTO();
        dto.setEmail("amine@pgdi.test");
        dto.setPassword("password123");
        dto.setFirstName("Amine");
        dto.setLastName("Ghayout");

        when(userRepository.findByEmail("amine@pgdi.test")).thenReturn(Optional.of(existingUser));

        assertThatThrownBy(() -> userService.createUser(dto))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("existe déjà");
    }
}
