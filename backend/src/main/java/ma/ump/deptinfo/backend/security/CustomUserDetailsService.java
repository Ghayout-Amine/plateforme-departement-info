
package ma.ump.deptinfo.backend.security;

import ma.ump.deptinfo.backend.entities.User;
import ma.ump.deptinfo.backend.repositories.UserRepository;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "Utilisateur introuvable : " + email));

        if (user.getDeletedAt() != null) {
            throw new UsernameNotFoundException(
                    "Ce compte n'existe plus.");
        }

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPasswordHash())
                .disabled(!Boolean.TRUE.equals(user.getEnabled()))
                .authorities("ROLE_USER")
                .build();
    }
}
