package ma.ump.deptinfo.backend.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeacherResponseDTO {

    private Long id;
    private String employeeNumber;
    private String academicTitle;
    private String specialty;
    private String office;
    private LocalDate hireDate;
    private String bio;
    private Long userId;
    private String userEmail;
    private String firstName;
    private String lastName;
    private LocalDateTime createdAt;
}
