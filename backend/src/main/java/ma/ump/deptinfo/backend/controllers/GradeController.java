package ma.ump.deptinfo.backend.controllers;

import jakarta.validation.Valid;
import ma.ump.deptinfo.backend.dtos.GradeAverageResponseDTO;
import ma.ump.deptinfo.backend.dtos.GradeCreateRequestDTO;
import ma.ump.deptinfo.backend.dtos.GradeResponseDTO;
import ma.ump.deptinfo.backend.services.GradeService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/grades")
public class GradeController {

    private final GradeService gradeService;

    public GradeController(GradeService gradeService) {
        this.gradeService = gradeService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ENSEIGNANT')")
    public GradeResponseDTO create(
            @Valid @RequestBody GradeCreateRequestDTO request,
            Authentication authentication) {

        return gradeService.createDraftGradeResponse(
                request, authentication.getName());
    }

    @PatchMapping("/{id}/publish")
    @PreAuthorize("hasRole('ENSEIGNANT')")
    public GradeResponseDTO publish(
            @PathVariable Long id,
            Authentication authentication) {

        return gradeService.publishGradeResponse(
                id, authentication.getName());
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('ETUDIANT')")
    public List<GradeResponseDTO> myGrades(
            Authentication authentication) {

        return gradeService.getMyPublishedGradeResponses(
                authentication.getName());
    }

    @GetMapping("/average")
    @PreAuthorize("hasRole('ENSEIGNANT')")
    public GradeAverageResponseDTO average(
            @RequestParam Long studentId,
            @RequestParam Long moduleId,
            @RequestParam Long academicYearId,
            Authentication authentication) {

        BigDecimal result = gradeService.calculateModuleAverage(
                studentId,
                moduleId,
                academicYearId,
                authentication.getName()
        ).orElse(null);

        return new GradeAverageResponseDTO(
                studentId, moduleId, academicYearId, result);
    }
}