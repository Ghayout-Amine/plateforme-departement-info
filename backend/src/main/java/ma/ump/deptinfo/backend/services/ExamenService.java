package ma.ump.deptinfo.backend.services;

import ma.ump.deptinfo.backend.dtos.ExamenRequestDTO;
import ma.ump.deptinfo.backend.dtos.ExamenResponseDTO;
import ma.ump.deptinfo.backend.entities.*;
import ma.ump.deptinfo.backend.exceptions.BadRequestException;
import ma.ump.deptinfo.backend.exceptions.ConflictException;
import ma.ump.deptinfo.backend.exceptions.ResourceNotFoundException;
import ma.ump.deptinfo.backend.repositories.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Calendrier des examens (PGDI2026-75, US49).
 * Règles appliquées à chaque planification ou modification d'examen :
 * - le module, le groupe, l'année et chaque salle doivent exister (404) ;
 * - l'heure de fin doit suivre l'heure de début (400) ;
 * - le module doit appartenir à la filière du groupe (400) ;
 * - la date doit tomber dans l'année universitaire (400) ;
 * - les salles doivent être en service et offrir assez de places au total (400) ;
 * - ni les salles ni le groupe ne doivent être pris sur ce créneau,
 *   par un autre examen ou par une séance de cours (409).
 */
@Service
@Transactional
public class ExamenService {

    static final LocalDate NO_LOWER_BOUND = LocalDate.of(2000, 1, 1);
    static final LocalDate NO_UPPER_BOUND = LocalDate.of(2100, 12, 31);

    private final ExamenRepository examenRepository;
    private final PlanningRepository planningRepository;
    private final ModuleCoursRepository moduleCoursRepository;
    private final GroupeRepository groupeRepository;
    private final SalleRepository salleRepository;
    private final AcademicYearRepository academicYearRepository;
    private final UserRepository userRepository;

    public ExamenService(ExamenRepository examenRepository,
                         PlanningRepository planningRepository,
                         ModuleCoursRepository moduleCoursRepository,
                         GroupeRepository groupeRepository,
                         SalleRepository salleRepository,
                         AcademicYearRepository academicYearRepository,
                         UserRepository userRepository) {
        this.examenRepository = examenRepository;
        this.planningRepository = planningRepository;
        this.moduleCoursRepository = moduleCoursRepository;
        this.groupeRepository = groupeRepository;
        this.salleRepository = salleRepository;
        this.academicYearRepository = academicYearRepository;
        this.userRepository = userRepository;
    }

    // ---------- consultation ----------

    // Liste chronologique, filtres facultatifs (période, groupe, module, salle)
    @Transactional(readOnly = true)
    public List<ExamenResponseDTO> getExamens(LocalDate from, LocalDate to, Long groupeId, Long moduleId, Long salleId) {
        LocalDate[] range = range(from, to);
        return examenRepository.search(range[0], range[1], groupeId, moduleId, salleId).stream()
                .map(this::toResponseDTO)
                .toList();
    }

    /**
     * "Mes examens" : un étudiant voit les examens de son groupe,
     * un enseignant ceux des modules dont il est responsable.
     */
    @Transactional(readOnly = true)
    public List<ExamenResponseDTO> getMyExamens(String email, LocalDate from, LocalDate to) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable : " + email));
        LocalDate[] range = range(from, to);
        List<Examen> examens = switch (user.getRole() == null ? Role.ADMIN : user.getRole()) {
            case ETUDIANT -> examenRepository.findForStudent(email, range[0], range[1], EnrollmentStatus.ACTIVE);
            case ENSEIGNANT -> examenRepository.findForTeacher(email, range[0], range[1]);
            default -> throw new BadRequestException("« Mes examens » est réservé aux étudiants et aux enseignants");
        };
        return examens.stream().map(this::toResponseDTO).toList();
    }

    @Transactional(readOnly = true)
    public ExamenResponseDTO getExamenById(Long id) {
        return toResponseDTO(findExamen(id));
    }

    // ---------- planification ----------

    public ExamenResponseDTO createExamen(ExamenRequestDTO dto) {
        Examen examen = new Examen();
        apply(examen, dto, null);
        return toResponseDTO(examenRepository.save(examen));
    }

    public ExamenResponseDTO updateExamen(Long id, ExamenRequestDTO dto) {
        Examen examen = findExamen(id);
        apply(examen, dto, id);
        return toResponseDTO(examenRepository.save(examen));
    }

    public void deleteExamen(Long id) {
        examenRepository.delete(findExamen(id));
    }

    // ---------- règles métier ----------

    private void apply(Examen examen, ExamenRequestDTO dto, Long excludeId) {
        ModuleCours module = moduleCoursRepository.findById(dto.getModuleId())
                .orElseThrow(() -> new ResourceNotFoundException("Module introuvable avec l'id : " + dto.getModuleId()));
        Groupe groupe = groupeRepository.findById(dto.getGroupeId())
                .orElseThrow(() -> new ResourceNotFoundException("Groupe introuvable avec l'id : " + dto.getGroupeId()));
        AcademicYear year = resolveAcademicYear(dto.getAcademicYearId());
        Set<Salle> salles = resolveSalles(dto.getSalleIds());

        LocalDate date = dto.getExamDate();
        LocalTime start = dto.getStartTime();
        LocalTime end = dto.getEndTime();
        if (!end.isAfter(start)) {
            throw new BadRequestException("L'heure de fin (" + end + ") doit être après l'heure de début (" + start + ")");
        }
        if (!module.getFiliere().getId().equals(groupe.getFiliere().getId())) {
            throw new BadRequestException("Le module " + module.getCode() + " n'est pas enseigné dans la filière "
                    + groupe.getFiliere().getCode() + " du groupe \"" + groupe.getName() + "\"");
        }
        if (date.isBefore(year.getStartDate()) || date.isAfter(year.getEndDate())) {
            throw new BadRequestException("La date " + date + " est en dehors de l'année universitaire "
                    + year.getLabel() + " (" + year.getStartDate() + " → " + year.getEndDate() + ")");
        }
        int places = salles.stream().mapToInt(s -> s.getCapacity() != null ? s.getCapacity() : 0).sum();
        if (groupe.getCapacity() != null && places < groupe.getCapacity()) {
            throw new BadRequestException("Les salles choisies offrent " + places + " places pour "
                    + groupe.getCapacity() + " étudiants : ajoutez une salle");
        }

        List<String> conflicts = findConflicts(date, start, end, groupe, salles, excludeId);
        if (!conflicts.isEmpty()) {
            throw new ConflictException("Conflit le " + date + " : " + String.join(" ; ", conflicts));
        }

        examen.setTitle(dto.getTitle().trim());
        examen.setExamType(dto.getExamType());
        examen.setModule(module);
        examen.setGroupe(groupe);
        examen.setAcademicYear(year);
        examen.getSalles().clear();
        examen.getSalles().addAll(salles);
        examen.setExamDate(date);
        examen.setStartTime(start);
        examen.setEndTime(end);
        examen.setNotes(dto.getNotes());
    }

    /** Conflits avec les autres examens et avec les séances de cours actives. */
    List<String> findConflicts(LocalDate date, LocalTime start, LocalTime end,
                               Groupe groupe, Set<Salle> salles, Long excludeId) {
        List<String> conflicts = new ArrayList<>();
        for (Examen other : examenRepository.findOverlapping(date, start, end, excludeId)) {
            String slot = other.getStartTime() + "-" + other.getEndTime() + " (examen " + other.getTitle() + ")";
            for (Salle salle : salles) {
                if (other.getSalles().stream().anyMatch(s -> s.getId().equals(salle.getId()))) {
                    conflicts.add("la salle " + salle.getCode() + " est déjà réservée " + slot);
                }
            }
            if (other.getGroupe().getId().equals(groupe.getId())) {
                conflicts.add("le groupe \"" + groupe.getName() + "\" passe déjà un examen " + slot);
            }
        }
        for (Planning seance : planningRepository.findOverlapping(date, start, end, null, PlanningStatus.PLANNED)) {
            String slot = seance.getStartTime() + "-" + seance.getEndTime() + " (cours " + seance.getModule().getCode() + ")";
            for (Salle salle : salles) {
                if (seance.getSalle().getId().equals(salle.getId())) {
                    conflicts.add("la salle " + salle.getCode() + " est occupée " + slot);
                }
            }
            if (seance.getGroupe().getId().equals(groupe.getId())) {
                conflicts.add("le groupe \"" + groupe.getName() + "\" a cours " + slot);
            }
        }
        return conflicts;
    }

    // Année indiquée, ou à défaut l'année universitaire en cours
    private AcademicYear resolveAcademicYear(Long academicYearId) {
        if (academicYearId != null) {
            return academicYearRepository.findById(academicYearId)
                    .orElseThrow(() -> new ResourceNotFoundException("Année universitaire introuvable avec l'id : " + academicYearId));
        }
        return academicYearRepository.findFirstByCurrentTrue()
                .orElseThrow(() -> new BadRequestException("Aucune année universitaire en cours : précisez academicYearId"));
    }

    // Chaque salle doit exister (404) et être en service (400) ; les doublons sont ignorés
    private Set<Salle> resolveSalles(List<Long> salleIds) {
        Set<Salle> salles = new LinkedHashSet<>();
        for (Long salleId : new LinkedHashSet<>(salleIds)) {
            Salle salle = salleRepository.findById(salleId)
                    .orElseThrow(() -> new ResourceNotFoundException("Salle introuvable avec l'id : " + salleId));
            if (!salle.isActive()) {
                throw new BadRequestException("La salle " + salle.getCode() + " est hors service");
            }
            salles.add(salle);
        }
        return salles;
    }

    private static LocalDate[] range(LocalDate from, LocalDate to) {
        LocalDate start = from != null ? from : NO_LOWER_BOUND;
        LocalDate end = to != null ? to : NO_UPPER_BOUND;
        if (end.isBefore(start)) {
            throw new BadRequestException("La date de fin doit suivre la date de début");
        }
        return new LocalDate[]{start, end};
    }

    private Examen findExamen(Long id) {
        return examenRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Examen introuvable avec l'id : " + id));
    }

    private ExamenResponseDTO toResponseDTO(Examen e) {
        ExamenResponseDTO dto = new ExamenResponseDTO();
        dto.setId(e.getId());
        dto.setTitle(e.getTitle());
        dto.setExamType(e.getExamType());
        dto.setExamDate(e.getExamDate());
        dto.setStartTime(e.getStartTime());
        dto.setEndTime(e.getEndTime());
        dto.setNotes(e.getNotes());
        dto.setModuleId(e.getModule().getId());
        dto.setModuleCode(e.getModule().getCode());
        dto.setModuleNom(e.getModule().getNom());
        dto.setGroupeId(e.getGroupe().getId());
        dto.setGroupeName(e.getGroupe().getName());
        dto.setGroupeCapacity(e.getGroupe().getCapacity());
        dto.setFiliereCode(e.getGroupe().getFiliere().getCode());
        dto.setAcademicYearId(e.getAcademicYear().getId());
        dto.setAcademicYearLabel(e.getAcademicYear().getLabel());
        List<Salle> salles = e.getSalles().stream()
                .sorted((a, b) -> a.getCode().compareTo(b.getCode()))
                .toList();
        dto.setSalleIds(salles.stream().map(Salle::getId).toList());
        dto.setSalleCodes(salles.stream().map(Salle::getCode).toList());
        dto.setTotalCapacity(e.totalCapacity());
        return dto;
    }
}
