package ma.ump.deptinfo.backend.services;

import ma.ump.deptinfo.backend.entities.*;
import ma.ump.deptinfo.backend.exceptions.BadRequestException;
import ma.ump.deptinfo.backend.exceptions.ResourceNotFoundException;
import ma.ump.deptinfo.backend.repositories.AcademicYearRepository;
import ma.ump.deptinfo.backend.repositories.EnrollmentRepository;
import ma.ump.deptinfo.backend.repositories.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Service
public class EnrollmentEligibilityValidator {

    private final StudentRepository studentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AcademicYearRepository academicYearRepository;

    public EnrollmentEligibilityValidator(
            StudentRepository studentRepository,
            EnrollmentRepository enrollmentRepository,
            AcademicYearRepository academicYearRepository) {
        this.studentRepository = studentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.academicYearRepository = academicYearRepository;
    }

    @Transactional(readOnly = true)
    public void validate(Long studentId, ModuleCours module, LocalDate date) {

        if (studentId == null || module == null
                || module.getFiliere() == null || date == null) {
            throw new BadRequestException("Donnees pedagogiques incompletes");
        }

        if (!studentRepository.existsById(studentId)) {
            throw new ResourceNotFoundException(
                    "Etudiant introuvable avec l'id : " + studentId);
        }

        List<AcademicYear> matchingYears = academicYearRepository
                .findAllByOrderByStartDateDesc()
                .stream()
                .filter(year -> !date.isBefore(year.getStartDate())
                        && !date.isAfter(year.getEndDate()))
                .toList();

        if (matchingYears.size() != 1) {
            throw new BadRequestException(
                    "Aucune annee universitaire unique pour cette date");
        }

        AcademicYear academicYear = matchingYears.get(0);

        Enrollment enrollment = enrollmentRepository
                .findByStudentIdAndAcademicYearId(
                        studentId, academicYear.getId())
                .orElseThrow(() -> new BadRequestException(
                        "Etudiant non inscrit pour cette annee universitaire"));

        if (enrollment.getStatus() != EnrollmentStatus.ACTIVE) {
            throw new BadRequestException(
                    "L'inscription pedagogique n'est pas active");
        }

        if (!Objects.equals(
                enrollment.getFiliere().getId(),
                module.getFiliere().getId())) {
            throw new BadRequestException(
                    "L'etudiant n'appartient pas a la filiere du module");
        }
    }

    @Transactional(readOnly = true)
    public void validateForAcademicYear(
            Long studentId,
            ModuleCours module,
            Long academicYearId) {

        if (studentId == null || academicYearId == null
                || module == null || module.getFiliere() == null) {
            throw new BadRequestException(
                    "Donnees pedagogiques incompletes");
        }

        if (!studentRepository.existsById(studentId)) {
            throw new ResourceNotFoundException(
                    "Etudiant introuvable avec l'id : " + studentId);
        }

        if (!academicYearRepository.existsById(academicYearId)) {
            throw new ResourceNotFoundException(
                    "Annee universitaire introuvable");
        }

        Enrollment enrollment = enrollmentRepository
                .findByStudentIdAndAcademicYearId(
                        studentId, academicYearId)
                .orElseThrow(() -> new BadRequestException(
                        "Etudiant non inscrit pour cette annee"));

        if (enrollment.getStatus() != EnrollmentStatus.ACTIVE) {
            throw new BadRequestException(
                    "Inscription pedagogique non active");
        }

        if (enrollment.getFiliere() == null
                || !Objects.equals(
                        enrollment.getFiliere().getId(),
                        module.getFiliere().getId())) {
            throw new BadRequestException(
                    "Filiere de l'etudiant incompatible avec le module");
        }
    }
}