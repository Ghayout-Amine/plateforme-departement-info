package ma.ump.deptinfo.backend.dtos;

import jakarta.validation.constraints.Email;
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
public class StudentCreateDTO {

    // Informations du compte utilisateur (créé en même temps que l'étudiant)
    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Format d'email invalide")
    private String email;

    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères")
    private String password;

    @NotBlank(message = "Le prénom est obligatoire")
    private String firstName;

    @NotBlank(message = "Le nom est obligatoire")
    private String lastName;

    private String phone;

    // Informations propres à l'étudiant
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
}
