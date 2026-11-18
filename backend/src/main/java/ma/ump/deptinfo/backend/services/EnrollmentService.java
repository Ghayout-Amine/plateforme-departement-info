package ma.ump.deptinfo.backend.services;

import ma.ump.deptinfo.backend.dtos.EnrollmentRequestDTO;
import ma.ump.deptinfo.backend.dtos.EnrollmentResponseDTO;
import ma.ump.deptinfo.backend.entities.*;
import ma.ump.deptinfo.backend.exceptions.BadRequestException;
import ma.ump.deptinfo.backend.exceptions.ConflictException;
import ma.ump.deptinfo.backend.exceptions.ResourceNotFoundException;
import ma.ump.deptinfo.backend.repositories.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Inscription pédagogique des étudiants (PGDI2026-60, US26).
 * Règles :
 * - l'étudiant, la filière, l'année et le groupe doivent exister (404) ;
 * - un étudiant n'a qu'une inscription par année universitaire (409) ;
 * - le groupe doit appartenir à la filière de l'inscription (400) ;
 * - un groupe plein (effectif maximal atteint) refuse de nouveaux étudiants (409).
 */
@Service
@Transactional
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final FiliereRepository filiereRepository;
    private final AcademicYearRepository academicYearRepository;
    private final GroupeRepository groupeRepository;

    public EnrollmentService(EnrollmentRepository enrollmentRepository,
                             StudentRepository studentRepository,
                             FiliereRepository filiereRepository,
                             AcademicYearRepository academicYearRepository,
                             GroupeRepository groupeRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.studentRepository = studentRepository;
        this.filiereRepository = filiereRepository;
        this.academicYearRepository = academicYearRepository;
        this.groupeRepository = groupeRepository;
    }

    // Liste avec filtres facultatifs (année, filière, groupe)
    @Transactional(readOnly = true)
    public List<EnrollmentResponseDTO> getEnrollments(Long academicYearId, Long filiereId, Long groupeId) {
        return enrollmentRepository.search(academicYearId, filiereId, groupeId).stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public EnrollmentResponseDTO getEnrollmentById(Long id) {
        return toResponseDTO(findEnrollment(id));
    }

    // Historique des inscriptions d'un étudiant
    @Transactional(readOnly = true)
    public List<EnrollmentResponseDTO> getEnrollmentsByStudent(Long studentId) {
        findStudent(studentId); // 404 si l'étudiant n'existe pas
        return enrollmentRepository.findByStudentIdOrderByAcademicYearStartDateDesc(studentId).stream()
                .map(this::toResponseDTO)
                .toList();
    }

    // Inscrire un étudiant dans une filière pour une année (et éventuellement dans un groupe)
    public EnrollmentResponseDTO createEnrollment(EnrollmentRequestDTO dto) {
        Student student = findStudent(dto.getStudentId());
        Filiere filiere = findFiliere(dto.getFiliereId());
        AcademicYear year = findAcademicYear(dto.getAcademicYearId());

        if (enrollmentRepository.findByStudentIdAndAcademicYearId(student.getId(), year.getId()).isPresent()) {
            throw new ConflictException("L'étudiant " + student.getStudentNumber()
                    + " est déjà inscrit pour l'année " + year.getLabel());
        }

        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setFiliere(filiere);
        enrollment.setAcademicYear(year);
        enrollment.setGroupe(resolveGroupe(dto.getGroupeId(), filiere, null));
        if (dto.getStatus() != null) {
            enrollment.setStatus(dto.getStatus());
        }

        return toResponseDTO(enrollmentRepository.save(enrollment));
    }

    // Modifier une inscription : changer de filière, de groupe ou de statut
    public EnrollmentResponseDTO updateEnrollment(Long id, EnrollmentRequestDTO dto) {
        Enrollment enrollment = findEnrollment(id);
        Student student = findStudent(dto.getStudentId());
        Filiere filiere = findFiliere(dto.getFiliereId());
        AcademicYear year = findAcademicYear(dto.getAcademicYearId());

        enrollmentRepository.findByStudentIdAndAcademicYearId(student.getId(), year.getId()).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new ConflictException("L'étudiant " + student.getStudentNumber()
                        + " est déjà inscrit pour l'année " + year.getLabel());
            }
        });

        enrollment.setStudent(student);
        enrollment.setFiliere(filiere);
        enrollment.setAcademicYear(year);
        enrollment.setGroupe(resolveGroupe(dto.getGroupeId(), filiere, enrollment.getGroupe()));
        if (dto.getStatus() != null) {
            enrollment.setStatus(dto.getStatus());
        }

        return toResponseDTO(enrollmentRepository.save(enrollment));
    }

    public void deleteEnrollment(Long id) {
        enrollmentRepository.delete(findEnrollment(id));
    }

    // ---------- méthodes utilitaires ----------

    /**
     * Vérifie le groupe demandé : il doit exister, appartenir à la filière
     * et ne pas être plein. currentGroupe = groupe actuel de l'inscription
     * (on ne compte pas l'étudiant deux fois s'il reste dans le même groupe).
     */
    private Groupe resolveGroupe(Long groupeId, Filiere filiere, Groupe currentGroupe) {
        if (groupeId == null) {
            return null;
        }
        Groupe groupe = groupeRepository.findById(groupeId)
                .orElseThrow(() -> new ResourceNotFoundException("Groupe introuvable avec l'id : " + groupeId));

        if (!groupe.getFiliere().getId().equals(filiere.getId())) {
            throw new BadRequestException("Le groupe \"" + groupe.getName()
                    + "\" n'appartient pas à la filière " + filiere.getCode());
        }

        boolean sameGroupe = currentGroupe != null && currentGroupe.getId().equals(groupe.getId());
        if (!sameGroupe && groupe.getCapacity() != null
                && enrollmentRepository.countByGroupeId(groupe.getId()) >= groupe.getCapacity()) {
            throw new ConflictException("Le groupe \"" + groupe.getName()
                    + "\" est complet (" + groupe.getCapacity() + " étudiants maximum)");
        }
        return groupe;
    }

    private Enrollment findEnrollment(Long id) {
        return enrollmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inscription introuvable avec l'id : " + id));
    }

    private Student findStudent(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Étudiant introuvable avec l'id : " + id));
    }

    private Filiere findFiliere(Long id) {
        return filiereRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Filière introuvable avec l'id : " + id));
    }

    private AcademicYear findAcademicYear(Long id) {
        return academicYearRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Année universitaire introuvable avec l'id : " + id));
    }

    private EnrollmentResponseDTO toResponseDTO(Enrollment e) {
        Student student = e.getStudent();
        User user = student.getUser();
        Filiere filiere = e.getFiliere();
        AcademicYear year = e.getAcademicYear();
        Groupe groupe = e.getGroupe();
        return new EnrollmentResponseDTO(
                e.getId(),
                e.getStatus(),
                student.getId(),
                student.getStudentNumber(),
                user != null ? user.getFirstName() : null,
                user != null ? user.getLastName() : null,
                filiere.getId(),
                filiere.getCode(),
                filiere.getNom(),
                year.getId(),
                year.getLabel(),
                groupe != null ? groupe.getId() : null,
                groupe != null ? groupe.getName() : null,
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }
}
