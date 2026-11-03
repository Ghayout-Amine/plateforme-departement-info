package ma.ump.deptinfo.backend;

import ma.ump.deptinfo.backend.entities.Role;
import ma.ump.deptinfo.backend.entities.User;
import ma.ump.deptinfo.backend.repositories.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initializeTestUser(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {

        return args -> {
            createOrUpdateUser(userRepository, passwordEncoder,
                    "admin@pgdi.test", "Test@2026", "Amine", "Ghayout", Role.ADMIN);

            createOrUpdateUser(userRepository, passwordEncoder,
                    "chef.departement@pgdi.test", "Test@2026", "Karim", "El Fassi", Role.CHEF_DEPARTEMENT);

            createOrUpdateUser(userRepository, passwordEncoder,
                    "n.alami@pgdi.test", "Test@2026", "Nadia", "Alami", Role.ENSEIGNANT);

            createOrUpdateUser(userRepository, passwordEncoder,
                    "omar.tazi@etu.pgdi.test", "Test@2026", "Omar", "Tazi", Role.ETUDIANT);

            createOrUpdateUser(userRepository, passwordEncoder,
                    "test@pgdi.ma", "Test1234!", "Test", "User", Role.ADMIN);

            System.out.println("=================================================");
            System.out.println(">>> COMPTES DE TEST INITIALISÉS AVEC SUCCÈS   <<<");
            System.out.println(">>> admin@pgdi.test / Test@2026 (ADMIN)       <<<");
            System.out.println("=================================================");
        };
    }

    private void createOrUpdateUser(UserRepository repo, PasswordEncoder encoder,
                                    String email, String rawPassword,
                                    String firstName, String lastName, Role role) {
        User user = repo.findByEmail(email).orElse(new User());
        user.setEmail(email);
        user.setPasswordHash(encoder.encode(rawPassword));
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setRole(role);
        user.setEnabled(true);
        repo.save(user);
    }
}
