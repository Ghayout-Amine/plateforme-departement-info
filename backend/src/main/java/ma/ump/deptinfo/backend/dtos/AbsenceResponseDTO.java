
package ma.ump.deptinfo.backend.dtos;

import ma.ump.deptinfo.backend.entities.AbsenceStatus;

import java.time.LocalDate;

public record AbsenceResponseDTO(

        Long id,

        Long studentId,

        Long moduleId,

        LocalDate date,

        String sessionTime,

        Boolean justified,

        String motif,

        AbsenceStatus status

) {

    // Compatibilite avec les anciens appels.

    public AbsenceResponseDTO(
            Long id,
            Long studentId,
            Long moduleId,
            LocalDate date,
            String sessionTime,
            Boolean justified,
            String motif) {

        this(
                id,
                studentId,
                moduleId,
                date,
                sessionTime,
                justified,
                motif,
                Boolean.TRUE.equals(justified)
                        ? AbsenceStatus.JUSTIFIED
                        : AbsenceStatus.UNJUSTIFIED
        );
    }
}
