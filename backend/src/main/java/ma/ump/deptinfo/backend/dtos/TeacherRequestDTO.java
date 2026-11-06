package ma.ump.deptinfo.backend.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeacherRequestDTO {

    @NotBlank(message = "Le numéro de matricule est obligatoire")
    private String employeeNumber;

    private String academicTitle;

    private String specialty;

    private String office;

    private LocalDate hireDate;

    private String bio;

    @NotNull(message = "L'identifiant de l'utilisateur est obligatoire")
    private Long userId;
}
