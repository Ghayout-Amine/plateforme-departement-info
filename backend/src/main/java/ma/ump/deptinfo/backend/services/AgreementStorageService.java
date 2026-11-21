package ma.ump.deptinfo.backend.services;

import ma.ump.deptinfo.backend.exceptions.BadRequestException;
import ma.ump.deptinfo.backend.exceptions.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

/**
 * Stockage des conventions de stage (PDF) sur disque, hors base de données (PGDI2026-80).
 * Même logique de dossier que les photos : variable CONVENTIONS_DIR, sinon dossier de l'utilisateur.
 * Le nom stocké est un UUID généré par le serveur : le nom du fichier envoyé n'est jamais utilisé.
 */
@Service
public class AgreementStorageService {

    static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final byte[] PDF_MAGIC = "%PDF-".getBytes(StandardCharsets.US_ASCII);

    /** Vérifie que le fichier est un vrai PDF de moins de 5 Mo, puis l'enregistre. Retourne le nom stocké. */
    public String store(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("La convention (PDF) est obligatoire");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new BadRequestException("La convention dépasse 5 Mo");
        }
        byte[] bytes = file.getBytes();
        if (!isPdf(bytes)) {
            throw new BadRequestException("La convention doit être un fichier PDF");
        }
        String filename = UUID.randomUUID() + ".pdf";
        Files.write(directory().resolve(filename), bytes, StandardOpenOption.CREATE_NEW);
        return filename;
    }

    /** Lit une convention déjà enregistrée. */
    public byte[] load(String filename) throws IOException {
        // Défense en profondeur : seuls nos propres noms (UUID.pdf) sont acceptés
        if (filename == null || !filename.matches("[0-9a-f\\-]{36}\\.pdf")) {
            throw new ResourceNotFoundException("Convention introuvable");
        }
        Path target = directory().resolve(filename);
        if (!Files.isRegularFile(target)) {
            throw new ResourceNotFoundException("Convention introuvable");
        }
        return Files.readAllBytes(target);
    }

    /** Supprime un fichier orphelin (ex. échec d'enregistrement en base). */
    public void deleteQuietly(String filename) {
        try {
            if (filename != null && filename.matches("[0-9a-f\\-]{36}\\.pdf")) {
                Files.deleteIfExists(directory().resolve(filename));
            }
        } catch (IOException ignored) {
            // Nettoyage au mieux : un fichier orphelin n'est pas bloquant
        }
    }

    static boolean isPdf(byte[] bytes) {
        if (bytes == null || bytes.length < PDF_MAGIC.length) {
            return false;
        }
        for (int i = 0; i < PDF_MAGIC.length; i++) {
            if (bytes[i] != PDF_MAGIC[i]) {
                return false;
            }
        }
        return true;
    }

    private Path directory() throws IOException {
        String custom = System.getenv("CONVENTIONS_DIR");
        Path directory;
        if (custom != null && !custom.isBlank()) {
            directory = Path.of(custom);
        } else {
            String localAppData = System.getenv("LOCALAPPDATA");
            if (localAppData != null && !localAppData.isBlank()) {
                directory = Path.of(localAppData, "PGDI", "conventions");
            } else {
                directory = Path.of(System.getProperty("user.home", "/app"), ".pgdi", "conventions");
            }
        }
        Files.createDirectories(directory);
        return directory;
    }
}
