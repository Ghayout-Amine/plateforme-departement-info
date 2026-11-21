package ma.ump.deptinfo.backend.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import ma.ump.deptinfo.backend.exceptions.BadRequestException;
import ma.ump.deptinfo.backend.exceptions.ResourceNotFoundException;

import static org.junit.jupiter.api.Assertions.*;

// PGDI2026-80 : contrôles sur le fichier de convention (vrai PDF, taille, nom serveur)
class AgreementStorageServiceTest {

    private final AgreementStorageService storage = new AgreementStorageService();

    @Test
    @DisplayName("isPdf : reconnaît l'en-tête %PDF- et refuse le reste")
    void isPdf() {
        assertTrue(AgreementStorageService.isPdf("%PDF-1.7 contenu".getBytes()));
        assertFalse(AgreementStorageService.isPdf("<html>faux pdf</html>".getBytes()));
        assertFalse(AgreementStorageService.isPdf(new byte[0]));
        assertFalse(AgreementStorageService.isPdf(null));
    }

    @Test
    @DisplayName("store : refuse un fichier qui n'est pas un PDF, même avec l'extension .pdf")
    void store_fauxPdf() {
        MockMultipartFile faux = new MockMultipartFile("file", "convention.pdf", "application/pdf", "pas un pdf".getBytes());
        assertThrows(BadRequestException.class, () -> storage.store(faux));
    }

    @Test
    @DisplayName("store : refuse un fichier vide ou absent")
    void store_vide() {
        MockMultipartFile vide = new MockMultipartFile("file", "convention.pdf", "application/pdf", new byte[0]);
        assertThrows(BadRequestException.class, () -> storage.store(vide));
        assertThrows(BadRequestException.class, () -> storage.store(null));
    }

    @Test
    @DisplayName("store : refuse un fichier de plus de 5 Mo")
    void store_tropGros() {
        byte[] gros = new byte[(int) AgreementStorageService.MAX_BYTES + 1];
        System.arraycopy("%PDF-".getBytes(), 0, gros, 0, 5);
        MockMultipartFile fichier = new MockMultipartFile("file", "convention.pdf", "application/pdf", gros);
        assertThrows(BadRequestException.class, () -> storage.store(fichier));
    }

    @Test
    @DisplayName("load : refuse un nom qui n'est pas un UUID.pdf (pas de ../)")
    void load_nomInterdit() {
        assertThrows(ResourceNotFoundException.class, () -> storage.load("../../etc/passwd"));
        assertThrows(ResourceNotFoundException.class, () -> storage.load(null));
    }
}
