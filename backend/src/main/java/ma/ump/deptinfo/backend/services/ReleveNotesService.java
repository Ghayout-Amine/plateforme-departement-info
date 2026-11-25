
package ma.ump.deptinfo.backend.services;

import ma.ump.deptinfo.backend.dtos.ReleveNotesResponseDTO;

import ma.ump.deptinfo.backend.entities.AcademicYear;
import ma.ump.deptinfo.backend.entities.Enrollment;
import ma.ump.deptinfo.backend.entities.Grade;
import ma.ump.deptinfo.backend.entities.GradeType;
import ma.ump.deptinfo.backend.entities.ModuleCours;
import ma.ump.deptinfo.backend.entities.Role;
import ma.ump.deptinfo.backend.entities.Student;
import ma.ump.deptinfo.backend.entities.User;

import ma.ump.deptinfo.backend.exceptions.BadRequestException;
import ma.ump.deptinfo.backend.exceptions.ResourceNotFoundException;

import ma.ump.deptinfo.backend.repositories.AcademicYearRepository;
import ma.ump.deptinfo.backend.repositories.EnrollmentRepository;
import ma.ump.deptinfo.backend.repositories.GradeRepository;
import ma.ump.deptinfo.backend.repositories.ModuleCoursRepository;
import ma.ump.deptinfo.backend.repositories.StudentRepository;
import ma.ump.deptinfo.backend.repositories.UserRepository;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class ReleveNotesService {

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final AcademicYearRepository academicYearRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ModuleCoursRepository moduleCoursRepository;
    private final GradeRepository gradeRepository;
    private final GradeAverageCalculator gradeAverageCalculator;

    public ReleveNotesService(
            StudentRepository studentRepository,
            UserRepository userRepository,
            AcademicYearRepository academicYearRepository,
            EnrollmentRepository enrollmentRepository,
            ModuleCoursRepository moduleCoursRepository,
            GradeRepository gradeRepository,
            GradeAverageCalculator gradeAverageCalculator) {

        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.academicYearRepository = academicYearRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.moduleCoursRepository = moduleCoursRepository;
        this.gradeRepository = gradeRepository;
        this.gradeAverageCalculator = gradeAverageCalculator;
    }

    // 1. Verification des droits d'acces
    public Student validateStudentAccess(
            Long studentId,
            String authenticatedEmail) {

        if (studentId == null || studentId <= 0) {
            throw new BadRequestException(
                    "Identifiant de l'etudiant invalide");
        }

        if (authenticatedEmail == null
                || authenticatedEmail.isBlank()) {
            throw new AccessDeniedException("Acces refuse");
        }

        User currentUser = userRepository
                .findByEmail(authenticatedEmail)
                .orElseThrow(() ->
                        new AccessDeniedException("Acces refuse"));

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Etudiant introuvable : " + studentId));

        Role role = currentUser.getRole();

        if (role == Role.ADMIN
                || role == Role.CHEF_DEPARTEMENT) {
            return student;
        }

        if (role == Role.ETUDIANT
                && student.getUser() != null
                && student.getUser().getId()
                        .equals(currentUser.getId())) {
            return student;
        }

        throw new AccessDeniedException("Acces refuse");
    }

    // 2. Recuperation de l'annee universitaire actuelle
    private AcademicYear getCurrentAcademicYear() {

        return academicYearRepository.findFirstByCurrentTrue()
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Aucune annee universitaire active"));
    }

    // 3. Recuperation de l'inscription de l'etudiant
    private Enrollment getStudentEnrollment(
            Long studentId,
            AcademicYear academicYear) {

        return enrollmentRepository
                .findByStudentIdAndAcademicYearId(
                        studentId,
                        academicYear.getId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Aucune inscription pour cet etudiant "
                                        + "dans l'annee universitaire actuelle"));
    }

    // 4. Recuperation des modules du semestre
    private List<ModuleCours> getSemesterModules(
            Enrollment enrollment,
            String semester) {

        if (semester == null || semester.isBlank()) {
            throw new BadRequestException(
                    "Le semestre est obligatoire");
        }

        String normalizedSemester =
                semester.trim().toUpperCase(Locale.ROOT);

        if (!normalizedSemester.matches("S[1-6]")) {
            throw new BadRequestException(
                    "Semestre invalide : " + semester);
        }

        if (enrollment.getFiliere() == null) {
            throw new BadRequestException(
                    "Aucune filiere associee a l'inscription");
        }

        List<ModuleCours> modules = moduleCoursRepository
                .findByFiliereIdAndSemestre(
                        enrollment.getFiliere().getId(),
                        normalizedSemester);

        if (modules.isEmpty()) {
            throw new ResourceNotFoundException(
                    "Aucun module trouve pour le semestre "
                            + normalizedSemester);
        }

        return modules;
    }

    // 5. Calcul de la moyenne d'un module
    private Optional<BigDecimal> calculateModuleAverage(
            Long studentId,
            ModuleCours module,
            AcademicYear academicYear) {

        List<Grade> grades = gradeRepository
                .findByStudentIdAndModuleIdAndSessionDateBetween(
                        studentId,
                        module.getId(),
                        academicYear.getStartDate(),
                        academicYear.getEndDate());

        // Les notes de rattrapage sont exclues provisoirement.
        List<Grade> regularGrades = grades.stream()
                .filter(grade -> grade != null
                        && grade.getType() != GradeType.RATTRAPAGE)
                .toList();

        return gradeAverageCalculator.calculate(regularGrades);
    }

    // 6. Calcul de la moyenne generale semestrielle
    private BigDecimal calculateSemesterAverage(
            List<BigDecimal> moduleAverages) {

        if (moduleAverages == null
                || moduleAverages.isEmpty()
                || moduleAverages.stream().anyMatch(
                        average -> average == null)) {
            throw new BadRequestException(
                    "Moyennes des modules incompletes");
        }

        BigDecimal sum = moduleAverages.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return sum.divide(
                BigDecimal.valueOf(moduleAverages.size()),
                2,
                RoundingMode.HALF_UP);
    }

    // 7. Decision du semestre (session normale)
    private String determineDecision(
            BigDecimal semesterAverage,
            List<BigDecimal> moduleAverages) {

        if (semesterAverage == null
                || moduleAverages == null
                || moduleAverages.isEmpty()
                || moduleAverages.stream().anyMatch(
                        average -> average == null)) {
            throw new BadRequestException(
                    "Resultats semestriels incomplets");
        }

        boolean hasEliminatoryModule = moduleAverages.stream()
                .anyMatch(average ->
                        average.compareTo(
                                BigDecimal.valueOf(5)) < 0);

        if (semesterAverage.compareTo(BigDecimal.TEN) >= 0
                && !hasEliminatoryModule) {
            return "ADMIS";
        }

        return "RATTRAPAGE";
    }

    // 8. Mention du semestre
    private String determineMention(
            BigDecimal semesterAverage,
            String decision) {

        if (!"ADMIS".equals(decision)) {
            return null;
        }

        if (semesterAverage.compareTo(
                BigDecimal.valueOf(16)) >= 0) {
            return "Tres Bien";
        }

        if (semesterAverage.compareTo(
                BigDecimal.valueOf(14)) >= 0) {
            return "Bien";
        }

        if (semesterAverage.compareTo(
                BigDecimal.valueOf(12)) >= 0) {
            return "Assez Bien";
        }

        return "Passable";
    }

    // 9. Generation du releve de notes semestriel
    @Transactional(readOnly = true)
    public ReleveNotesResponseDTO getReleveNotes(
            Long studentId,
            String semester,
            String authenticatedEmail) {

        Student student = validateStudentAccess(
                studentId, authenticatedEmail);

        AcademicYear academicYear = getCurrentAcademicYear();

        Enrollment enrollment = getStudentEnrollment(
                studentId, academicYear);

        List<ModuleCours> modules = getSemesterModules(
                enrollment, semester);

        List<ReleveNotesResponseDTO.ModuleResultDTO> results =
                new ArrayList<>();

        List<BigDecimal> moduleAverages = new ArrayList<>();

        for (ModuleCours module : modules) {

            BigDecimal average = calculateModuleAverage(
                    studentId, module, academicYear)
                    .orElseThrow(() ->
                            new BadRequestException(
                                    "Notes publiees manquantes pour le module : "
                                            + module.getCode()));

            moduleAverages.add(average);

            String status = average.compareTo(BigDecimal.TEN) >= 0
                    ? "VALIDE"
                    : "NON_VALIDE";

            results.add(
                    new ReleveNotesResponseDTO.ModuleResultDTO(
                            module.getCode(),
                            module.getNom(),
                            average,
                            status));
        }

        BigDecimal semesterAverage =
                calculateSemesterAverage(moduleAverages);

        String decision = determineDecision(
                semesterAverage, moduleAverages);

        String mention = determineMention(
                semesterAverage, decision);

        // Compensation des modules entre 5 et 10
        if ("ADMIS".equals(decision)) {

            List<ReleveNotesResponseDTO.ModuleResultDTO>
                    compensatedResults = new ArrayList<>();

            for (ReleveNotesResponseDTO.ModuleResultDTO result
                    : results) {

                String status = result.score()
                        .compareTo(BigDecimal.TEN) >= 0
                        ? "VALIDE"
                        : "VALIDE_PAR_COMPENSATION";

                compensatedResults.add(
                        new ReleveNotesResponseDTO.ModuleResultDTO(
                                result.code(),
                                result.name(),
                                result.score(),
                                status));
            }

            results = compensatedResults;
        }

        if (student.getUser() == null) {
            throw new BadRequestException(
                    "Aucun compte utilisateur associe a l'etudiant");
        }

        String studentName =
                student.getUser().getFirstName()
                        + " "
                        + student.getUser().getLastName();

        return new ReleveNotesResponseDTO(
                studentName,
                student.getStudentNumber(),
                semester.trim().toUpperCase(Locale.ROOT),
                results,
                semesterAverage,
                decision,
                mention);
    }
}
