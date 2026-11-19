package ma.ump.deptinfo.backend.services;

import ma.ump.deptinfo.backend.dtos.ExamenRequestDTO;
import ma.ump.deptinfo.backend.dtos.ExamenResponseDTO;
import ma.ump.deptinfo.backend.entities.*;
import ma.ump.deptinfo.backend.exceptions.BadRequestException;
import ma.ump.deptinfo.backend.exceptions.ConflictException;
import ma.ump.deptinfo.backend.exceptions.ResourceNotFoundException;
import ma.ump.deptinfo.backend.repositories.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

/**
 * PGDI2026-75 (US49) : règles du calendrier des examens, testées sans base de données.
 */
@ExtendWith(MockitoExtension.class)
class ExamenServiceTest {

    @Mock
    private ExamenRepository examenRepository;
    @Mock
    private PlanningRepository planningRepository;
    @Mock
    private ModuleCoursRepository moduleCoursRepository;
    @Mock
    private GroupeRepository groupeRepository;
    @Mock
    private SalleRepository salleRepository;
    @Mock
    private AcademicYearRepository academicYearRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ExamenService examenService;

    private static final LocalDate EXAM_DAY = LocalDate.of(2027, 1, 18);
    private static final LocalTime H0900 = LocalTime.of(9, 0);
    private static final LocalTime H1100 = LocalTime.of(11, 0);

    private AcademicYear year;
    private ModuleCours gl501;
    private ModuleCours ml504;
    private Groupe groupe1;
    private Salle amphiA;
    private Salle tp1;
    private Salle tp2;
    private Salle sr1;
    private ExamenRequestDTO dto;

    @BeforeEach
    void setUp() {
        Filiere isi = filiere(1L, "M2I-ISI");
        Filiere ia = filiere(2L, "M2I-IA");

        year = new AcademicYear();
        ReflectionTestUtils.setField(year, "id", 1L);
        year.setLabel("2026-2027");
        year.setStartDate(LocalDate.of(2026, 9, 1));
        year.setEndDate(LocalDate.of(2027, 7, 15));
        year.setCurrent(true);

        gl501 = module(1L, "GL501", isi);
        ml504 = module(5L, "ML504", ia);

        groupe1 = new Groupe();
        ReflectionTestUtils.setField(groupe1, "id", 1L);
        groupe1.setName("Groupe 1 - ISI");
        groupe1.setCapacity(30);
        groupe1.setFiliere(isi);

        amphiA = salle(1L, "AMPHI_A", 150, true);
        tp1 = salle(4L, "TP1", 24, true);
        tp2 = salle(5L, "TP2", 24, true);
        sr1 = salle(6L, "SR1", 12, false);

        dto = new ExamenRequestDTO();
        dto.setTitle("  Examen final Génie Logiciel ");
        dto.setExamType(ExamType.FINAL);
        dto.setModuleId(1L);
        dto.setGroupeId(1L);
        dto.setSalleIds(List.of(1L));
        dto.setExamDate(EXAM_DAY);
        dto.setStartTime(H0900);
        dto.setEndTime(H1100);
    }

    private static Filiere filiere(Long id, String code) {
        Filiere f = new Filiere();
        f.setId(id);
        f.setCode(code);
        f.setNom("Filière " + code);
        f.setNiveau("MASTER");
        return f;
    }

    private static ModuleCours module(Long id, String code, Filiere filiere) {
        ModuleCours m = new ModuleCours();
        m.setId(id);
        m.setCode(code);
        m.setNom("Module " + code);
        m.setFiliere(filiere);
        return m;
    }

    private static Salle salle(Long id, String code, int capacity, boolean active) {
        Salle s = new Salle();
        ReflectionTestUtils.setField(s, "id", id);
        s.setCode(code);
        s.setName("Salle " + code);
        s.setCapacity(capacity);
        s.setRoomType(RoomType.AMPHI);
        s.setActive(active);
        return s;
    }

    private void mockModuleGroupeYear() {
        when(moduleCoursRepository.findById(1L)).thenReturn(Optional.of(gl501));
        when(groupeRepository.findById(1L)).thenReturn(Optional.of(groupe1));
        when(academicYearRepository.findFirstByCurrentTrue()).thenReturn(Optional.of(year));
    }

    private void mockNoConflict(Long excludeId) {
        when(examenRepository.findOverlapping(EXAM_DAY, H0900, H1100, excludeId)).thenReturn(List.of());
        when(planningRepository.findOverlapping(EXAM_DAY, H0900, H1100, null, PlanningStatus.PLANNED)).thenReturn(List.of());
    }

    private void mockSave() {
        when(examenRepository.save(any(Examen.class))).thenAnswer(invocation -> {
            Examen e = invocation.getArgument(0);
            if (e.getId() == null) {
                ReflectionTestUtils.setField(e, "id", 200L);
            }
            return e;
        });
    }

    private Examen existingExam(Long id, Groupe groupe, Set<Salle> salles) {
        Examen e = new Examen();
        ReflectionTestUtils.setField(e, "id", id);
        e.setTitle("Contrôle continu");
        e.setExamType(ExamType.CC);
        e.setModule(gl501);
        e.setGroupe(groupe);
        e.setAcademicYear(year);
        e.setSalles(new LinkedHashSet<>(salles));
        e.setExamDate(EXAM_DAY);
        e.setStartTime(LocalTime.of(10, 0));
        e.setEndTime(LocalTime.of(12, 0));
        return e;
    }

    // ---------- création ----------

    @Test
    @DisplayName("createExamen - Succès : année en cours par défaut, titre nettoyé, capacité calculée")
    void createExamen_Success() {
        mockModuleGroupeYear();
        when(salleRepository.findById(1L)).thenReturn(Optional.of(amphiA));
        mockNoConflict(null);
        mockSave();

        ExamenResponseDTO response = examenService.createExamen(dto);

        assertEquals(200L, response.getId());
        assertEquals("Examen final Génie Logiciel", response.getTitle());
        assertEquals("2026-2027", response.getAcademicYearLabel());
        assertEquals(List.of("AMPHI_A"), response.getSalleCodes());
        assertEquals(150, response.getTotalCapacity());
        assertEquals("M2I-ISI", response.getFiliereCode());
    }

    @Test
    @DisplayName("createExamen - Succès : deux salles de TP (48 places) pour 30 étudiants, doublon d'id ignoré")
    void createExamen_TwoSallesEnough() {
        mockModuleGroupeYear();
        when(salleRepository.findById(4L)).thenReturn(Optional.of(tp1));
        when(salleRepository.findById(5L)).thenReturn(Optional.of(tp2));
        mockNoConflict(null);
        mockSave();
        dto.setSalleIds(List.of(5L, 4L, 5L));

        ExamenResponseDTO response = examenService.createExamen(dto);

        assertEquals(List.of("TP1", "TP2"), response.getSalleCodes());
        assertEquals(48, response.getTotalCapacity());
        verify(salleRepository, times(1)).findById(5L);
    }

    @Test
    @DisplayName("createExamen - 400 : une seule salle de 24 places pour 30 étudiants")
    void createExamen_NotEnoughPlaces() {
        mockModuleGroupeYear();
        when(salleRepository.findById(4L)).thenReturn(Optional.of(tp1));
        dto.setSalleIds(List.of(4L));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> examenService.createExamen(dto));
        assertTrue(ex.getMessage().contains("24 places pour 30"));
        verify(examenRepository, never()).save(any());
    }

    @Test
    @DisplayName("createExamen - 400 : aucune année universitaire en cours")
    void createExamen_NoCurrentYear() {
        when(moduleCoursRepository.findById(1L)).thenReturn(Optional.of(gl501));
        when(groupeRepository.findById(1L)).thenReturn(Optional.of(groupe1));
        when(academicYearRepository.findFirstByCurrentTrue()).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () -> examenService.createExamen(dto));
    }

    @Test
    @DisplayName("createExamen - 404 : année universitaire indiquée inexistante")
    void createExamen_YearNotFound() {
        when(moduleCoursRepository.findById(1L)).thenReturn(Optional.of(gl501));
        when(groupeRepository.findById(1L)).thenReturn(Optional.of(groupe1));
        when(academicYearRepository.findById(99L)).thenReturn(Optional.empty());
        dto.setAcademicYearId(99L);

        assertThrows(ResourceNotFoundException.class, () -> examenService.createExamen(dto));
    }

    @Test
    @DisplayName("createExamen - 404 : salle inexistante")
    void createExamen_SalleNotFound() {
        mockModuleGroupeYear();
        when(salleRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> examenService.createExamen(dto));
    }

    @Test
    @DisplayName("createExamen - 400 : salle hors service")
    void createExamen_SalleInactive() {
        mockModuleGroupeYear();
        when(salleRepository.findById(6L)).thenReturn(Optional.of(sr1));
        dto.setSalleIds(List.of(6L));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> examenService.createExamen(dto));
        assertTrue(ex.getMessage().contains("SR1"));
    }

    @Test
    @DisplayName("createExamen - 400 : date en dehors de l'année universitaire")
    void createExamen_OutsideAcademicYear() {
        mockModuleGroupeYear();
        when(salleRepository.findById(1L)).thenReturn(Optional.of(amphiA));
        dto.setExamDate(LocalDate.of(2027, 8, 20));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> examenService.createExamen(dto));
        assertTrue(ex.getMessage().contains("2026-2027"));
    }

    @Test
    @DisplayName("createExamen - 400 : module d'une autre filière que le groupe")
    void createExamen_ModuleNotInFiliere() {
        when(moduleCoursRepository.findById(5L)).thenReturn(Optional.of(ml504));
        when(groupeRepository.findById(1L)).thenReturn(Optional.of(groupe1));
        when(academicYearRepository.findFirstByCurrentTrue()).thenReturn(Optional.of(year));
        when(salleRepository.findById(1L)).thenReturn(Optional.of(amphiA));
        dto.setModuleId(5L);

        assertThrows(BadRequestException.class, () -> examenService.createExamen(dto));
    }

    @Test
    @DisplayName("createExamen - 400 : l'heure de fin précède l'heure de début")
    void createExamen_EndBeforeStart() {
        mockModuleGroupeYear();
        when(salleRepository.findById(1L)).thenReturn(Optional.of(amphiA));
        dto.setEndTime(LocalTime.of(8, 0));

        assertThrows(BadRequestException.class, () -> examenService.createExamen(dto));
    }

    @Test
    @DisplayName("createExamen - 409 : la salle est déjà réservée pour un autre examen")
    void createExamen_SalleTakenByExam() {
        mockModuleGroupeYear();
        when(salleRepository.findById(1L)).thenReturn(Optional.of(amphiA));
        Groupe otherGroupe = new Groupe();
        ReflectionTestUtils.setField(otherGroupe, "id", 3L);
        otherGroupe.setName("Groupe 1 - IA");
        when(examenRepository.findOverlapping(EXAM_DAY, H0900, H1100, null))
                .thenReturn(List.of(existingExam(10L, otherGroupe, Set.of(amphiA))));
        when(planningRepository.findOverlapping(EXAM_DAY, H0900, H1100, null, PlanningStatus.PLANNED)).thenReturn(List.of());

        ConflictException ex = assertThrows(ConflictException.class, () -> examenService.createExamen(dto));
        assertTrue(ex.getMessage().contains("la salle AMPHI_A est déjà réservée"));
        verify(examenRepository, never()).save(any());
    }

    @Test
    @DisplayName("createExamen - 409 : le groupe a cours pendant l'examen")
    void createExamen_GroupeHasCourse() {
        mockModuleGroupeYear();
        when(salleRepository.findById(1L)).thenReturn(Optional.of(amphiA));
        Planning course = new Planning();
        course.setModule(gl501);
        course.setGroupe(groupe1);
        course.setSalle(tp1);
        course.setSessionDate(EXAM_DAY);
        course.setStartTime(LocalTime.of(10, 30));
        course.setEndTime(LocalTime.of(12, 30));
        when(examenRepository.findOverlapping(EXAM_DAY, H0900, H1100, null)).thenReturn(List.of());
        when(planningRepository.findOverlapping(EXAM_DAY, H0900, H1100, null, PlanningStatus.PLANNED)).thenReturn(List.of(course));

        ConflictException ex = assertThrows(ConflictException.class, () -> examenService.createExamen(dto));
        assertTrue(ex.getMessage().contains("a cours"));
    }

    @Test
    @DisplayName("updateExamen - l'examen modifié est exclu de la recherche de conflits")
    void updateExamen_ExcludesItself() {
        Examen current = existingExam(10L, groupe1, Set.of(amphiA));
        when(examenRepository.findById(10L)).thenReturn(Optional.of(current));
        mockModuleGroupeYear();
        when(salleRepository.findById(1L)).thenReturn(Optional.of(amphiA));
        mockNoConflict(10L);
        mockSave();

        ExamenResponseDTO response = examenService.updateExamen(10L, dto);

        assertEquals(10L, response.getId());
        assertEquals(ExamType.FINAL, response.getExamType());
        verify(examenRepository).findOverlapping(EXAM_DAY, H0900, H1100, 10L);
    }

    @Test
    @DisplayName("deleteExamen - 404 : examen inexistant")
    void deleteExamen_NotFound() {
        when(examenRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> examenService.deleteExamen(999L));
    }

    // ---------- consultation ----------

    @Test
    @DisplayName("getExamens - sans période : toutes les dates ; période inversée : 400")
    void getExamens_Range() {
        when(examenRepository.search(ExamenService.NO_LOWER_BOUND, ExamenService.NO_UPPER_BOUND, 1L, null, null))
                .thenReturn(List.of(existingExam(10L, groupe1, Set.of(amphiA))));

        assertEquals(1, examenService.getExamens(null, null, 1L, null, null).size());
        assertThrows(BadRequestException.class,
                () -> examenService.getExamens(EXAM_DAY, EXAM_DAY.minusDays(1), null, null, null));
    }

    @Test
    @DisplayName("getMyExamens - étudiant, enseignant, et 400 pour les autres rôles")
    void getMyExamens_ByRole() {
        User student = new User();
        student.setRole(Role.ETUDIANT);
        User teacher = new User();
        teacher.setRole(Role.ENSEIGNANT);
        User chef = new User();
        chef.setRole(Role.CHEF_DEPARTEMENT);
        when(userRepository.findByEmail("omar@test")).thenReturn(Optional.of(student));
        when(userRepository.findByEmail("nadia@test")).thenReturn(Optional.of(teacher));
        when(userRepository.findByEmail("chef@test")).thenReturn(Optional.of(chef));
        when(examenRepository.findForStudent(any(), any(), any(), any())).thenReturn(List.of());
        when(examenRepository.findForTeacher(any(), any(), any())).thenReturn(List.of());

        examenService.getMyExamens("omar@test", null, null);
        examenService.getMyExamens("nadia@test", null, null);

        verify(examenRepository).findForStudent("omar@test", ExamenService.NO_LOWER_BOUND,
                ExamenService.NO_UPPER_BOUND, EnrollmentStatus.ACTIVE);
        verify(examenRepository).findForTeacher("nadia@test", ExamenService.NO_LOWER_BOUND, ExamenService.NO_UPPER_BOUND);
        assertThrows(BadRequestException.class, () -> examenService.getMyExamens("chef@test", null, null));
        verify(examenRepository, never()).findOverlapping(any(), any(), any(), isNull());
    }
}
