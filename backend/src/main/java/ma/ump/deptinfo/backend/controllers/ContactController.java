package ma.ump.deptinfo.backend.controllers;

import jakarta.validation.Valid;
import ma.ump.deptinfo.backend.dtos.ContactMessageDTO;
import ma.ump.deptinfo.backend.dtos.ContactMessageResponseDTO;
import ma.ump.deptinfo.backend.entities.MessageStatus;
import ma.ump.deptinfo.backend.services.ContactMessageService;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/contact")
public class ContactController {

    private final ContactMessageService contactMessageService;

    public ContactController(ContactMessageService contactMessageService) {
        this.contactMessageService = contactMessageService;
    }

    // POST /api/contact : public pour tous les visiteurs
    @PostMapping
    public ResponseEntity<ContactMessageResponseDTO> submitMessage(@Valid @RequestBody ContactMessageDTO dto) {
        ContactMessageResponseDTO response = contactMessageService.saveMessage(dto);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // GET /api/contact : consultation réservée à l'administration
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_DEPARTEMENT')")
    public ResponseEntity<Page<ContactMessageResponseDTO>> getMessages(
            @RequestParam(required = false) MessageStatus status,
            @ParameterObject
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(contactMessageService.getMessages(status, pageable));
    }

    // PUT /api/contact/{id}/read : marquer comme lu
    @PutMapping("/{id}/read")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_DEPARTEMENT')")
    public ResponseEntity<ContactMessageResponseDTO> markAsRead(@PathVariable Long id) {
        return ResponseEntity.ok(contactMessageService.markAsRead(id));
    }
}
