package ma.ump.deptinfo.backend.controllers;

import jakarta.validation.Valid;
import ma.ump.deptinfo.backend.dtos.FiliereRequestDTO;
import ma.ump.deptinfo.backend.dtos.FiliereResponseDTO;
import ma.ump.deptinfo.backend.services.FiliereService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/filieres")
@CrossOrigin(origins = "*")
public class FiliereController {

    private final FiliereService filiereService;

    public FiliereController(FiliereService filiereService) {
        this.filiereService = filiereService;
    }

    @GetMapping
    public ResponseEntity<List<FiliereResponseDTO>> getAllFilieres() {
        return ResponseEntity.ok(filiereService.getAllFilieres());
    }

    @GetMapping("/{id}")
    public ResponseEntity<FiliereResponseDTO> getFiliereById(@PathVariable Long id) {
        return ResponseEntity.ok(filiereService.getFiliereById(id));
    }

    @PostMapping
    public ResponseEntity<FiliereResponseDTO> createFiliere(@Valid @RequestBody FiliereRequestDTO dto) {
        FiliereResponseDTO created = filiereService.createFiliere(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<FiliereResponseDTO> updateFiliere(@PathVariable Long id,
                                                            @Valid @RequestBody FiliereRequestDTO dto) {
        return ResponseEntity.ok(filiereService.updateFiliere(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFiliere(@PathVariable Long id) {
        filiereService.deleteFiliere(id);
        return ResponseEntity.noContent().build();
    }
}