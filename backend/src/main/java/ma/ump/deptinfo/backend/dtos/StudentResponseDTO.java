package ma.ump.deptinfo.backend.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ma.ump.deptinfo.backend.entities.Gender;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentResponseDTO {

    private Long id;
    private String studentNumber;
    private String cne;
    private LocalDate birthDate;
    private String birthPlace;
    private Gender gender;
    private String address;
    private String city;
    private Long userId;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private Boolean enabled;
    private LocalDateTime createdAt;
}
