import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { JobOfferService } from '../../../services/job-offer.service';
import { JobOffer, JobOfferSort, QUICK_SEARCHES, SUGGESTED_LOCATIONS } from '../../../models/job-offer.model';
import { filterOffers, relativeDate, sortOffers, sourcesOf } from './offres-externes.utils';

const PAGE_SIZE = 10;

// PGDI2026-93 : recherche et filtrage des offres de stage des API externes (Jooble / JSearch, PGDI2026-86).
// La recherche interroge le serveur (mis en cache 30 min) ; les filtres et le tri s'appliquent ensuite
// dans le navigateur, sans nouvel appel. Les critères sont gardés dans l'URL (?q=...&lieu=...).
@Component({
  selector: 'app-offres-externes',
  imports: [FormsModule],
  templateUrl: './offres-externes.html',
  styleUrl: './offres-externes.css',
})
export class OffresExternes implements OnInit {
  private readonly jobOfferService = inject(JobOfferService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly quickSearches = QUICK_SEARCHES;
  readonly locations = SUGGESTED_LOCATIONS;
  readonly relativeDate = relativeDate;

  // critères envoyés au serveur
  keyword = 'stage informatique';
  location = 'Maroc';

  // filtres locaux
  readonly textFilter = signal('');
  readonly sourceFilter = signal('');
  readonly sort = signal<JobOfferSort>('recent');
  readonly shown = signal(PAGE_SIZE);

  readonly offers = signal<JobOffer[]>([]);
  readonly loading = signal(false);
  readonly error = signal('');
  readonly searched = signal(false);
  readonly lastSearch = signal('');

  readonly sources = computed(() => sourcesOf(this.offers()));
  readonly filtered = computed(() =>
    sortOffers(filterOffers(this.offers(), this.textFilter(), this.sourceFilter()), this.sort()));
  readonly visible = computed(() => this.filtered().slice(0, this.shown()));
  readonly onlyExamples = computed(() => this.offers().length > 0 && this.offers().every((o) => o.source === 'Exemple'));

  ngOnInit(): void {
    const q = this.route.snapshot.queryParamMap;
    this.keyword = q.get('q') ?? this.keyword;
    this.location = q.get('lieu') ?? this.location;
    this.search();
  }

  search(): void {
    const keyword = this.keyword.trim();
    const location = this.location.trim();
    this.loading.set(true);
    this.error.set('');
    this.shown.set(PAGE_SIZE);
    this.router.navigate([], { queryParams: { q: keyword || null, lieu: location || null }, replaceUrl: true });

    this.jobOfferService.search(keyword, location).subscribe({
      next: (list) => {
        this.offers.set(list);
        this.sourceFilter.set('');
        this.searched.set(true);
        this.lastSearch.set([keyword || 'stage informatique', location || 'Maroc'].join(' · '));
        this.loading.set(false);
      },
      error: (err) => {
        this.offers.set([]);
        this.searched.set(true);
        this.error.set(
          err?.status === 400 && err?.error?.message
            ? err.error.message
            : 'Le service des offres externes ne répond pas. Réessayez dans quelques instants.');
        this.loading.set(false);
      },
    });
  }

  quickSearch(keyword: string): void {
    this.keyword = keyword;
    this.search();
  }

  resetFilters(): void {
    this.textFilter.set('');
    this.sourceFilter.set('');
    this.sort.set('recent');
  }

  showMore(): void {
    this.shown.update((n) => n + PAGE_SIZE);
  }

  initials(company: string): string {
    const words = (company || '?').replace(/\(.*?\)/g, '').trim().split(/\s+/);
    return words.slice(0, 2).map((w) => w[0]?.toUpperCase() ?? '').join('') || '?';
  }
}
