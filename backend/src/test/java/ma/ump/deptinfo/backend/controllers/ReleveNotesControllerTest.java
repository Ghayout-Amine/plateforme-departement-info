
package ma.ump.deptinfo.backend.controllers;

import ma.ump.deptinfo.backend.dtos.ReleveNotesResponseDTO;
import ma.ump.deptinfo.backend.exceptions.BadRequestException;
import ma.ump.deptinfo.backend.exceptions.GlobalExceptionHandler;
import ma.ump.deptinfo.backend.exceptions.ResourceNotFoundException;
import ma.ump.deptinfo.backend.security.JwtAuthenticationFilter;
import ma.ump.deptinfo.backend.security.JwtService;
import ma.ump.deptinfo.backend.security.SecurityConfig;
import ma.ump.deptinfo.backend.services.ReleveNotesService;

import jakarta.servlet.Filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReleveNotesController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        JwtService.class,
        GlobalExceptionHandler.class
})
@TestPropertySource(properties = {
        "app.jwt.secret=PGDI-test-only-secret-key-for-jwt-controller-tests-2026"
})
class ReleveNotesControllerTest {

    private static final String URL =
            "/api/notes/releve/1";

    private static final String STUDENT_EMAIL =
            "student@test.local";

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    @Qualifier("springSecurityFilterChain")
    private Filter springSecurityFilterChain;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private ReleveNotesService releveNotesService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private MockMvc mockMvc;

    // Configuration manuelle de MockMvc.
    // Le filtre JWT est execute uniquement
    // dans la chaine Spring Security.

    @BeforeEach
    void setUp() {

        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .addFilters(springSecurityFilterChain)
                .build();
    }

    private String createToken(String email, String role) {

        when(userDetailsService.loadUserByUsername(email))
                .thenReturn(
                        User.withUsername(email)
                                .password("test-password")
                                .roles(role)
                                .build()
                );

        return "Bearer " + jwtService.generateToken(email, role);
    }

    private ReleveNotesResponseDTO createReleveResponse() {

        List<ReleveNotesResponseDTO.ModuleResultDTO> modules =
                List.of(
                        new ReleveNotesResponseDTO.ModuleResultDTO(
                                "JAVA",
                                "Java",
                                new BigDecimal("14.50"),
                                "VALIDE"
                        ),
                        new ReleveNotesResponseDTO.ModuleResultDTO(
                                "RES",
                                "Reseaux",
                                new BigDecimal("11.00"),
                                "VALIDE"
                        ),
                        new ReleveNotesResponseDTO.ModuleResultDTO(
                                "CYBER",
                                "Cybersecurite",
                                new BigDecimal("8.50"),
                                "VALIDE_PAR_COMPENSATION"
                        )
                );

        return new ReleveNotesResponseDTO(
                "Test Student",
                "M2I001",
                "S3",
                modules,
                new BigDecimal("11.33"),
                "ADMIS",
                "Passable"
        );
    }

    // TEST 1 : Consultation autorisee par un etudiant.

    @Test
    void shouldReturnTranscriptForAuthenticatedStudent()
            throws Exception {

        String token = createToken(
                STUDENT_EMAIL, "ETUDIANT");

        when(releveNotesService.getReleveNotes(
                1L, "S3", STUDENT_EMAIL))
                .thenReturn(createReleveResponse());

        mockMvc.perform(
                        get(URL)
                                .param("semester", "S3")
                                .header("Authorization", token)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentName")
                        .value("Test Student"))
                .andExpect(jsonPath("$.studentNumber")
                        .value("M2I001"))
                .andExpect(jsonPath("$.semester")
                        .value("S3"))
                .andExpect(jsonPath("$.semesterAverage")
                        .value(11.33))
                .andExpect(jsonPath("$.decision")
                        .value("ADMIS"))
                .andExpect(jsonPath("$.mention")
                        .value("Passable"))
                .andExpect(jsonPath("$.modules.length()")
                        .value(3))
                .andExpect(jsonPath("$.modules[0].code")
                        .value("JAVA"))
                .andExpect(jsonPath("$.modules[0].score")
                        .value(14.50))
                .andExpect(jsonPath("$.modules[0].status")
                        .value("VALIDE"))
                .andExpect(jsonPath("$.modules[2].status")
                        .value("VALIDE_PAR_COMPENSATION"));

        verify(releveNotesService)
                .getReleveNotes(1L, "S3", STUDENT_EMAIL);
    }

    // TEST 2 : Semestre invalide.

    @Test
    void shouldReturnBadRequestForInvalidSemester()
            throws Exception {

        String token = createToken(
                STUDENT_EMAIL, "ETUDIANT");

        when(releveNotesService.getReleveNotes(
                1L, "S9", STUDENT_EMAIL))
                .thenThrow(
                        new BadRequestException(
                                "Semestre invalide : S9"));

        mockMvc.perform(
                        get(URL)
                                .param("semester", "S9")
                                .header("Authorization", token)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message")
                        .value("Semestre invalide : S9"));
    }

    // TEST 3 : Requete sans JWT.

    @Test
    void shouldRejectRequestWithoutToken()
            throws Exception {

        mockMvc.perform(
                        get(URL)
                                .param("semester", "S3")
                )
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(releveNotesService);
    }

    // TEST 4 : JWT invalide.

    @Test
    void shouldRejectInvalidToken()
            throws Exception {

        mockMvc.perform(
                        get(URL)
                                .param("semester", "S3")
                                .header(
                                        "Authorization",
                                        "Bearer invalid-token")
                )
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(releveNotesService);
    }

    // TEST 5 : Acces interdit pour un enseignant.

    @Test
    void shouldRejectTeacherAccess()
            throws Exception {

        String token = createToken(
                "teacher@test.local", "ENSEIGNANT");

        mockMvc.perform(
                        get(URL)
                                .param("semester", "S3")
                                .header("Authorization", token)
                )
                .andExpect(status().isForbidden());

        verifyNoInteractions(releveNotesService);
    }

    // TEST 6 : Consultation du releve d'un autre etudiant.

    @Test
    void shouldReturnForbiddenForAnotherStudentsTranscript()
            throws Exception {

        String email = "other@test.local";

        String token = createToken(email, "ETUDIANT");

        when(releveNotesService.getReleveNotes(
                1L, "S3", email))
                .thenThrow(
                        new AccessDeniedException("Acces refuse"));

        mockMvc.perform(
                        get(URL)
                                .param("semester", "S3")
                                .header("Authorization", token)
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error")
                        .value("Forbidden"));
    }

    // TEST 7 : Etudiant introuvable.

    @Test
    void shouldReturnNotFoundForUnknownStudent()
            throws Exception {

        String email = "admin@test.local";

        String token = createToken(email, "ADMIN");

        when(releveNotesService.getReleveNotes(
                999L, "S3", email))
                .thenThrow(
                        new ResourceNotFoundException(
                                "Etudiant introuvable : 999"));

        mockMvc.perform(
                        get("/api/notes/releve/999")
                                .param("semester", "S3")
                                .header("Authorization", token)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message")
                        .value("Etudiant introuvable : 999"));
    }

    // TEST 8 : Acces administrateur.

    @Test
    void shouldAllowAdminAccess()
            throws Exception {

        String email = "admin@test.local";

        String token = createToken(email, "ADMIN");

        when(releveNotesService.getReleveNotes(
                1L, "S3", email))
                .thenReturn(createReleveResponse());

        mockMvc.perform(
                        get(URL)
                                .param("semester", "S3")
                                .header("Authorization", token)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision")
                        .value("ADMIS"));

        verify(releveNotesService)
                .getReleveNotes(1L, "S3", email);
    }

    // TEST 9 : Acces chef de departement.

    @Test
    void shouldAllowDepartmentHeadAccess()
            throws Exception {

        String email = "chef@test.local";

        String token = createToken(
                email, "CHEF_DEPARTEMENT");

        when(releveNotesService.getReleveNotes(
                1L, "S3", email))
                .thenReturn(createReleveResponse());

        mockMvc.perform(
                        get(URL)
                                .param("semester", "S3")
                                .header("Authorization", token)
                )
                .andExpect(status().isOk());

        verify(releveNotesService)
                .getReleveNotes(1L, "S3", email);
    }

    // TEST 10 : Parametre semester manquant.

    @Test
    void shouldRejectMissingSemesterParameter()
            throws Exception {

        String token = createToken(
                STUDENT_EMAIL, "ETUDIANT");

        mockMvc.perform(
                        get(URL)
                                .header("Authorization", token)
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(releveNotesService);
    }
}
