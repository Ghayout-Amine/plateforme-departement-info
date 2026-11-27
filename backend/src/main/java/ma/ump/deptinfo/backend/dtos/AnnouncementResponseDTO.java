package ma.ump.deptinfo.backend.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ma.ump.deptinfo.backend.entities.AnnouncementPriority;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnnouncementResponseDTO {

    private Long id;
    private String title;
    private String content;
    private AnnouncementPriority priority;
    private boolean pinned;
    private LocalDateTime publishedAt;
    private String authorName;
    private LocalDateTime updatedAt;
}
