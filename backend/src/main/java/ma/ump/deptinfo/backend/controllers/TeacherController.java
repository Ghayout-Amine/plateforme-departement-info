package ma.ump.deptinfo.backend.controllers;

import jakarta.validation.Valid;
import ma.ump.deptinfo.backend.dtos.TeacherRequestDTO;
import ma.ump.deptinfo.backend.dtos.TeacherResponseDTO;
import ma.ump.deptinfo.backend.services.TeacherService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teachers")
public class TeacherController {

    private final TeacherService teacherService;

    public TeacherController(TeacherService teacherService) {
        this.teacherService = teacherService;
    }

    // GET /api/teachers
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_DEPARTEMENT')")
    public ResponseEntity<List<TeacherResponseDTO>> getAllTeachers() {
        return ResponseEntity.ok(teacherService.getAllTeachers());
    }

    // GET /api/teachers/{id}
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_DEPARTEMENT')")
    public ResponseEntity<TeacherResponseDTO> getTeacherById(@PathVariable Long id) {
        return ResponseEntity.ok(teacherService.getTeacherById(id));
    }

    // POST /api/teachers
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_DEPARTEMENT')")
    public ResponseEntity<TeacherResponseDTO> createTeacher(
            @Valid @RequestBody TeacherRequestDTO dto) {
        TeacherResponseDTO created = teacherService.createTeacher(dto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    // PUT /api/teachers/{id}
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_DEPARTEMENT')")
    public ResponseEntity<TeacherResponseDTO> updateTeacher(
            @PathVariable Long id,
            @Valid @RequestBody TeacherRequestDTO dto) {
        return ResponseEntity.ok(teacherService.updateTeacher(id, dto));
    }

    // DELETE /api/teachers/{id}
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_DEPARTEMENT')")
    public ResponseEntity<Void> deleteTeacher(@PathVariable Long id) {
        teacherService.deleteTeacher(id);
        return ResponseEntity.noContent().build();
    }
}
