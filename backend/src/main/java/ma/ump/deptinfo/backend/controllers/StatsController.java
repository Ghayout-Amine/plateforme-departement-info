package ma.ump.deptinfo.backend.controllers;

import ma.ump.deptinfo.backend.dtos.StatsDTO;
import ma.ump.deptinfo.backend.repositories.FiliereRepository;
import ma.ump.deptinfo.backend.repositories.ModuleCoursRepository;
import ma.ump.deptinfo.backend.repositories.StudentRepository;
import ma.ump.deptinfo.backend.repositories.TeacherRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * GET /api/stats
 * Retourne les chiffres réels lus depuis la base de données MySQL.
 * Accessible publiquement (sans connexion) pour la page d'accueil vitrine.
 */
@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final StudentRepository  studentRepository;
    private final TeacherRepository  teacherRepository;
    private final FiliereRepository  filiereRepository;
    private final ModuleCoursRepository moduleRepository;

    public StatsController(StudentRepository studentRepository,
                           TeacherRepository teacherRepository,
                           FiliereRepository filiereRepository,
                           ModuleCoursRepository moduleRepository) {
        this.studentRepository  = studentRepository;
        this.teacherRepository  = teacherRepository;
        this.filiereRepository  = filiereRepository;
        this.moduleRepository   = moduleRepository;
    }

    @GetMapping
    public ResponseEntity<StatsDTO> getStats() {
        long totalEtudiants   = studentRepository.count();
        long totalEnseignants = teacherRepository.count();
        long totalFilieres    = filiereRepository.count();
        long totalModules     = moduleRepository.count();

        return ResponseEntity.ok(
            new StatsDTO(totalEtudiants, totalEnseignants, totalFilieres, totalModules)
        );
    }
}
