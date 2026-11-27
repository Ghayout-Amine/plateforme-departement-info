package ma.ump.deptinfo.backend.repositories;

import ma.ump.deptinfo.backend.entities.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    // Fil des annonces : épinglées d'abord, puis les plus récentes
    List<Announcement> findAllByOrderByPinnedDescPublishedAtDesc();
}
