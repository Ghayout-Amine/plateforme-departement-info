package ma.ump.deptinfo.backend.services;

import ma.ump.deptinfo.backend.dtos.AbsenceJustificationDTO;
import ma.ump.deptinfo.backend.dtos.AbsenceRequestDTO;
import ma.ump.deptinfo.backend.dtos.AbsenceResponseDTO;
import ma.ump.deptinfo.backend.entities.Absence;
import ma.ump.deptinfo.backend.entities.ModuleCours;
import ma.ump.deptinfo.backend.entities.Role;
import ma.ump.deptinfo.backend.entities.Student;
import ma.ump.deptinfo.backend.entities.User;
import ma.ump.deptinfo.backend.exceptions.BadRequestException;
import ma.ump.deptinfo.backend.exceptions.ConflictException;
import ma.ump.deptinfo.backend.exceptions.ResourceNotFoundException;
import ma.ump.deptinfo.backend.repositories.AbsenceRepository;
import ma.ump.deptinfo.backend.repositories.ModuleCoursRepository;
import ma.ump.deptinfo.backend.repositories.StudentRepository;
import ma.ump.deptinfo.backend.repositories.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class AbsenceService {

    private final AbsenceRepository absenceRepository;
    private final StudentRepository studentRepository;
    private final ModuleCoursRepository moduleCoursRepository;
    private final UserRepository userRepository;
    private final GradeService gradeService;
    private final EnrollmentEligibilityValidator enrollmentValidator;

    public AbsenceService(
            AbsenceRepository absenceRepository,
            StudentRepository studentRepository,
            ModuleCoursRepository moduleCoursRepository,
            UserRepository userRepository,
            GradeService gradeService,
            EnrollmentEligibilityValidator enrollmentValidator) {
        this.absenceRepository = absenceRepository;
        this.studentRepository = studentRepository;
        this.moduleCoursRepository = moduleCoursRepository;
        this.userRepository = userRepository;
        this.gradeService = gradeService;
        this.enrollmentValidator = enrollmentValidator;
    }

    public AbsenceResponseDTO create(
            AbsenceRequestDTO dto,
            String authenticatedEmail) {

        if (dto == null || dto.studentId() == null
                || dto.moduleId() == null || dto.date() == null
                || dto.sessionTime() == null
                || dto.sessionTime().isBlank()
                || dto.sessionTime().length() > 50
                || dto.justified() == null) {
            throw new BadRequestException("Donnees d'absence incompletes");
        }

        String sessionTime = dto.sessionTime().trim();

        if (dto.motif() != null && dto.motif().length() > 500) {
            throw new BadRequestException("Motif trop long");
        }

        if (Boolean.TRUE.equals(dto.justified())
                && (dto.motif() == null || dto.motif().isBlank())) {
            throw new BadRequestException(
                    "Un motif est obligatoire pour justifier une absence");
        }

        gradeService.verifyTeacherAccess(dto.moduleId(), authenticatedEmail);

        ModuleCours module = moduleCoursRepository.findById(dto.moduleId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Module introuvable"));

        enrollmentValidator.validate(dto.studentId(), module, dto.date());

        if (absenceRepository
                .findByStudentIdAndModuleIdAndDateAndSessionTime(
                        dto.studentId(), dto.moduleId(),
                        dto.date(), sessionTime)
                .isPresent()) {
            throw new ConflictException(
                    "Une absence existe deja pour cette seance");
        }

        Student student = studentRepository.findById(dto.studentId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Etudiant introuvable"));

        Absence absence = new Absence();
        absence.setStudent(student);
        absence.setModule(module);
        absence.setDate(dto.date());
        absence.setSessionTime(sessionTime);
        absence.setJustified(dto.justified());
        absence.setMotif(dto.motif());

        return toResponse(absenceRepository.save(absence));
    }

    public AbsenceResponseDTO justify(
            Long absenceId,
            AbsenceJustificationDTO dto,
            String authenticatedEmail) {

        if (absenceId == null || dto == null
                || dto.motif() == null || dto.motif().isBlank()
                || dto.motif().length() > 500) {
            throw new BadRequestException("Motif de justification invalide");
        }

        Absence absence = absenceRepository.findById(absenceId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Absence introuvable"));

        gradeService.verifyTeacherAccess(
                absence.getModule().getId(), authenticatedEmail);

        if (Boolean.TRUE.equals(absence.getJustified())) {
            throw new ConflictException("Absence deja justifiee");
        }

        absence.setJustified(true);
        absence.setMotif(dto.motif().trim());

        return toResponse(absenceRepository.save(absence));
    }

    @Transactional(readOnly = true)
    public List<AbsenceResponseDTO> getMyAbsences(
            String authenticatedEmail) {

        if (authenticatedEmail == null || authenticatedEmail.isBlank()) {
            throw new AccessDeniedException("Authentification requise");
        }

        User user = userRepository.findByEmail(authenticatedEmail)
                .orElseThrow(() ->
                        new AccessDeniedException("Utilisateur non autorise"));

        if (user.getRole() != Role.ETUDIANT
                || !Boolean.TRUE.equals(user.getEnabled())
                || user.getDeletedAt() != null) {
            throw new AccessDeniedException(
                    "Compte etudiant non autorise");
        }

        return absenceRepository.findByStudent_User_Email(
                        user.getEmail())
                .stream()
                .map(this::toResponse)
                .toList();
    }


    private AbsenceResponseDTO toResponse(Absence absence) {
        return new AbsenceResponseDTO(
                absence.getId(),
                absence.getStudent().getId(),
                absence.getModule().getId(),
                absence.getDate(),
                absence.getSessionTime(),
                absence.getJustified(),
                absence.getMotif()
        );
    }
}