package ma.ump.deptinfo.backend.controllers;

import ma.ump.deptinfo.backend.services.TeacherPhotoService;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/teachers")
public class TeacherPhotoController {

    private final TeacherPhotoService photoService;

    public TeacherPhotoController(TeacherPhotoService photoService) {
        this.photoService = photoService;
    }

    @PostMapping(
            value = "/{id}/photo",
            consumes = "multipart/form-data")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> upload(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            Authentication authentication) throws IOException {

        String avatarUrl = photoService.upload(
                id, file, authentication.getName());

        return ResponseEntity.status(HttpStatus.OK)
                .body(Map.of("avatarUrl", avatarUrl));
    }

    @GetMapping("/{id}/photo")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> getPhoto(
            @PathVariable Long id,
            Authentication authentication) throws IOException {

        TeacherPhotoService.PhotoData photo =
                photoService.getPhoto(
                        id, authentication.getName());

        return ResponseEntity.ok()
                .contentType(photo.contentType())
                .cacheControl(CacheControl.noStore())
                .header(
                        "X-Content-Type-Options",
                        "nosniff")
                .body(photo.bytes());
    }
}