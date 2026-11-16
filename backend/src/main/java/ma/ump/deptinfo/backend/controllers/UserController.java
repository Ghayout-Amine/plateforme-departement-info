package ma.ump.deptinfo.backend.controllers;

import jakarta.validation.Valid;
import ma.ump.deptinfo.backend.dtos.*;
import ma.ump.deptinfo.backend.entities.Role;
import ma.ump.deptinfo.backend.services.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // ─── ADMIN : Lister tous les utilisateurs (sans pagination) ──────────────
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('CHEF_DEPARTEMENT')")
    public ResponseEntity<List<UserResponseDTO>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    // ─── ADMIN : Lister avec filtres et pagination ────────────────────────────
    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CHEF_DEPARTEMENT')")
    public ResponseEntity<Map<String, Object>> getUsersFiltered(
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(userService.getUsersWithFilters(role, enabled, search, page, size));
    }

    // ─── ADMIN : Récupérer un utilisateur par ID ─────────────────────────────
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponseDTO> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    // ─── ADMIN : Créer un utilisateur ────────────────────────────────────────
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponseDTO> createUser(@Valid @RequestBody UserCreateDTO dto) {
        return new ResponseEntity<>(userService.createUser(dto), HttpStatus.CREATED);
    }

    // ─── ADMIN : Mettre à jour un utilisateur ────────────────────────────────
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponseDTO> updateUser(@PathVariable Long id,
                                                       @Valid @RequestBody UserUpdateDTO dto) {
        return ResponseEntity.ok(userService.updateUser(id, dto));
    }

    // ─── ADMIN : Activer / Désactiver un compte (toggle) ─────────────────────
    @PatchMapping("/{id}/toggle-status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponseDTO> toggleStatus(@PathVariable Long id) {
        return ResponseEntity.ok(userService.toggleUserStatus(id));
    }

    // ─── ADMIN : Changer le rôle d'un utilisateur ────────────────────────────
    @PatchMapping("/{id}/change-role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponseDTO> changeRole(@PathVariable Long id,
                                                       @RequestParam Role role) {
        return ResponseEntity.ok(userService.changeUserRole(id, role));
    }

    // ─── ADMIN : Supprimer un utilisateur ────────────────────────────────────
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    // ─── PROFIL : Mon profil (utilisateur connecté) ───────────────────────────
    @GetMapping("/profile")
    public ResponseEntity<UserResponseDTO> getMyProfile(Authentication auth) {
        return ResponseEntity.ok(userService.getMyProfile(auth.getName()));
    }

    // ─── PROFIL : Mettre à jour mon profil ───────────────────────────────────
    @PutMapping("/profile")
    public ResponseEntity<UserResponseDTO> updateMyProfile(Authentication auth,
                                                            @Valid @RequestBody UpdateProfileDTO dto) {
        return ResponseEntity.ok(userService.updateMyProfile(auth.getName(), dto));
    }

    // ─── PROFIL : Changer mon mot de passe ───────────────────────────────────
    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(Authentication auth,
                                                               @Valid @RequestBody ChangePasswordDTO dto) {
        userService.changePassword(auth.getName(), dto);
        return ResponseEntity.ok(Map.of("message", "Mot de passe modifié avec succès."));
    }
}
