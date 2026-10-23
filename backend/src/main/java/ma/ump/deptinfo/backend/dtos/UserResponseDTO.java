package ma.ump.deptinfo.backend.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ma.ump.deptinfo.backend.entities.Role;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDTO {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private Role role;
    private Boolean enabled;
    private LocalDateTime createdAt;
}