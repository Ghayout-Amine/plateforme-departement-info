package ma.ump.deptinfo.backend.services;

import ma.ump.deptinfo.backend.dtos.GradeCreateRequestDTO;
import ma.ump.deptinfo.backend.entities.Grade;
import ma.ump.deptinfo.backend.entities.GradeStatus;
import ma.ump.deptinfo.backend.entities.ModuleCours;
import ma.ump.deptinfo.backend.exceptions.BadRequestException;
import ma.ump.deptinfo.backend.repositories.AcademicYearRepository;
import ma.ump.deptinfo.backend.repositories.GradeRepository;
import ma.ump.deptinfo.backend.repositories.StudentRepository;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import ma.ump.deptinfo.backend.entities.Role;
import ma.ump.deptinfo.backend.entities.User;
import ma.ump.deptinfo.backend.exceptions.ResourceNotFoundException;
import ma.ump.deptinfo.backend.repositories.ModuleCoursRepository;
import ma.ump.deptinfo.backend.repositories.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@Transactional
public class GradeService {

    private final GradeRepository gradeRepository;
    private final StudentRepository studentRepository;
    private final EnrollmentEligibilityValidator enrollmentValidator;
    private final GradeAverageCalculator averageCalculator;
    private final AcademicYearRepository academicYearRepository;

    private final ModuleCoursRepository moduleCoursRepository;
    private final UserRepository userRepository;

    public GradeService(
            ModuleCoursRepository moduleCoursRepository,
            UserRepository userRepository,
            GradeRepository gradeRepository,
            StudentRepository studentRepository,
            EnrollmentEligibilityValidator enrollmentValidator,
            GradeAverageCalculator averageCalculator,
            AcademicYearRepository academicYearRepository) {
        this.moduleCoursRepository = moduleCoursRepository;
        this.userRepository = userRepository;
        this.gradeRepository = gradeRepository;
        this.studentRepository = studentRepository;
        this.enrollmentValidator = enrollmentValidator;
        this.averageCalculator = averageCalculator;
        this.academicYearRepository = academicYearRepository;
    }

    @Transactional(readOnly = true)
    public void verifyTeacherAccess(Long moduleId, String authenticatedEmail) {

        if (authenticatedEmail == null || authenticatedEmail.isBlank()) {
            throw new AccessDeniedException("Authentification requise");
        }

        User teacher = userRepository.findByEmail(authenticatedEmail)
                .orElseThrow(() ->
                        new AccessDeniedException("Utilisateur non autorise"));

        if (teacher.getRole() != Role.ENSEIGNANT
                || !Boolean.TRUE.equals(teacher.getEnabled())
                || teacher.getDeletedAt() != null) {
            throw new AccessDeniedException("Compte enseignant non autorise");
        }

        ModuleCours module = moduleCoursRepository.findById(moduleId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Module introuvable avec l'id : " + moduleId));

        if (module.getEnseignant() == null
                || !Objects.equals(
                        module.getEnseignant().getId(),
                        teacher.getId())) {
            throw new AccessDeniedException(
                    "Vous n'etes pas responsable de ce module");
        }
    }

    public Grade createDraftGrade(
            @Valid GradeCreateRequestDTO dto,
            String authenticatedEmail) {

        if (dto == null || dto.studentId() == null
                || dto.moduleId() == null || dto.score() == null
                || dto.type() == null || dto.coefficient() == null
                || dto.sessionDate() == null) {
            throw new BadRequestException("Donnees de note incompletes");
        }

        if (dto.score().compareTo(BigDecimal.ZERO) < 0
                || dto.score().compareTo(new BigDecimal("20")) > 0) {
            throw new BadRequestException("La note doit etre entre 0 et 20");
        }

        if (dto.coefficient().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Le coefficient doit etre positif");
        }

        if (dto.coefficient().compareTo(new BigDecimal("999.99")) > 0) {
            throw new BadRequestException(
                    "Le coefficient ne doit pas depasser 999.99");
        }

        try {
            dto.score().setScale(2, java.math.RoundingMode.UNNECESSARY);
            dto.coefficient().setScale(2, java.math.RoundingMode.UNNECESSARY);
        } catch (ArithmeticException ex) {
            throw new BadRequestException(
                    "La note et le coefficient doivent avoir au maximum deux decimales");
        }

        verifyTeacherAccess(dto.moduleId(), authenticatedEmail);

        ModuleCours module = moduleCoursRepository.findById(dto.moduleId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Module introuvable avec l'id : " + dto.moduleId()));

        enrollmentValidator.validate(
                dto.studentId(), module, dto.sessionDate());

        Grade grade = new Grade();
        grade.setStudent(studentRepository.findById(dto.studentId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Etudiant introuvable avec l'id : " + dto.studentId())));
        grade.setModule(module);
        grade.setScore(dto.score());
        grade.setType(dto.type());
        grade.setCoefficient(dto.coefficient());
        grade.setSessionDate(dto.sessionDate());
        grade.setStatus(GradeStatus.DRAFT);

        return gradeRepository.save(grade);
    }

    @Transactional(readOnly = true)
    public java.util.Optional<BigDecimal> calculateModuleAverage(
            Long studentId,
            Long moduleId,
            Long academicYearId,
            String authenticatedEmail) {

        if (studentId == null || moduleId == null
                || academicYearId == null) {
            throw new BadRequestException(
                    "Identifiants obligatoires pour calculer la moyenne");
        }

        verifyTeacherAccess(moduleId, authenticatedEmail);

        ModuleCours module = moduleCoursRepository.findById(moduleId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Module introuvable avec l'id : " + moduleId));

        ma.ump.deptinfo.backend.entities.AcademicYear year =
                academicYearRepository.findById(academicYearId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Annee universitaire introuvable avec l'id : "
                                + academicYearId));

        enrollmentValidator.validateForAcademicYear(
                studentId, module, academicYearId);

        return averageCalculator.calculate(
                gradeRepository.findByStudentIdAndModuleIdAndSessionDateBetween(
                        studentId,
                        moduleId,
                        year.getStartDate(),
                        year.getEndDate()));
    }

    public Grade publishGrade(
            Long gradeId,
            String authenticatedEmail) {

        if (gradeId == null) {
            throw new BadRequestException(
                    "Identifiant de la note obligatoire");
        }

        Grade grade = gradeRepository.findById(gradeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Note introuvable avec l'id : " + gradeId));

        verifyTeacherAccess(
                grade.getModule().getId(),
                authenticatedEmail);

        if (grade.getStatus() != GradeStatus.DRAFT) {
            throw new BadRequestException(
                    "Seule une note en brouillon peut etre publiee");
        }

        if (grade.getScore() == null
                || grade.getScore().compareTo(BigDecimal.ZERO) < 0
                || grade.getScore().compareTo(new BigDecimal("20.00")) > 0) {
            throw new BadRequestException(
                    "Note invalide : score obligatoire entre 0 et 20");
        }

        if (grade.getCoefficient() == null
                || grade.getCoefficient().compareTo(BigDecimal.ZERO) <= 0
                || grade.getCoefficient().compareTo(new BigDecimal("999.99")) > 0) {
            throw new BadRequestException(
                    "Note invalide : coefficient incorrect");
        }

        try {
            grade.getScore().setScale(
                    2, java.math.RoundingMode.UNNECESSARY);
            grade.getCoefficient().setScale(
                    2, java.math.RoundingMode.UNNECESSARY);
        } catch (ArithmeticException ex) {
            throw new BadRequestException(
                    "Note invalide : precision decimale excessive");
        }

        if (grade.getType() == null || grade.getSessionDate() == null) {
            throw new BadRequestException(
                    "Note invalide : type et date obligatoires");
        }

        grade.setStatus(GradeStatus.PUBLISHED);

        return gradeRepository.save(grade);
    }

    @Transactional(readOnly = true)
    public java.util.List<Grade> getMyPublishedGrades(
            String authenticatedEmail) {

        if (authenticatedEmail == null || authenticatedEmail.isBlank()) {
            throw new AccessDeniedException("Authentification requise");
        }

        User user = userRepository.findByEmail(authenticatedEmail)
                .orElseThrow(() -> new AccessDeniedException(
                        "Utilisateur non autorise"));

        if (user.getRole() != Role.ETUDIANT
                || !Boolean.TRUE.equals(user.getEnabled())
                || user.getDeletedAt() != null) {
            throw new AccessDeniedException(
                    "Compte etudiant non autorise");
        }

        return gradeRepository.findByStudent_User_EmailAndStatus(
                user.getEmail(), GradeStatus.PUBLISHED);
    }

    private ma.ump.deptinfo.backend.dtos.GradeResponseDTO toGradeResponse(
            Grade grade) {

        return new ma.ump.deptinfo.backend.dtos.GradeResponseDTO(
                grade.getId(),
                grade.getStudent().getId(),
                grade.getModule().getId(),
                grade.getScore(),
                grade.getType(),
                grade.getCoefficient(),
                grade.getSessionDate(),
                grade.getStatus()
        );
    }

    public ma.ump.deptinfo.backend.dtos.GradeResponseDTO createDraftGradeResponse(
            GradeCreateRequestDTO dto,
            String authenticatedEmail) {

        return toGradeResponse(
                createDraftGrade(dto, authenticatedEmail));
    }

    public ma.ump.deptinfo.backend.dtos.GradeResponseDTO publishGradeResponse(
            Long gradeId,
            String authenticatedEmail) {

        return toGradeResponse(
                publishGrade(gradeId, authenticatedEmail));
    }

    @Transactional(readOnly = true)
    public java.util.List<ma.ump.deptinfo.backend.dtos.GradeResponseDTO>
            getMyPublishedGradeResponses(String authenticatedEmail) {

        return getMyPublishedGrades(authenticatedEmail)
                .stream()
                .map(this::toGradeResponse)
                .toList();
    }
}