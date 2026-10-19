
package ma.ump.deptinfo.backend;

import ma.ump.deptinfo.backend.entities.User;
import ma.ump.deptinfo.backend.repositories.UserRepository;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@ConditionalOnProperty(
        name = "app.init-test-user",
        havingValue = "true"
)
public class DataInitializer {

    @Bean
    CommandLineRunner initializeTestUser(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {

        return args -> {
            String email = "test@pgdi.ma";

            if (userRepository.findByEmail(email).isEmpty()) {
                User user = new User();
                user.setEmail(email);
                user.setPasswordHash(
                        passwordEncoder.encode("Test1234!"));
                user.setFirstName("Test");
                user.setLastName("User");
                user.setEnabled(true);

                userRepository.save(user);

                System.out.println(
                        "Utilisateur de test créé : " + email);
            }
        };
    }
}
