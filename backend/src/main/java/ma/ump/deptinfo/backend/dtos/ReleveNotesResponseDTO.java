package ma.ump.deptinfo.backend.dtos;

import java.math.BigDecimal;
import java.util.List;

public record ReleveNotesResponseDTO(
        String studentName,
        String studentNumber,
        String semester,
        List<ModuleResultDTO> modules,
        BigDecimal semesterAverage,
        String decision,
        String mention
) {
    public record ModuleResultDTO(
            String code,
            String name,
            BigDecimal score,
            String status
    ) {
    }
}