
package ma.ump.deptinfo.backend.services;

import ma.ump.deptinfo.backend.dtos.AbsenceJustificationDTO;
import ma.ump.deptinfo.backend.dtos.AbsenceRequestDTO;
import ma.ump.deptinfo.backend.dtos.AbsenceResponseDTO;
import ma.ump.deptinfo.backend.entities.Absence;
import ma.ump.deptinfo.backend.entities.AbsenceStatus;
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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
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
    private final JustificatifStorageService justificatifStorageService;

    public AbsenceService(
            AbsenceRepository absenceRepository,
            StudentRepository studentRepository,
            ModuleCoursRepository moduleCoursRepository,
            UserRepository userRepository,
            GradeService gradeService,
            EnrollmentEligibilityValidator enrollmentValidator,
            JustificatifStorageService justificatifStorageService) {

        this.absenceRepository = absenceRepository;
        this.studentRepository = studentRepository;
        this.moduleCoursRepository = moduleCoursRepository;
        this.userRepository = userRepository;
        this.gradeService = gradeService;
        this.enrollmentValidator = enrollmentValidator;
        this.justificatifStorageService = justificatifStorageService;
    }

    // Creation d'une absence par un enseignant.

    public AbsenceResponseDTO create(
            AbsenceRequestDTO dto,
            String authenticatedEmail) {

        if (dto == null || dto.studentId() == null
                || dto.moduleId() == null || dto.date() == null
                || dto.sessionTime() == null
                || dto.sessionTime().isBlank()
                || dto.sessionTime().length() > 50
                || dto.justified() == null) {

            throw new BadRequestException(
                    "Donnees d'absence incompletes");
        }

        String sessionTime = dto.sessionTime().trim();

        if (dto.motif() != null
                && dto.motif().length() > 500) {

            throw new BadRequestException(
                    "Motif trop long");
        }

        if (Boolean.TRUE.equals(dto.justified())
                && (dto.motif() == null
                || dto.motif().isBlank())) {

            throw new BadRequestException(
                    "Un motif est obligatoire pour justifier une absence");
        }

        gradeService.verifyTeacherAccess(
                dto.moduleId(), authenticatedEmail);

        ModuleCours module = moduleCoursRepository
                .findById(dto.moduleId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Module introuvable"));

        enrollmentValidator.validate(
                dto.studentId(), module, dto.date());

        if (absenceRepository
                .findByStudentIdAndModuleIdAndDateAndSessionTime(
                        dto.studentId(),
                        dto.moduleId(),
                        dto.date(),
                        sessionTime)
                .isPresent()) {

            throw new ConflictException(
                    "Une absence existe deja pour cette seance");
        }

        Student student = studentRepository
                .findById(dto.studentId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Etudiant introuvable"));

        Absence absence = new Absence();

        absence.setStudent(student);
        absence.setModule(module);
        absence.setDate(dto.date());
        absence.setSessionTime(sessionTime);
        absence.setJustified(dto.justified());
        absence.setMotif(dto.motif());

        return toResponse(
                absenceRepository.save(absence));
    }

    // Justification par un enseignant autorise.

    public AbsenceResponseDTO justify(
            Long absenceId,
            AbsenceJustificationDTO dto,
            String authenticatedEmail) {

        if (absenceId == null || dto == null
                || dto.motif() == null
                || dto.motif().isBlank()
                || dto.motif().length() > 500) {

            throw new BadRequestException(
                    "Motif de justification invalide");
        }

        Absence absence = absenceRepository
                .findByIdForUpdate(absenceId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Absence introuvable"));

        gradeService.verifyTeacherAccess(
                absence.getModule().getId(),
                authenticatedEmail);

        if (Boolean.TRUE.equals(absence.getJustified())) {

            throw new ConflictException(
                    "Absence deja justifiee");
        }

        absence.setJustified(true);
        absence.setMotif(dto.motif().trim());

        return toResponse(
                absenceRepository.save(absence));
    }

    // PGDI2026-91 : depot du justificatif medical PDF.

    public AbsenceResponseDTO uploadJustificatif(
            Long absenceId,
            MultipartFile file,
            String authenticatedEmail) {

        if (absenceId == null || absenceId <= 0) {

            throw new BadRequestException(
                    "Identifiant d'absence invalide");
        }

        User user = requireActiveStudent(
                authenticatedEmail);

        Absence absence = absenceRepository
                .findByIdForUpdate(absenceId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Absence introuvable"));

        // Verification du proprietaire.

        Student owner = absence.getStudent();

        if (owner == null
                || owner.getUser() == null
                || user.getId() == null
                || !user.getId().equals(
                        owner.getUser().getId())) {

            throw new ResourceNotFoundException(
                    "Absence introuvable");
        }

        // Interdiction de modifier une absence justifiee.

        if (Boolean.TRUE.equals(absence.getJustified())
                || absence.getStatus()
                        == AbsenceStatus.JUSTIFIED) {

            throw new ConflictException(
                    "Cette absence est deja justifiee");
        }

        // Interdiction des depots multiples.

        if (absence.getJustificatifFilename() != null
                || absence.getStatus()
                        == AbsenceStatus.PENDING_JUSTIFICATION) {

            throw new ConflictException(
                    "Un justificatif est deja en attente de validation");
        }

        // Verification de la transaction.

        if (!TransactionSynchronizationManager
                .isActualTransactionActive()
                || !TransactionSynchronizationManager
                        .isSynchronizationActive()) {

            throw new IllegalStateException(
                    "Transaction de stockage non disponible");
        }

        // Validation et stockage du PDF.

        final String filename;

        try {

            filename = justificatifStorageService.store(file);

        } catch (IOException exception) {

            throw new UncheckedIOException(
                    "Impossible de stocker le justificatif",
                    exception);
        }

        // Suppression du fichier en cas de rollback SQL.

        try {

            TransactionSynchronizationManager
                    .registerSynchronization(
                            new TransactionSynchronization() {

                @Override
                public void afterCompletion(int status) {

                    if (status == STATUS_ROLLED_BACK) {

                        justificatifStorageService
                                .deleteQuietly(filename);
                    }
                }
            });

        } catch (RuntimeException exception) {

            justificatifStorageService
                    .deleteQuietly(filename);

            throw exception;
        }

        // Mise a jour du statut.

        absence.markPendingJustification(filename);

        return toResponse(
                absenceRepository.save(absence));
    }

    // Lecture du justificatif medical PDF (proprietaire, enseignant ou administration).

    @Transactional(readOnly = true)
    public byte[] getJustificatif(
            Long absenceId,
            String authenticatedEmail) throws IOException {

        if (absenceId == null || absenceId <= 0) {
            throw new BadRequestException("Identifiant d'absence invalide");
        }
        if (authenticatedEmail == null || authenticatedEmail.isBlank()) {
            throw new AccessDeniedException("Authentification requise");
        }

        User user = userRepository.findByEmail(authenticatedEmail)
                .orElseThrow(() -> new AccessDeniedException("Utilisateur non autorise"));

        Absence absence = absenceRepository.findById(absenceId)
                .orElseThrow(() -> new ResourceNotFoundException("Absence introuvable"));

        Role role = user.getRole();
        boolean isOwner = role == Role.ETUDIANT
                && absence.getStudent() != null
                && absence.getStudent().getUser() != null
                && user.getId().equals(absence.getStudent().getUser().getId());
        boolean isStaff = role == Role.ENSEIGNANT
                || role == Role.ADMIN
                || role == Role.CHEF_DEPARTEMENT;

        if (!isOwner && !isStaff) {
            throw new AccessDeniedException("Acces refuse");
        }

        return justificatifStorageService.load(
                absence.getJustificatifFilename());
    }

    // Consultation des absences de l'etudiant.

    @Transactional(readOnly = true)
    public List<AbsenceResponseDTO> getMyAbsences(
            String authenticatedEmail) {

        User user = requireActiveStudent(
                authenticatedEmail);

        return absenceRepository
                .findByStudent_User_Email(user.getEmail())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // Verification du compte etudiant.

    private User requireActiveStudent(
            String authenticatedEmail) {

        if (authenticatedEmail == null
                || authenticatedEmail.isBlank()) {

            throw new AccessDeniedException(
                    "Authentification requise");
        }

        User user = userRepository
                .findByEmail(authenticatedEmail)
                .orElseThrow(() ->
                        new AccessDeniedException(
                                "Utilisateur non autorise"));

        if (user.getRole() != Role.ETUDIANT
                || !Boolean.TRUE.equals(user.getEnabled())
                || user.getDeletedAt() != null) {

            throw new AccessDeniedException(
                    "Compte etudiant non autorise");
        }

        return user;
    }

    // Conversion vers le DTO.

    private AbsenceResponseDTO toResponse(
            Absence absence) {

        return new AbsenceResponseDTO(
                absence.getId(),
                absence.getStudent().getId(),
                absence.getModule().getId(),
                absence.getDate(),
                absence.getSessionTime(),
                absence.getJustified(),
                absence.getMotif(),
                absence.getStatus()
        );
    }
}
