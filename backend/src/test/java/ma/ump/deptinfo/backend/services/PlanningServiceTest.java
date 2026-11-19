package ma.ump.deptinfo.backend.services;

import ma.ump.deptinfo.backend.dtos.PlanningRequestDTO;
import ma.ump.deptinfo.backend.dtos.PlanningResponseDTO;
import ma.ump.deptinfo.backend.entities.*;
import ma.ump.deptinfo.backend.exceptions.BadRequestException;
import ma.ump.deptinfo.backend.exceptions.ConflictException;
import ma.ump.deptinfo.backend.exceptions.ResourceNotFoundException;
import ma.ump.deptinfo.backend.repositories.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * PGDI2026-75 (US36) : règles de l'emploi du temps, testées sans base de données.
 */
@ExtendWith(MockitoExtension.class)
class PlanningServiceTest {

    @Mock
    private PlanningRepository planningRepository;
    @Mock
    private ExamenRepository examenRepository;
    @Mock
    private TeacherRepository teacherRepository;
    @Mock
    private ModuleCoursRepository moduleCoursRepository;
    @Mock
    private GroupeRepository groupeRepository;
    @Mock
    private SalleRepository salleRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PlanningService planningService;

    private static final LocalDate MONDAY = LocalDate.of(2026, 11, 23);
    private static final LocalTime H0830 = LocalTime.of(8, 30);
    private static final LocalTime H1030 = LocalTime.of(10, 30);

    private Filiere isi;
    private Teacher nadia;
    private Teacher youssef;
    private ModuleCours gl501;
    private ModuleCours ml504;
    private Groupe groupe1;
    private Groupe groupe2;
    private Salle s12;
    private Salle tp1;
    private Salle amphi;
    private Salle sr1;
    private PlanningRequestDTO dto;

    @BeforeEach
    void setUp() {
        isi = filiere(1L, "M2I-ISI");
        Filiere ia = filiere(2L, "M2I-IA");

        nadia = teacher(1L, "Nadia", "Alami");
        youssef = teacher(2L, "Youssef", "Bennani");

        gl501 = module(1L, "GL501", isi);
        ml504 = module(5L, "ML504", ia);

        groupe1 = groupe(1L, "Groupe 1 - ISI", 30, isi);
        groupe2 = groupe(2L, "Groupe 2 - ISI", 30, isi);

        amphi = salle(1L, "AMPHI_A", 150, true);
        s12 = salle(3L, "S12", 40, true);
        tp1 = salle(4L, "TP1", 24, true);
        sr1 = salle(6L, "SR1", 12, false);

        dto = request(1L, 1L, 1L, 3L);
    }

    // ---------- fabriques de données ----------

    private static Filiere filiere(Long id, String code) {
        Filiere f = new Filiere();
        f.setId(id);
        f.setCode(code);
        f.setNom("Filière " + code);
        f.setNiveau("MASTER");
        return f;
    }

    private static Teacher teacher(Long id, String firstName, String lastName) {
        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        Teacher t = new Teacher();
        ReflectionTestUtils.setField(t, "id", id);
        t.setEmployeeNumber("ENS-00" + id);
        t.setUser(user);
        return t;
    }

    private static ModuleCours module(Long id, String code, Filiere filiere) {
        ModuleCours m = new ModuleCours();
        m.setId(id);
        m.setCode(code);
        m.setNom("Module " + code);
        m.setFiliere(filiere);
        return m;
    }

    private static Groupe groupe(Long id, String name, Integer capacity, Filiere filiere) {
        Groupe g = new Groupe();
        ReflectionTestUtils.setField(g, "id", id);
        g.setName(name);
        g.setCapacity(capacity);
        g.setFiliere(filiere);
        return g;
    }

    private static Salle salle(Long id, String code, int capacity, boolean active) {
        Salle s = new Salle();
        ReflectionTestUtils.setField(s, "id", id);
        s.setCode(code);
        s.setName("Salle " + code);
        s.setCapacity(capacity);
        s.setRoomType(RoomType.CLASSROOM);
        s.setActive(active);
        return s;
    }

    private static PlanningRequestDTO request(Long teacherId, Long moduleId, Long groupeId, Long salleId) {
        PlanningRequestDTO r = new PlanningRequestDTO();
        r.setTeacherId(teacherId);
        r.setModuleId(moduleId);
        r.setGroupeId(groupeId);
        r.setSalleId(salleId);
        r.setSessionType(SessionType.TD);
        r.setSessionDate(MONDAY);
        r.setStartTime(H0830);
        r.setEndTime(H1030);
        return r;
    }

    private Planning existing(Long id, Teacher t, Groupe g, Salle s, LocalTime start, LocalTime end) {
        Planning p = new Planning();
        ReflectionTestUtils.setField(p, "id", id);
        p.setTeacher(t);
        p.setModule(gl501);
        p.setGroupe(g);
        p.setSalle(s);
        p.setSessionType(SessionType.CM);
        p.setSessionDate(MONDAY);
        p.setStartTime(start);
        p.setEndTime(end);
        return p;
    }

    /** Les 4 références de la requête par défaut existent (Nadia, GL501, Groupe 1, S12). */
    private void mockReferences() {
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(nadia));
        when(moduleCoursRepository.findById(1L)).thenReturn(Optional.of(gl501));
        when(groupeRepository.findById(1L)).thenReturn(Optional.of(groupe1));
        when(salleRepository.findById(3L)).thenReturn(Optional.of(s12));
    }

    private void mockNoConflict() {
        when(planningRepository.findOverlapping(any(), any(), any(), any(), eq(PlanningStatus.PLANNED))).thenReturn(List.of());
        when(examenRepository.findOverlapping(any(), any(), any(), isNull())).thenReturn(List.of());
    }

    private void mockSaveWithId(Long id) {
        when(planningRepository.save(any(Planning.class))).thenAnswer(invocation -> {
            Planning p = invocation.getArgument(0);
            if (p.getId() == null) {
                ReflectionTestUtils.setField(p, "id", id);
            }
            return p;
        });
    }

    // ---------- création ----------

    @Test
    @DisplayName("createPlanning - Succès : séance PLANNED avec les libellés pour l'affichage")
    void createPlanning_Success() {
        mockReferences();
        mockNoConflict();
        mockSaveWithId(100L);

        PlanningResponseDTO response = planningService.createPlanning(dto);

        assertEquals(100L, response.getId());
        assertEquals(PlanningStatus.PLANNED, response.getStatus());
        assertEquals(SessionType.TD, response.getSessionType());
        assertEquals("MONDAY", response.getDayOfWeek());
        assertEquals("Nadia Alami", response.getTeacherName());
        assertEquals("GL501", response.getModuleCode());
        assertEquals("Groupe 1 - ISI", response.getGroupeName());
        assertEquals("M2I-ISI", response.getFiliereCode());
        assertEquals("S12", response.getSalleCode());
        assertEquals(H0830, response.getStartTime());
        assertEquals(H1030, response.getEndTime());
    }

    @Test
    @DisplayName("createPlanning - 404 : enseignant inexistant")
    void createPlanning_TeacherNotFound() {
        when(teacherRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> planningService.createPlanning(dto));
        verify(planningRepository, never()).save(any());
    }

    @Test
    @DisplayName("createPlanning - 400 : l'heure de fin précède l'heure de début")
    void createPlanning_EndBeforeStart() {
        mockReferences();
        dto.setStartTime(LocalTime.of(14, 0));
        dto.setEndTime(LocalTime.of(12, 0));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> planningService.createPlanning(dto));
        assertTrue(ex.getMessage().contains("heure de fin"));
    }

    @Test
    @DisplayName("createPlanning - 400 : module d'une autre filière que le groupe")
    void createPlanning_ModuleNotInFiliere() {
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(nadia));
        when(moduleCoursRepository.findById(5L)).thenReturn(Optional.of(ml504));
        when(groupeRepository.findById(1L)).thenReturn(Optional.of(groupe1));
        when(salleRepository.findById(3L)).thenReturn(Optional.of(s12));
        dto.setModuleId(5L);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> planningService.createPlanning(dto));
        assertTrue(ex.getMessage().contains("ML504"));
    }

    @Test
    @DisplayName("createPlanning - 400 : salle hors service")
    void createPlanning_SalleInactive() {
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(nadia));
        when(moduleCoursRepository.findById(1L)).thenReturn(Optional.of(gl501));
        when(groupeRepository.findById(1L)).thenReturn(Optional.of(groupe1));
        when(salleRepository.findById(6L)).thenReturn(Optional.of(sr1));
        dto.setSalleId(6L);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> planningService.createPlanning(dto));
        assertTrue(ex.getMessage().contains("hors service"));
    }

    @Test
    @DisplayName("createPlanning - 400 : salle trop petite pour le groupe (24 places pour 30 étudiants)")
    void createPlanning_SalleTooSmall() {
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(nadia));
        when(moduleCoursRepository.findById(1L)).thenReturn(Optional.of(gl501));
        when(groupeRepository.findById(1L)).thenReturn(Optional.of(groupe1));
        when(salleRepository.findById(4L)).thenReturn(Optional.of(tp1));
        dto.setSalleId(4L);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> planningService.createPlanning(dto));
        assertTrue(ex.getMessage().contains("trop petite"));
    }

    @Test
    @DisplayName("createPlanning - 409 : la salle est déjà occupée par une autre séance")
    void createPlanning_SalleConflict() {
        mockReferences();
        Planning other = existing(50L, youssef, groupe2, s12, LocalTime.of(9, 0), LocalTime.of(11, 0));
        when(planningRepository.findOverlapping(MONDAY, H0830, H1030, null, PlanningStatus.PLANNED)).thenReturn(List.of(other));
        when(examenRepository.findOverlapping(MONDAY, H0830, H1030, null)).thenReturn(List.of());

        ConflictException ex = assertThrows(ConflictException.class, () -> planningService.createPlanning(dto));
        assertTrue(ex.getMessage().contains("la salle S12 est déjà occupée"));
        assertFalse(ex.getMessage().contains("enseignant"));
        verify(planningRepository, never()).save(any());
    }

    @Test
    @DisplayName("createPlanning - 409 : l'enseignant a déjà cours sur ce créneau (autre salle)")
    void createPlanning_TeacherConflict() {
        mockReferences();
        Planning other = existing(51L, nadia, groupe2, amphi, LocalTime.of(10, 0), LocalTime.of(12, 0));
        when(planningRepository.findOverlapping(MONDAY, H0830, H1030, null, PlanningStatus.PLANNED)).thenReturn(List.of(other));
        when(examenRepository.findOverlapping(MONDAY, H0830, H1030, null)).thenReturn(List.of());

        ConflictException ex = assertThrows(ConflictException.class, () -> planningService.createPlanning(dto));
        assertTrue(ex.getMessage().contains("l'enseignant Nadia Alami a déjà cours"));
    }

    @Test
    @DisplayName("createPlanning - 409 : le groupe a déjà cours, tous les conflits sont listés")
    void createPlanning_GroupeConflictListsAll() {
        mockReferences();
        Planning other = existing(52L, nadia, groupe1, s12, H0830, H1030);
        when(planningRepository.findOverlapping(MONDAY, H0830, H1030, null, PlanningStatus.PLANNED)).thenReturn(List.of(other));
        when(examenRepository.findOverlapping(MONDAY, H0830, H1030, null)).thenReturn(List.of());

        ConflictException ex = assertThrows(ConflictException.class, () -> planningService.createPlanning(dto));
        assertTrue(ex.getMessage().contains("la salle S12"));
        assertTrue(ex.getMessage().contains("l'enseignant Nadia Alami"));
        assertTrue(ex.getMessage().contains("le groupe \"Groupe 1 - ISI\""));
    }

    @Test
    @DisplayName("createPlanning - 409 : le groupe passe un examen sur ce créneau")
    void createPlanning_ExamConflict() {
        mockReferences();
        Examen exam = new Examen();
        exam.setTitle("Contrôle GL");
        exam.setGroupe(groupe1);
        exam.setSalles(Set.of(amphi));
        exam.setExamDate(MONDAY);
        exam.setStartTime(LocalTime.of(9, 0));
        exam.setEndTime(LocalTime.of(10, 0));
        when(planningRepository.findOverlapping(MONDAY, H0830, H1030, null, PlanningStatus.PLANNED)).thenReturn(List.of());
        when(examenRepository.findOverlapping(MONDAY, H0830, H1030, null)).thenReturn(List.of(exam));

        ConflictException ex = assertThrows(ConflictException.class, () -> planningService.createPlanning(dto));
        assertTrue(ex.getMessage().contains("passe un examen"));
    }

    @Test
    @DisplayName("Planning.overlaps - créneaux bout à bout acceptés, chevauchement détecté")
    void overlaps_Rules() {
        Planning p = existing(1L, nadia, groupe1, s12, H0830, H1030);

        assertFalse(p.overlaps(MONDAY, H1030, LocalTime.of(12, 30)), "10:30-12:30 suit 08:30-10:30 sans chevauchement");
        assertTrue(p.overlaps(MONDAY, LocalTime.of(10, 0), LocalTime.of(12, 0)));
        assertFalse(p.overlaps(MONDAY.plusDays(1), H0830, H1030), "même créneau mais autre jour");
    }

    // ---------- modification, série, annulation ----------

    @Test
    @DisplayName("updatePlanning - la séance modifiée est exclue de la recherche de conflits")
    void updatePlanning_ExcludesItself() {
        Planning current = existing(7L, nadia, groupe1, s12, H0830, H1030);
        when(planningRepository.findById(7L)).thenReturn(Optional.of(current));
        mockReferences();
        when(planningRepository.findOverlapping(MONDAY, H0830, LocalTime.of(11, 0), 7L, PlanningStatus.PLANNED)).thenReturn(List.of());
        when(examenRepository.findOverlapping(MONDAY, H0830, LocalTime.of(11, 0), null)).thenReturn(List.of());
        mockSaveWithId(7L);
        dto.setEndTime(LocalTime.of(11, 0));

        PlanningResponseDTO response = planningService.updatePlanning(7L, dto);

        assertEquals(7L, response.getId());
        assertEquals(LocalTime.of(11, 0), response.getEndTime());
    }

    @Test
    @DisplayName("createSerie - 3 semaines : 3 séances le même jour, à 7 jours d'intervalle")
    @SuppressWarnings("unchecked")
    void createSerie_ThreeWeeks() {
        mockReferences();
        mockNoConflict();
        when(planningRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        dto.setWeeks(3);

        List<PlanningResponseDTO> response = planningService.createSerie(dto);

        assertEquals(3, response.size());
        assertEquals(List.of(MONDAY, MONDAY.plusWeeks(1), MONDAY.plusWeeks(2)),
                response.stream().map(PlanningResponseDTO::getSessionDate).toList());
        ArgumentCaptor<List<Planning>> captor = ArgumentCaptor.forClass(List.class);
        verify(planningRepository).saveAll(captor.capture());
        assertEquals(3, captor.getValue().size());
    }

    @Test
    @DisplayName("createSerie - tout ou rien : un conflit la 2e semaine bloque toute la série")
    void createSerie_ConflictOnSecondWeek() {
        mockReferences();
        LocalDate week2 = MONDAY.plusWeeks(1);
        Planning busy = existing(60L, youssef, groupe2, s12, H0830, H1030);
        busy.setSessionDate(week2);
        when(planningRepository.findOverlapping(MONDAY, H0830, H1030, null, PlanningStatus.PLANNED)).thenReturn(List.of());
        when(planningRepository.findOverlapping(week2, H0830, H1030, null, PlanningStatus.PLANNED)).thenReturn(List.of(busy));
        when(examenRepository.findOverlapping(any(), any(), any(), isNull())).thenReturn(List.of());
        dto.setWeeks(4);

        ConflictException ex = assertThrows(ConflictException.class, () -> planningService.createSerie(dto));
        assertTrue(ex.getMessage().contains(week2.toString()));
        verify(planningRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("createSerie - 400 : plus de 16 semaines refusé")
    void createSerie_TooManyWeeks() {
        dto.setWeeks(20);

        assertThrows(BadRequestException.class, () -> planningService.createSerie(dto));
        verifyNoInteractions(teacherRepository);
    }

    @Test
    @DisplayName("cancelPlanning - la séance passe à CANCELLED")
    void cancelPlanning_Success() {
        Planning current = existing(8L, nadia, groupe1, s12, H0830, H1030);
        when(planningRepository.findById(8L)).thenReturn(Optional.of(current));
        when(planningRepository.save(current)).thenReturn(current);

        PlanningResponseDTO response = planningService.cancelPlanning(8L);

        assertEquals(PlanningStatus.CANCELLED, response.getStatus());
    }

    @Test
    @DisplayName("deletePlanning - 404 : séance inexistante")
    void deletePlanning_NotFound() {
        when(planningRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> planningService.deletePlanning(999L));
        verify(planningRepository, never()).delete(any());
    }

    // ---------- consultation ----------

    @Test
    @DisplayName("getPlannings - un mercredi donne la semaine du lundi 23 au dimanche 29 novembre")
    void getPlannings_WeekOfDate() {
        when(planningRepository.search(MONDAY, LocalDate.of(2026, 11, 29), 1L, null, null)).thenReturn(List.of());

        planningService.getPlannings(LocalDate.of(2026, 11, 25), null, null, 1L, null, null);

        verify(planningRepository).search(MONDAY, LocalDate.of(2026, 11, 29), 1L, null, null);
    }

    @Test
    @DisplayName("getPlannings - 400 : from sans to, ou période inversée")
    void getPlannings_InvalidRange() {
        assertThrows(BadRequestException.class,
                () -> planningService.getPlannings(null, MONDAY, null, null, null, null));
        assertThrows(BadRequestException.class,
                () -> planningService.getPlannings(null, MONDAY, MONDAY.minusDays(1), null, null, null));
    }

    @Test
    @DisplayName("getMyPlannings - étudiant : séances de son groupe ; enseignant : ses séances")
    void getMyPlannings_ByRole() {
        User student = new User();
        student.setRole(Role.ETUDIANT);
        User teacherUser = new User();
        teacherUser.setRole(Role.ENSEIGNANT);
        when(userRepository.findByEmail("omar@test")).thenReturn(Optional.of(student));
        when(userRepository.findByEmail("nadia@test")).thenReturn(Optional.of(teacherUser));
        LocalDate sunday = MONDAY.plusDays(6);
        when(planningRepository.findForStudent("omar@test", MONDAY, sunday, EnrollmentStatus.ACTIVE))
                .thenReturn(List.of(existing(1L, nadia, groupe1, s12, H0830, H1030)));
        when(planningRepository.findForTeacher("nadia@test", MONDAY, sunday)).thenReturn(List.of());

        assertEquals(1, planningService.getMyPlannings("omar@test", MONDAY).size());
        assertEquals(0, planningService.getMyPlannings("nadia@test", MONDAY).size());
    }

    @Test
    @DisplayName("getMyPlannings - 400 : réservé aux étudiants et aux enseignants")
    void getMyPlannings_AdminRefused() {
        User admin = new User();
        admin.setRole(Role.ADMIN);
        when(userRepository.findByEmail("admin@test")).thenReturn(Optional.of(admin));

        assertThrows(BadRequestException.class, () -> planningService.getMyPlannings("admin@test", MONDAY));
    }
}
