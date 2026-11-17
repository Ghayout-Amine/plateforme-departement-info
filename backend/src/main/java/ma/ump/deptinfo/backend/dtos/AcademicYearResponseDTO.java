package ma.ump.deptinfo.backend.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AcademicYearResponseDTO {

    private Long id;
    private String label;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean current;
}
