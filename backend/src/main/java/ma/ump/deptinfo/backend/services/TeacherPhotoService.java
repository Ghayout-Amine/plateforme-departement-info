package ma.ump.deptinfo.backend.services;

import ma.ump.deptinfo.backend.entities.Role;
import ma.ump.deptinfo.backend.entities.Teacher;
import ma.ump.deptinfo.backend.entities.User;
import ma.ump.deptinfo.backend.exceptions.ResourceNotFoundException;
import ma.ump.deptinfo.backend.repositories.TeacherRepository;
import ma.ump.deptinfo.backend.repositories.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import java.util.UUID;

@Service
public class TeacherPhotoService {

    private static final long MAX_BYTES = 2L * 1024 * 1024;

    private final TeacherRepository teacherRepository;
    private final UserRepository userRepository;

    public TeacherPhotoService(
            TeacherRepository teacherRepository,
            UserRepository userRepository) {
        this.teacherRepository = teacherRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public String upload(
            Long teacherId,
            MultipartFile file,
            String authenticatedEmail) throws IOException {

        if (authenticatedEmail == null || authenticatedEmail.isBlank()) {
            throw new AccessDeniedException("Authentification requise");
        }

        User actor = userRepository.findByEmail(authenticatedEmail)
                .orElseThrow(() -> new AccessDeniedException(
                        "Utilisateur non autorise"));

        if (!Boolean.TRUE.equals(actor.getEnabled())
                || actor.getDeletedAt() != null) {
            throw new AccessDeniedException("Compte inactif");
        }

        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Enseignant introuvable"));

        User owner = teacher.getUser();

        boolean manager = actor.getRole() == Role.ADMIN
                || actor.getRole() == Role.CHEF_DEPARTEMENT;

        boolean self = actor.getRole() == Role.ENSEIGNANT
                && actor.getId().equals(owner.getId());

        if (!manager && !self) {
            throw new AccessDeniedException("Modification non autorisee");
        }

        if (file == null || file.isEmpty()
                || file.getSize() > MAX_BYTES) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Image invalide ou trop volumineuse");
        }

        byte[] bytes = file.getBytes();
        if (bytes.length > MAX_BYTES) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Image trop volumineuse");
        }

        String format;
        BufferedImage image;

        try (ImageInputStream input = ImageIO.createImageInputStream(
                new java.io.ByteArrayInputStream(bytes))) {

            if (input == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Image invalide");
            }

            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);

            if (!readers.hasNext()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Format image non reconnu");
            }

            ImageReader reader = readers.next();
            try {
                reader.setInput(input);
                format = reader.getFormatName().toLowerCase(
                        java.util.Locale.ROOT);

                if (!format.equals("jpeg") && !format.equals("jpg")
                        && !format.equals("png")) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST, "JPEG ou PNG uniquement");
                }

                int width = reader.getWidth(0);
                int height = reader.getHeight(0);

                if (width <= 0 || height <= 0
                        || width > 4096 || height > 4096
                        || (long) width * height > 12000000L) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Dimensions image non autorisees");
                }

                image = reader.read(0);
            } finally {
                reader.dispose();
            }
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (IOException | RuntimeException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Image illisible");
        }

        boolean png = format.equals("png");
        String extension = png ? ".png" : ".jpg";
        String outputFormat = png ? "png" : "jpg";

        BufferedImage sanitized = new BufferedImage(
                image.getWidth(), image.getHeight(),
                png ? BufferedImage.TYPE_INT_ARGB
                    : BufferedImage.TYPE_INT_RGB);

        Graphics2D graphics = sanitized.createGraphics();
        try {
            graphics.drawImage(image, 0, 0, null);
        } finally {
            graphics.dispose();
        }

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        if (!ImageIO.write(sanitized, outputFormat, output)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Encodage image impossible");
        }

        String filename = UUID.randomUUID() + extension;

        Path directory = getStorageDirectory();
        Path target = directory.resolve(filename);

        Files.write(target, output.toByteArray(),
                StandardOpenOption.CREATE_NEW);

        String avatarUrl = "/api/teachers/"
                + teacherId + "/photo";

        try {
            owner.setAvatarUrl(avatarUrl + "?v=" + filename);
            userRepository.save(owner);
        } catch (RuntimeException ex) {
            Files.deleteIfExists(target);
            throw ex;
        }

        return owner.getAvatarUrl();
    }

    public record PhotoData(
            byte[] bytes,
            org.springframework.http.MediaType contentType) {
    }

    @Transactional(readOnly = true)
    public PhotoData getPhoto(
            Long teacherId,
            String authenticatedEmail) throws IOException {

        if (authenticatedEmail == null || authenticatedEmail.isBlank()) {
            throw new AccessDeniedException("Authentification requise");
        }

        User actor = userRepository.findByEmail(authenticatedEmail)
                .orElseThrow(() ->
                        new AccessDeniedException("Utilisateur non autorise"));

        if (!Boolean.TRUE.equals(actor.getEnabled())
                || actor.getDeletedAt() != null) {
            throw new AccessDeniedException("Compte inactif");
        }

        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Enseignant introuvable"));

        User owner = teacher.getUser();

        boolean manager = actor.getRole() == Role.ADMIN
                || actor.getRole() == Role.CHEF_DEPARTEMENT;

        boolean self = actor.getRole() == Role.ENSEIGNANT
                && actor.getId().equals(owner.getId());

        if (!manager && !self) {
            throw new AccessDeniedException("Consultation non autorisee");
        }

        String avatarUrl = owner.getAvatarUrl();
        String prefix = "/api/teachers/" + teacherId + "/photo?v=";

        if (avatarUrl == null || !avatarUrl.startsWith(prefix)) {
            throw new ResourceNotFoundException("Photo introuvable");
        }

        String filename = avatarUrl.substring(prefix.length());

        if (!filename.matches(
                "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-"
              + "[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\\.(jpg|png)")) {
            throw new ResourceNotFoundException("Photo introuvable");
        }

        Path directory = getStorageDirectory();
        Path target = directory.resolve(filename);

        if (!Files.isRegularFile(target)) {
            throw new ResourceNotFoundException("Photo introuvable");
        }

        org.springframework.http.MediaType type =
                filename.endsWith(".png")
                ? org.springframework.http.MediaType.IMAGE_PNG
                : org.springframework.http.MediaType.IMAGE_JPEG;

        return new PhotoData(Files.readAllBytes(target), type);
    }

    private Path getStorageDirectory() throws IOException {
        String customPath = System.getenv("TEACHER_PHOTOS_DIR");
        Path directory;
        if (customPath != null && !customPath.isBlank()) {
            directory = Path.of(customPath);
        } else {
            String localAppData = System.getenv("LOCALAPPDATA");
            if (localAppData != null && !localAppData.isBlank()) {
                directory = Path.of(localAppData, "PGDI", "teacher-photos");
            } else {
                String userHome = System.getProperty("user.home", "/app");
                directory = Path.of(userHome, ".pgdi", "teacher-photos");
            }
        }
        Files.createDirectories(directory);
        return directory;
    }
}