package ma.ump.deptinfo.backend.services;

import ma.ump.deptinfo.backend.dtos.InternshipRequestDTO;
import ma.ump.deptinfo.backend.dtos.InternshipResponseDTO;
import ma.ump.deptinfo.backend.dtos.InternshipValidationDTO;
import ma.ump.deptinfo.backend.entities.*;
import ma.ump.deptinfo.backend.exceptions.BadRequestException;
import ma.ump.deptinfo.backend.exceptions.ConflictException;
import ma.ump.deptinfo.backend.exceptions.ResourceNotFoundException;
import ma.ump.deptinfo.backend.repositories.InternshipRepository;
import ma.ump.deptinfo.backend.repositories.StudentRepository;
import ma.ump.deptinfo.backend.repositories.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Déclaration et validation des stages/PFE (PGDI2026-80, US66/US21).
 * L'étudiant déclare son stage avec sa convention PDF ; le département (ADMIN, CHEF_DEPARTEMENT)
 * valide ou refuse. Un étudiant n'a qu'un dossier actif par type (stage ou PFE).
 */
@Service
@Transactional
public class InternshipService {

    private static final Set<InternshipStatus> ACTIVE_STATUSES = EnumSet.of(
            InternshipStatus.PENDING_VALIDATION, InternshipStatus.VALIDATED, InternshipStatus.IN_PROGRESS);

    private final InternshipRepository internshipRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final AgreementStorageService storage;

    public InternshipService(InternshipRepository internshipRepository,
                             StudentRepository studentRepository,
                             UserRepository userRepository,
                             AgreementStorageService storage) {
        this.internshipRepository = internshipRepository;
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.storage = storage;
    }

    // ---------- étudiant ----------

    public InternshipResponseDTO declare(String email, InternshipRequestDTO dto, MultipartFile file) throws IOException {
        Student student = studentRepository.findByUserEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Aucun profil étudiant pour le compte : " + email));

        if (!dto.getEndDate().isAfter(dto.getStartDate())) {
            throw new BadRequestException("La date de fin doit être après la date de début");
        }
        if (internshipRepository.existsByStudentIdAndInternshipTypeAndStatusIn(
                student.getId(), dto.getInternshipType(), ACTIVE_STATUSES)) {
            throw new ConflictException("Vous avez déjà un dossier "
                    + (dto.getInternshipType() == InternshipType.PFE ? "PFE" : "de stage") + " en cours");
        }

        String filename = storage.store(file);
        try {
            Internship internship = new Internship();
            internship.setStudent(student);
            internship.setSubject(dto.getSubject().trim());
            internship.setInternshipType(dto.getInternshipType());
            internship.setCompanyName(dto.getCompanyName().trim());
            internship.setCompanyCity(blankToNull(dto.getCompanyCity()));
            internship.setCompanyCountry(blankToNull(dto.getCompanyCountry()) == null
                    ? null : dto.getCompanyCountry().trim().toUpperCase());
            internship.setStartDate(dto.getStartDate());
            internship.setEndDate(dto.getEndDate());
            internship.setCompanyTutorName(blankToNull(dto.getCompanyTutorName()));
            internship.setCompanyTutorEmail(blankToNull(dto.getCompanyTutorEmail()));
            internship.setCompanyTutorPhone(blankToNull(dto.getCompanyTutorPhone()));
            internship.setAgreementFilePath(filename);
            return toResponseDTO(internshipRepository.save(internship));
        } catch (RuntimeException ex) {
            storage.deleteQuietly(filename);
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public List<InternshipResponseDTO> getMyInternships(String email) {
        Student student = studentRepository.findByUserEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Aucun profil étudiant pour le compte : " + email));
        return internshipRepository.findByStudentIdOrderByDeclaredAtDesc(student.getId())
                .stream().map(this::toResponseDTO).toList();
    }

    // ---------- département ----------

    @Transactional(readOnly = true)
    public List<InternshipResponseDTO> getInternships(InternshipStatus status) {
        List<Internship> found = status == null
                ? internshipRepository.findAllByOrderByDeclaredAtDesc()
                : internshipRepository.findByStatusOrderByDeclaredAtDesc(status);
        return found.stream().map(this::toResponseDTO).toList();
    }

    public InternshipResponseDTO validate(Long id, InternshipValidationDTO dto, String email) {
        Internship internship = findInternship(id);
        if (internship.getStatus() != InternshipStatus.PENDING_VALIDATION) {
            throw new ConflictException("Ce dossier a déjà été traité (statut : " + internship.getStatus() + ")");
        }
        String comment = blankToNull(dto.getComment());
        if (!dto.getApproved() && comment == null) {
            throw new BadRequestException("Un motif est obligatoire pour refuser une convention");
        }
        User actor = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable : " + email));
        internship.decide(dto.getApproved(), comment, actor);
        return toResponseDTO(internshipRepository.save(internship));
    }

    // ---------- lecture commune ----------

    @Transactional(readOnly = true)
    public InternshipResponseDTO getInternshipById(Long id, String email) {
        Internship internship = findInternship(id);
        checkAccess(internship, email);
        return toResponseDTO(internship);
    }

    /** Contenu PDF de la convention : réservé au propriétaire et au département. */
    @Transactional(readOnly = true)
    public byte[] getAgreement(Long id, String email) throws IOException {
        Internship internship = findInternship(id);
        checkAccess(internship, email);
        return storage.load(internship.getAgreementFilePath());
    }

    // ---------- outils ----------

    private void checkAccess(Internship internship, String email) {
        User actor = userRepository.findByEmail(email)
                .orElseThrow(() -> new AccessDeniedException("Utilisateur non autorisé"));
        boolean manager = actor.getRole() == Role.ADMIN || actor.getRole() == Role.CHEF_DEPARTEMENT;
        boolean owner = internship.getStudent().getUser().getId().equals(actor.getId());
        if (!manager && !owner) {
            throw new AccessDeniedException("Accès refusé à ce dossier de stage");
        }
    }

    private Internship findInternship(Long id) {
        return internshipRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Stage introuvable avec l'id : " + id));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private InternshipResponseDTO toResponseDTO(Internship i) {
        InternshipResponseDTO dto = new InternshipResponseDTO();
        dto.setId(i.getId());
        dto.setSubject(i.getSubject());
        dto.setInternshipType(i.getInternshipType());
        dto.setCompanyName(i.getCompanyName());
        dto.setCompanyCity(i.getCompanyCity());
        dto.setCompanyCountry(i.getCompanyCountry());
        dto.setStartDate(i.getStartDate());
        dto.setEndDate(i.getEndDate());
        dto.setStatus(i.getStatus());
        dto.setCompanyTutorName(i.getCompanyTutorName());
        dto.setCompanyTutorEmail(i.getCompanyTutorEmail());
        dto.setCompanyTutorPhone(i.getCompanyTutorPhone());
        dto.setDeclaredAt(i.getDeclaredAt());
        dto.setValidationComment(i.getValidationComment());
        dto.setValidatedAt(i.getValidatedAt());
        Student s = i.getStudent();
        dto.setStudentId(s.getId());
        dto.setStudentNumber(s.getStudentNumber());
        dto.setStudentName(s.getUser().getFirstName() + " " + s.getUser().getLastName());
        return dto;
    }
}
