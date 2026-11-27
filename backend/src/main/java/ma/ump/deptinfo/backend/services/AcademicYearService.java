package ma.ump.deptinfo.backend.services;

import ma.ump.deptinfo.backend.dtos.AcademicYearRequestDTO;
import ma.ump.deptinfo.backend.dtos.AcademicYearResponseDTO;
import ma.ump.deptinfo.backend.entities.AcademicYear;
import ma.ump.deptinfo.backend.exceptions.BadRequestException;
import ma.ump.deptinfo.backend.exceptions.ConflictException;
import ma.ump.deptinfo.backend.exceptions.ResourceNotFoundException;
import ma.ump.deptinfo.backend.repositories.AcademicYearRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Années universitaires, nécessaires aux inscriptions (PGDI2026-60).
 * - le libellé suit le format AAAA-AAAA avec deux années consécutives (ex. 2026-2027) ;
 * - la date de fin est après la date de début ;
 * - une seule année est "en cours" à la fois.
 */
@Service
@Transactional
public class AcademicYearService {

    private final AcademicYearRepository academicYearRepository;

    public AcademicYearService(AcademicYearRepository academicYearRepository) {
        this.academicYearRepository = academicYearRepository;
    }

    @Transactional(readOnly = true)
    public List<AcademicYearResponseDTO> getAll() {
        return academicYearRepository.findAllByOrderByStartDateDesc().stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public AcademicYearResponseDTO getCurrent() {
        return academicYearRepository.findFirstByCurrentTrue()
                .map(this::toResponseDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Aucune année universitaire n'est marquée en cours"));
    }

    public AcademicYearResponseDTO create(AcademicYearRequestDTO dto) {
        String label = dto.getLabel().trim();

        String[] years = label.split("-");
        if (Integer.parseInt(years[1]) != Integer.parseInt(years[0]) + 1) {
            throw new BadRequestException("Le libellé doit contenir deux années consécutives, ex. 2026-2027");
        }
        if (!dto.getEndDate().isAfter(dto.getStartDate())) {
            throw new BadRequestException("La date de fin doit être après la date de début");
        }
        if (academicYearRepository.findByLabel(label).isPresent()) {
            throw new ConflictException("L'année universitaire " + label + " existe déjà");
        }

        AcademicYear year = new AcademicYear();
        year.setLabel(label);
        year.setStartDate(dto.getStartDate());
        year.setEndDate(dto.getEndDate());

        if (dto.isCurrent()) {
            unsetCurrentYears();
            year.setCurrent(true);
        }

        return toResponseDTO(academicYearRepository.save(year));
    }

    // Marquer une année comme "en cours" (et retirer ce statut à l'ancienne)
    public AcademicYearResponseDTO setCurrent(Long id) {
        AcademicYear year = academicYearRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Année universitaire introuvable avec l'id : " + id));
        unsetCurrentYears();
        year.setCurrent(true);
        return toResponseDTO(academicYearRepository.save(year));
    }

    private void unsetCurrentYears() {
        List<AcademicYear> currentYears = academicYearRepository.findByCurrentTrue();
        currentYears.forEach(y -> y.setCurrent(false));
        academicYearRepository.saveAll(currentYears);
    }

    private AcademicYearResponseDTO toResponseDTO(AcademicYear year) {
        return new AcademicYearResponseDTO(
                year.getId(),
                year.getLabel(),
                year.getStartDate(),
                year.getEndDate(),
                year.isCurrent()
        );
    }
}
