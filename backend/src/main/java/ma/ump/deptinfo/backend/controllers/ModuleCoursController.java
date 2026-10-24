package ma.ump.deptinfo.backend.controllers;

import jakarta.validation.Valid;
import ma.ump.deptinfo.backend.dtos.ModuleCoursRequestDTO;
import ma.ump.deptinfo.backend.dtos.ModuleCoursResponseDTO;
import ma.ump.deptinfo.backend.services.ModuleCoursService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/modules")
@CrossOrigin(origins = "*")
public class ModuleCoursController {

    private final ModuleCoursService moduleCoursService;

    public ModuleCoursController(ModuleCoursService moduleCoursService) {
        this.moduleCoursService = moduleCoursService;
    }

    @GetMapping
    public ResponseEntity<List<ModuleCoursResponseDTO>> getAllModules() {
        return ResponseEntity.ok(moduleCoursService.getAllModules());
    }

    @GetMapping("/filiere/{filiereId}")
    public ResponseEntity<List<ModuleCoursResponseDTO>> getModulesByFiliere(@PathVariable Long filiereId) {
        return ResponseEntity.ok(moduleCoursService.getModulesByFiliereId(filiereId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ModuleCoursResponseDTO> getModuleById(@PathVariable Long id) {
        return ResponseEntity.ok(moduleCoursService.getModuleById(id));
    }

    @PostMapping
    public ResponseEntity<ModuleCoursResponseDTO> createModule(@Valid @RequestBody ModuleCoursRequestDTO dto) {
        ModuleCoursResponseDTO created = moduleCoursService.createModule(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ModuleCoursResponseDTO> updateModule(@PathVariable Long id,
                                                               @Valid @RequestBody ModuleCoursRequestDTO dto) {
        return ResponseEntity.ok(moduleCoursService.updateModule(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteModule(@PathVariable Long id) {
        moduleCoursService.deleteModule(id);
        return ResponseEntity.noContent().build();
    }
}