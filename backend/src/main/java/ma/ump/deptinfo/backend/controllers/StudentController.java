package ma.ump.deptinfo.backend.controllers;

import jakarta.validation.Valid;
import ma.ump.deptinfo.backend.dtos.StudentCreateDTO;
import ma.ump.deptinfo.backend.dtos.StudentResponseDTO;
import ma.ump.deptinfo.backend.dtos.StudentUpdateDTO;
import ma.ump.deptinfo.backend.services.StudentService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springdoc.core.annotations.ParameterObject;

@RestController
@RequestMapping("/api/students")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    // GET /api/students?search=ali&page=0&size=10&sort=studentNumber,asc
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_DEPARTEMENT')")
    public ResponseEntity<PagedModel<StudentResponseDTO>> getStudents(
                        @RequestParam(required = false) String search,
            @ParameterObject
            @PageableDefault(size = 10, sort = "studentNumber", direction = Sort.Direction.ASC)
            Pageable pageable) {
        return ResponseEntity.ok(new PagedModel<>(studentService.getStudents(search, pageable)));
    }

    // GET /api/students/{id}
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_DEPARTEMENT')")
    public ResponseEntity<StudentResponseDTO> getStudentById(@PathVariable Long id) {
        return ResponseEntity.ok(studentService.getStudentById(id));
    }

    // POST /api/students (crée aussi le compte utilisateur)
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_DEPARTEMENT')")
    public ResponseEntity<StudentResponseDTO> createStudent(
            @Valid @RequestBody StudentCreateDTO dto) {
        return new ResponseEntity<>(studentService.createStudent(dto), HttpStatus.CREATED);
    }

    // PUT /api/students/{id}
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_DEPARTEMENT')")
    public ResponseEntity<StudentResponseDTO> updateStudent(
            @PathVariable Long id,
            @Valid @RequestBody StudentUpdateDTO dto) {
        return ResponseEntity.ok(studentService.updateStudent(id, dto));
    }

    // DELETE /api/students/{id}
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_DEPARTEMENT')")
    public ResponseEntity<Void> deleteStudent(@PathVariable Long id) {
        studentService.deleteStudent(id);
        return ResponseEntity.noContent().build();
    }
}
