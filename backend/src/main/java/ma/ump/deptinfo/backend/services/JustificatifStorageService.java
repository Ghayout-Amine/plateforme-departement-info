
package ma.ump.deptinfo.backend.services;

import ma.ump.deptinfo.backend.exceptions.BadRequestException;
import ma.ump.deptinfo.backend.exceptions.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

@Service
public class JustificatifStorageService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(
                    JustificatifStorageService.class);

    static final int MAX_BYTES = 5 * 1024 * 1024;

    private static final byte[] PDF_MAGIC =
            "%PDF-".getBytes(StandardCharsets.US_ASCII);

    private static final String UUID_PDF_PATTERN =
            "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-"
            + "[0-9a-f]{4}-[0-9a-f]{12}\\.pdf";

    public String store(MultipartFile file)
            throws IOException {

        // 1. Verification du fichier.

        if (file == null || file.isEmpty()) {

            throw new BadRequestException(
                    "Le justificatif medical est obligatoire");
        }

        // 2. Verification de la taille declaree.

        if (file.getSize() > MAX_BYTES) {

            throw new BadRequestException(
                    "Le justificatif ne doit pas depasser 5 Mo");
        }

        // 3. Lecture bornee du contenu reel.

        byte[] bytes;

        try (InputStream input = file.getInputStream()) {

            bytes = input.readNBytes(MAX_BYTES + 1);
        }

        if (bytes.length > MAX_BYTES) {

            throw new BadRequestException(
                    "Le justificatif ne doit pas depasser 5 Mo");
        }

        // 4. Verification de la signature binaire PDF.

        if (!isPdf(bytes)) {

            throw new BadRequestException(
                    "Le justificatif doit etre un fichier PDF");
        }

        // 5. Generation d'un nom securise.

        String filename = UUID.randomUUID() + ".pdf";

        Path target = directory().resolve(filename);

        // 6. Creation sans ecrasement d'un fichier existant.

        Files.createFile(target);

        try {

            Files.write(
                    target,
                    bytes,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );

        } catch (IOException | RuntimeException exception) {

            try {

                Files.deleteIfExists(target);

            } catch (IOException cleanupException) {

                exception.addSuppressed(cleanupException);
            }

            throw exception;
        }

        return filename;
    }

    // Verification de l'en-tete binaire %PDF-.

    static boolean isPdf(byte[] bytes) {

        if (bytes == null
                || bytes.length < PDF_MAGIC.length) {

            return false;
        }

        for (int i = 0; i < PDF_MAGIC.length; i++) {

            if (bytes[i] != PDF_MAGIC[i]) {

                return false;
            }
        }

        return true;
    }

    // Lecture securisee d'un justificatif PDF.

    public byte[] load(String filename) throws IOException {

        if (filename == null
                || !filename.matches(UUID_PDF_PATTERN)) {

            throw new ResourceNotFoundException(
                    "Justificatif introuvable");
        }

        Path target = directory().resolve(filename);

        if (!Files.exists(target)) {

            throw new ResourceNotFoundException(
                    "Justificatif introuvable");
        }

        return Files.readAllBytes(target);
    }

    // Nettoyage en cas d'echec de persistance.

    public void deleteQuietly(String filename) {

        if (filename == null
                || !filename.matches(UUID_PDF_PATTERN)) {

            return;
        }

        try {

            Files.deleteIfExists(
                    directory().resolve(filename));

        } catch (IOException exception) {

            LOGGER.warn(
                    "Echec du nettoyage d'un justificatif medical");
        }
    }

    // Resolution du dossier de stockage.

    Path directory() throws IOException {
        String custom =
                System.getenv("JUSTIFICATIFS_DIR");

        Path directory;

        if (custom != null && !custom.isBlank()) {

            directory = Path.of(custom);

        } else {

            String localAppData =
                    System.getenv("LOCALAPPDATA");

            if (localAppData != null
                    && !localAppData.isBlank()) {

                directory = Path.of(
                        localAppData,
                        "PGDI",
                        "justificatifs"
                );

            } else {

                directory = Path.of(
                        System.getProperty("user.home", "/app"),
                        ".pgdi",
                        "justificatifs"
                );
            }
        }

        directory = directory.toAbsolutePath().normalize();

        Files.createDirectories(directory);

        return directory;
    }
}
