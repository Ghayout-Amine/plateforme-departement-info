import { JobOffer, JobOfferSort } from '../../../models/job-offer.model';

// PGDI2026-93 : filtrage et tri côté navigateur des offres déjà reçues (aucun nouvel appel à l'API)

/** Minuscules sans accents, pour comparer « Développeur » et « developpeur ». */
export function normalize(text: string | null | undefined): string {
  return (text ?? '').normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase().trim();
}

/** Tous les mots saisis doivent apparaître dans le titre, l'entreprise, le lieu ou le résumé. */
export function matchesText(offer: JobOffer, query: string): boolean {
  const words = normalize(query).split(/\s+/).filter(Boolean);
  if (!words.length) return true;
  const haystack = normalize(`${offer.title} ${offer.company} ${offer.location} ${offer.snippet}`);
  return words.every((w) => haystack.includes(w));
}

export function filterOffers(offers: JobOffer[], query: string, source: string): JobOffer[] {
  return offers.filter((o) => (!source || o.source === source) && matchesText(o, query));
}

export function sortOffers(offers: JobOffer[], sort: JobOfferSort): JobOffer[] {
  const copy = [...offers];
  switch (sort) {
    case 'title':
      return copy.sort((a, b) => a.title.localeCompare(b.title, 'fr'));
    case 'company':
      // entreprise non précisée : à la fin
      return copy.sort((a, b) => (!a.company ? 1 : 0) - (!b.company ? 1 : 0) || a.company.localeCompare(b.company, 'fr'));
    default:
      // les plus récentes d'abord ; sans date, à la fin
      return copy.sort((a, b) => (b.postedDate || '').localeCompare(a.postedDate || ''));
  }
}

/** « aujourd'hui », « il y a 3 jours », « il y a 2 semaines »… à partir d'une date AAAA-MM-JJ. */
export function relativeDate(isoDate: string, today: Date = new Date()): string {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(isoDate ?? '')) return '';
  const [y, m, d] = isoDate.split('-').map(Number);
  const start = Date.UTC(today.getFullYear(), today.getMonth(), today.getDate());
  const days = Math.round((start - Date.UTC(y, m - 1, d)) / 86_400_000);
  if (days <= 0) return "aujourd'hui";
  if (days === 1) return 'hier';
  if (days < 7) return `il y a ${days} jours`;
  if (days < 30) return `il y a ${Math.floor(days / 7)} semaine${days >= 14 ? 's' : ''}`;
  return `le ${String(d).padStart(2, '0')}/${String(m).padStart(2, '0')}/${y}`;
}

/** Sources présentes dans les résultats, pour le filtre « Source ». */
export function sourcesOf(offers: JobOffer[]): string[] {
  return [...new Set(offers.map((o) => o.source).filter(Boolean))].sort();
}
