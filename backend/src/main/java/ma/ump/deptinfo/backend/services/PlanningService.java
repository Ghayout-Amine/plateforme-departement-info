package ma.ump.deptinfo.backend.services;

import ma.ump.deptinfo.backend.dtos.PlanningRequestDTO;
import ma.ump.deptinfo.backend.dtos.PlanningResponseDTO;
import ma.ump.deptinfo.backend.entities.*;
import ma.ump.deptinfo.backend.exceptions.BadRequestException;
import ma.ump.deptinfo.backend.exceptions.ConflictException;
import ma.ump.deptinfo.backend.exceptions.ResourceNotFoundException;
import ma.ump.deptinfo.backend.repositories.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

/**
 * Emploi du temps (PGDI2026-75, US36).
 * Règles appliquées à chaque création ou modification de séance :
 * - l'enseignant, le module, le groupe et la salle doivent exister (404) ;
 * - l'heure de fin doit suivre l'heure de début (400) ;
 * - le module doit appartenir à la filière du groupe (400) ;
 * - la salle doit être en service et assez grande pour le groupe (400) ;
 * - ni la salle, ni l'enseignant, ni le groupe ne doivent être déjà pris
 *   sur ce créneau, par une autre séance ou par un examen (409).
 */
@Service
@Transactional
public class PlanningService {

    static final int MAX_WEEKS = 16;
    static final int MAX_RANGE_DAYS = 366;

    private final PlanningRepository planningRepository;
    private final ExamenRepository examenRepository;
    private final TeacherRepository teacherRepository;
    private final ModuleCoursRepository moduleCoursRepository;
    private final GroupeRepository groupeRepository;
    private final SalleRepository salleRepository;
    private final UserRepository userRepository;

    public PlanningService(PlanningRepository planningRepository,
                           ExamenRepository examenRepository,
                           TeacherRepository teacherRepository,
                           ModuleCoursRepository moduleCoursRepository,
                           GroupeRepository groupeRepository,
                           SalleRepository salleRepository,
                           UserRepository userRepository) {
        this.planningRepository = planningRepository;
        this.examenRepository = examenRepository;
        this.teacherRepository = teacherRepository;
        this.moduleCoursRepository = moduleCoursRepository;
        this.groupeRepository = groupeRepository;
        this.salleRepository = salleRepository;
        this.userRepository = userRepository;
    }

    // ---------- consultation ----------

    /**
     * Séances d'une période. Sans from/to, on renvoie la semaine (lundi → dimanche)
     * qui contient la date "semaine" (aujourd'hui par défaut).
     */
    @Transactional(readOnly = true)
    public List<PlanningResponseDTO> getPlannings(LocalDate semaine, LocalDate from, LocalDate to,
                                                  Long groupeId, Long teacherId, Long salleId) {
        LocalDate[] range = resolveRange(semaine, from, to);
        return planningRepository.search(range[0], range[1], groupeId, teacherId, salleId).stream()
                .map(this::toResponseDTO)
                .toList();
    }

    /**
     * "Mon emploi du temps" : un étudiant voit les séances de son groupe,
     * un enseignant voit ses propres séances.
     */
    @Transactional(readOnly = true)
    public List<PlanningResponseDTO> getMyPlannings(String email, LocalDate semaine) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable : " + email));
        LocalDate[] week = weekOf(semaine);
        List<Planning> plannings = switch (user.getRole() == null ? Role.ADMIN : user.getRole()) {
            case ETUDIANT -> planningRepository.findForStudent(email, week[0], week[1], EnrollmentStatus.ACTIVE);
            case ENSEIGNANT -> planningRepository.findForTeacher(email, week[0], week[1]);
            default -> throw new BadRequestException(
                    "« Mon emploi du temps » est réservé aux étudiants et aux enseignants");
        };
        return plannings.stream().map(this::toResponseDTO).toList();
    }

    @Transactional(readOnly = true)
    public PlanningResponseDTO getPlanningById(Long id) {
        return toResponseDTO(findPlanning(id));
    }

    // ---------- création, modification, annulation ----------

    public PlanningResponseDTO createPlanning(PlanningRequestDTO dto) {
        Planning planning = new Planning();
        apply(planning, dto, dto.getSessionDate(), null);
        return toResponseDTO(planningRepository.save(planning));
    }

    /**
     * Crée la même séance chaque semaine pendant "weeks" semaines.
     * Tout ou rien : si une seule date est en conflit, aucune séance n'est créée.
     */
    public List<PlanningResponseDTO> createSerie(PlanningRequestDTO dto) {
        int weeks = dto.getWeeks() == null ? 1 : dto.getWeeks();
        if (weeks < 1 || weeks > MAX_WEEKS) {
            throw new BadRequestException("Le nombre de semaines doit être compris entre 1 et " + MAX_WEEKS);
        }
        List<Planning> serie = new ArrayList<>();
        for (int i = 0; i < weeks; i++) {
            Planning planning = new Planning();
            apply(planning, dto, dto.getSessionDate().plusWeeks(i), null);
            serie.add(planning);
        }
        return planningRepository.saveAll(serie).stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public PlanningResponseDTO updatePlanning(Long id, PlanningRequestDTO dto) {
        Planning planning = findPlanning(id);
        apply(planning, dto, dto.getSessionDate(), id);
        return toResponseDTO(planningRepository.save(planning));
    }

    // Une séance annulée libère la salle, l'enseignant et le groupe
    public PlanningResponseDTO cancelPlanning(Long id) {
        Planning planning = findPlanning(id);
        planning.cancel();
        return toResponseDTO(planningRepository.save(planning));
    }

    public void deletePlanning(Long id) {
        planningRepository.delete(findPlanning(id));
    }

    // ---------- règles métier ----------

    /**
     * Vérifie toutes les règles puis recopie les données dans la séance.
     * excludeId = id de la séance modifiée (elle ne doit pas entrer en conflit avec elle-même).
     */
    private void apply(Planning planning, PlanningRequestDTO dto, LocalDate date, Long excludeId) {
        Teacher teacher = teacherRepository.findById(dto.getTeacherId())
                .orElseThrow(() -> new ResourceNotFoundException("Enseignant introuvable avec l'id : " + dto.getTeacherId()));
        ModuleCours module = moduleCoursRepository.findById(dto.getModuleId())
                .orElseThrow(() -> new ResourceNotFoundException("Module introuvable avec l'id : " + dto.getModuleId()));
        Groupe groupe = groupeRepository.findById(dto.getGroupeId())
                .orElseThrow(() -> new ResourceNotFoundException("Groupe introuvable avec l'id : " + dto.getGroupeId()));
        Salle salle = salleRepository.findById(dto.getSalleId())
                .orElseThrow(() -> new ResourceNotFoundException("Salle introuvable avec l'id : " + dto.getSalleId()));

        LocalTime start = dto.getStartTime();
        LocalTime end = dto.getEndTime();
        if (!end.isAfter(start)) {
            throw new BadRequestException("L'heure de fin (" + end + ") doit être après l'heure de début (" + start + ")");
        }
        if (!module.getFiliere().getId().equals(groupe.getFiliere().getId())) {
            throw new BadRequestException("Le module " + module.getCode() + " n'est pas enseigné dans la filière "
                    + groupe.getFiliere().getCode() + " du groupe \"" + groupe.getName() + "\"");
        }
        if (!salle.isActive()) {
            throw new BadRequestException("La salle " + salle.getCode() + " est hors service");
        }
        if (groupe.getCapacity() != null && salle.getCapacity() != null && salle.getCapacity() < groupe.getCapacity()) {
            throw new BadRequestException("La salle " + salle.getCode() + " (" + salle.getCapacity()
                    + " places) est trop petite pour le groupe \"" + groupe.getName() + "\" (" + groupe.getCapacity() + " étudiants)");
        }

        List<String> conflicts = findConflicts(date, start, end, teacher, groupe, salle, excludeId);
        if (!conflicts.isEmpty()) {
            throw new ConflictException("Conflit le " + date + " : " + String.join(" ; ", conflicts));
        }

        planning.setTeacher(teacher);
        planning.setModule(module);
        planning.setGroupe(groupe);
        planning.setSalle(salle);
        planning.setSessionType(dto.getSessionType());
        planning.setSessionDate(date);
        planning.setStartTime(start);
        planning.setEndTime(end);
        planning.setNotes(dto.getNotes());
    }

    /**
     * Liste lisible des conflits sur le créneau : séances actives et examens
     * qui utilisent déjà la même salle, le même enseignant ou le même groupe.
     */
    List<String> findConflicts(LocalDate date, LocalTime start, LocalTime end,
                               Teacher teacher, Groupe groupe, Salle salle, Long excludeId) {
        List<String> conflicts = new ArrayList<>();
        for (Planning other : planningRepository.findOverlapping(date, start, end, excludeId, PlanningStatus.PLANNED)) {
            String slot = other.getStartTime() + "-" + other.getEndTime() + " (" + other.getModule().getCode() + ")";
            if (other.getSalle().getId().equals(salle.getId())) {
                conflicts.add("la salle " + salle.getCode() + " est déjà occupée " + slot);
            }
            if (other.getTeacher().getId().equals(teacher.getId())) {
                conflicts.add("l'enseignant " + teacherName(teacher) + " a déjà cours " + slot);
            }
            if (other.getGroupe().getId().equals(groupe.getId())) {
                conflicts.add("le groupe \"" + groupe.getName() + "\" a déjà cours " + slot);
            }
        }
        for (Examen exam : examenRepository.findOverlapping(date, start, end, null)) {
            String slot = exam.getStartTime() + "-" + exam.getEndTime() + " (examen " + exam.getTitle() + ")";
            boolean sameSalle = exam.getSalles().stream().anyMatch(s -> s.getId().equals(salle.getId()));
            if (sameSalle) {
                conflicts.add("la salle " + salle.getCode() + " est réservée " + slot);
            }
            if (exam.getGroupe().getId().equals(groupe.getId())) {
                conflicts.add("le groupe \"" + groupe.getName() + "\" passe un examen " + slot);
            }
        }
        return conflicts;
    }

    // ---------- périodes ----------

    /** Lundi et dimanche de la semaine qui contient la date (aujourd'hui par défaut). */
    static LocalDate[] weekOf(LocalDate day) {
        LocalDate ref = day != null ? day : LocalDate.now();
        LocalDate monday = ref.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return new LocalDate[]{monday, monday.plusDays(6)};
    }

    private LocalDate[] resolveRange(LocalDate semaine, LocalDate from, LocalDate to) {
        if (from == null && to == null) {
            return weekOf(semaine);
        }
        if (from == null || to == null) {
            throw new BadRequestException("Indiquez à la fois from et to, ou aucun des deux");
        }
        if (to.isBefore(from)) {
            throw new BadRequestException("La date de fin doit suivre la date de début");
        }
        if (ChronoUnit.DAYS.between(from, to) > MAX_RANGE_DAYS) {
            throw new BadRequestException("La période demandée ne peut pas dépasser " + MAX_RANGE_DAYS + " jours");
        }
        return new LocalDate[]{from, to};
    }

    // ---------- utilitaires ----------

    private Planning findPlanning(Long id) {
        return planningRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Séance introuvable avec l'id : " + id));
    }

    private static String teacherName(Teacher teacher) {
        User user = teacher.getUser();
        return user != null ? user.getFirstName() + " " + user.getLastName() : teacher.getEmployeeNumber();
    }

    private PlanningResponseDTO toResponseDTO(Planning p) {
        PlanningResponseDTO dto = new PlanningResponseDTO();
        dto.setId(p.getId());
        dto.setSessionType(p.getSessionType());
        dto.setStatus(p.getStatus());
        dto.setSessionDate(p.getSessionDate());
        dto.setDayOfWeek(p.getSessionDate().getDayOfWeek().name());
        dto.setStartTime(p.getStartTime());
        dto.setEndTime(p.getEndTime());
        dto.setNotes(p.getNotes());
        dto.setTeacherId(p.getTeacher().getId());
        dto.setTeacherName(teacherName(p.getTeacher()));
        dto.setModuleId(p.getModule().getId());
        dto.setModuleCode(p.getModule().getCode());
        dto.setModuleNom(p.getModule().getNom());
        dto.setGroupeId(p.getGroupe().getId());
        dto.setGroupeName(p.getGroupe().getName());
        dto.setFiliereCode(p.getGroupe().getFiliere().getCode());
        dto.setSalleId(p.getSalle().getId());
        dto.setSalleCode(p.getSalle().getCode());
        dto.setSalleName(p.getSalle().getName());
        return dto;
    }
}
