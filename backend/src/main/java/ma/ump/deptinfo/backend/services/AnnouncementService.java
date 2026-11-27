package ma.ump.deptinfo.backend.services;

import ma.ump.deptinfo.backend.dtos.AnnouncementRequestDTO;
import ma.ump.deptinfo.backend.dtos.AnnouncementResponseDTO;
import ma.ump.deptinfo.backend.entities.Announcement;
import ma.ump.deptinfo.backend.entities.AnnouncementPriority;
import ma.ump.deptinfo.backend.entities.User;
import ma.ump.deptinfo.backend.exceptions.ResourceNotFoundException;
import ma.ump.deptinfo.backend.repositories.AnnouncementRepository;
import ma.ump.deptinfo.backend.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * PGDI2026-89 : annonces officielles du département.
 * Lecture publique ; publication, modification, épinglage et suppression par l'admin
 * ou le chef de département. Les annonces épinglées restent en tête du fil.
 */
@Service
@Transactional
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final UserRepository userRepository;

    public AnnouncementService(AnnouncementRepository announcementRepository, UserRepository userRepository) {
        this.announcementRepository = announcementRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<AnnouncementResponseDTO> getAll() {
        return announcementRepository.findAllByOrderByPinnedDescPublishedAtDesc().stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public AnnouncementResponseDTO getById(Long id) {
        return toResponseDTO(findAnnouncement(id));
    }

    // L'auteur est l'utilisateur connecté (email tiré du token JWT)
    public AnnouncementResponseDTO create(AnnouncementRequestDTO dto, String authorEmail) {
        User author = userRepository.findByEmail(authorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable : " + authorEmail));

        Announcement announcement = new Announcement();
        announcement.setAuthor(author);
        apply(announcement, dto);
        if (announcement.getPublishedAt() == null) {
            announcement.setPublishedAt(LocalDateTime.now());
        }
        return toResponseDTO(announcementRepository.save(announcement));
    }

    public AnnouncementResponseDTO update(Long id, AnnouncementRequestDTO dto) {
        Announcement announcement = findAnnouncement(id);
        LocalDateTime previousDate = announcement.getPublishedAt();
        apply(announcement, dto);
        if (announcement.getPublishedAt() == null) {
            announcement.setPublishedAt(previousDate);
        }
        return toResponseDTO(announcementRepository.save(announcement));
    }

    // Épingler / désépingler
    public AnnouncementResponseDTO togglePin(Long id) {
        Announcement announcement = findAnnouncement(id);
        announcement.setPinned(!announcement.isPinned());
        return toResponseDTO(announcementRepository.save(announcement));
    }

    public void delete(Long id) {
        announcementRepository.delete(findAnnouncement(id));
    }

    // ---------- méthodes utilitaires ----------

    private void apply(Announcement announcement, AnnouncementRequestDTO dto) {
        announcement.setTitle(dto.getTitle().trim());
        announcement.setContent(dto.getContent().trim());
        announcement.setPriority(dto.getPriority() != null ? dto.getPriority() : AnnouncementPriority.NORMALE);
        announcement.setPinned(dto.isPinned());
        announcement.setPublishedAt(dto.getPublishedAt());
    }

    private Announcement findAnnouncement(Long id) {
        return announcementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Annonce introuvable avec l'id : " + id));
    }

    private AnnouncementResponseDTO toResponseDTO(Announcement a) {
        User author = a.getAuthor();
        String authorName = author == null ? null
                : (author.getFirstName() + " " + author.getLastName()).trim();
        return new AnnouncementResponseDTO(
                a.getId(),
                a.getTitle(),
                a.getContent(),
                a.getPriority(),
                a.isPinned(),
                a.getPublishedAt(),
                authorName,
                a.getUpdatedAt()
        );
    }
}
