package ma.ump.deptinfo.backend.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", "MaSuperCleSecreteDeTestPourLeProjetPGDI2026Secure!");
    }

    @Test
    void testGenerateAndValidateToken() {
        String email = "test.admin@pgdi.test";
        String token = jwtService.generateToken(email, "ADMIN");

        assertNotNull(token);
        assertFalse(token.isBlank());
        assertEquals(email, jwtService.extractUsername(token));
        assertTrue(jwtService.isTokenValid(token, email));
    }
}
