package ma.ump.deptinfo.backend.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ma.ump.deptinfo.backend.entities.Gender;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentUpdateDTO {

    // L'email et le mot de passe ne se modifient pas ici (gestion du compte à part)
    @NotBlank(message = "Le prénom est obligatoire")
    private String firstName;

    @NotBlank(message = "Le nom est obligatoire")
    private String lastName;

    private String phone;

    @NotBlank(message = "Le numéro d'étudiant est obligatoire")
    @Size(max = 30)
    private String studentNumber;

    @Size(max = 20)
    private String cne;

    @Past(message = "La date de naissance doit être dans le passé")
    private LocalDate birthDate;

    @Size(max = 100)
    private String birthPlace;

    private Gender gender;

    @Size(max = 255)
    private String address;

    @Size(max = 80)
    private String city;

    private Boolean enabled;
}
