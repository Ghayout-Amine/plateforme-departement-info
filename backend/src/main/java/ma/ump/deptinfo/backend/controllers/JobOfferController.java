package ma.ump.deptinfo.backend.controllers;

import ma.ump.deptinfo.backend.dtos.JobOfferDto;
import ma.ump.deptinfo.backend.services.JobOfferService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * PGDI2026-86 : offres de stage des API externes (Jooble / JSearch), pour tout utilisateur connecté.
 * GET /api/stages/offres-externes?keyword=stage%20informatique&location=Maroc
 */
@RestController
@RequestMapping("/api/stages/offres-externes")
public class JobOfferController {

    private final JobOfferService jobOfferService;

    public JobOfferController(JobOfferService jobOfferService) {
        this.jobOfferService = jobOfferService;
    }

    @GetMapping
    public ResponseEntity<List<JobOfferDto>> getOffers(
            @RequestParam(required = false, defaultValue = "stage informatique") String keyword,
            @RequestParam(required = false, defaultValue = "Maroc") String location) {
        return ResponseEntity.ok(jobOfferService.searchOffers(keyword, location));
    }
}
