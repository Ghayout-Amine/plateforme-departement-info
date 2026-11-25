
package ma.ump.deptinfo.backend.services;

import ma.ump.deptinfo.backend.exceptions.BadRequestException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class JustificatifStorageServiceTest {

    @TempDir
    Path tempDir;

    private JustificatifStorageService service;

    @BeforeEach
    void setUp() {

        service = new JustificatifStorageService() {

            @Override
            Path directory() {
                return tempDir;
            }
        };
    }

    @Test
    void shouldStorePdfWithSecureUuid() throws Exception {

        byte[] content = "%PDF-1.7\nTest document"
                .getBytes(StandardCharsets.US_ASCII);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "../../certificat.pdf",
                "application/pdf",
                content
        );

        String filename = service.store(file);

        assertTrue(filename.matches(
                "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-"
                + "[0-9a-f]{4}-[0-9a-f]{12}\\.pdf"));

        Path storedFile = tempDir.resolve(filename);

        assertTrue(Files.exists(storedFile));

        assertArrayEquals(
                content,
                Files.readAllBytes(storedFile)
        );
    }

    @Test
    void shouldRejectInvalidPdf() {

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "fake.pdf",
                "application/pdf",
                "Not a PDF".getBytes(StandardCharsets.US_ASCII)
        );

        assertThrows(
                BadRequestException.class,
                () -> service.store(file)
        );
    }

    @Test
    void shouldRejectEmptyFile() {

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "empty.pdf",
                "application/pdf",
                new byte[0]
        );

        assertThrows(
                BadRequestException.class,
                () -> service.store(file)
        );
    }

    @Test
    void shouldRejectFileLargerThanFiveMb() {

        byte[] content = new byte[
                JustificatifStorageService.MAX_BYTES + 1
        ];

        byte[] header = "%PDF-"
                .getBytes(StandardCharsets.US_ASCII);

        System.arraycopy(
                header, 0, content, 0, header.length
        );

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "large.pdf",
                "application/pdf",
                content
        );

        assertThrows(
                BadRequestException.class,
                () -> service.store(file)
        );
    }

    @Test
    void shouldAcceptExactlyFiveMb() throws Exception {

        byte[] content = new byte[
                JustificatifStorageService.MAX_BYTES
        ];

        byte[] header = "%PDF-"
                .getBytes(StandardCharsets.US_ASCII);

        System.arraycopy(
                header, 0, content, 0, header.length
        );

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "limit.pdf",
                "application/pdf",
                content
        );

        String filename = service.store(file);

        assertEquals(
                JustificatifStorageService.MAX_BYTES,
                Files.size(tempDir.resolve(filename))
        );
    }

    @Test
    void shouldDeleteStoredFile() throws Exception {

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "certificat.pdf",
                "application/pdf",
                "%PDF-1.7".getBytes(StandardCharsets.US_ASCII)
        );

        String filename = service.store(file);

        service.deleteQuietly(filename);

        assertFalse(Files.exists(
                tempDir.resolve(filename)));
    }
}
