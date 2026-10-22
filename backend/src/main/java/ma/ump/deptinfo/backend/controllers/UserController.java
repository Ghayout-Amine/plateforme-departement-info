package ma.ump.deptinfo.backend.controllers;

import jakarta.validation.Valid;
import ma.ump.deptinfo.backend.dtos.UserCreateDTO;
import ma.ump.deptinfo.backend.dtos.UserResponseDTO;
import ma.ump.deptinfo.backend.services.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    // Injection du service via le constructeur
    public UserController(UserService userService) {
        this.userService = userService;
    }

    // GET http://localhost:8080/api/users (Lister tous les utilisateurs)
    @GetMapping
    public ResponseEntity<List<UserResponseDTO>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    // GET http://localhost:8080/api/users/{id} (Consulter un utilisateur par son identifiant)
    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDTO> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    // POST http://localhost:8080/api/users (Créer un utilisateur)
    @PostMapping
    public ResponseEntity<UserResponseDTO> createUser(@Valid @RequestBody UserCreateDTO dto) {
        UserResponseDTO created = userService.createUser(dto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    // DELETE http://localhost:8080/api/users/{id} (Supprimer un utilisateur)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}