// PGDI2026-86 / PGDI2026-93 : offre renvoyée par GET /api/stages/offres-externes
export interface JobOffer {
  id: string;
  title: string;
  company: string;
  location: string;
  url: string;
  snippet: string;
  source: 'Jooble' | 'JSearch' | 'Exemple' | string;
  postedDate: string; // AAAA-MM-JJ ou ''
}

export type JobOfferSort = 'recent' | 'title' | 'company';

/** Recherches rapides proposées à l'étudiant (domaine informatique). */
export const QUICK_SEARCHES: { label: string; keyword: string }[] = [
  { label: 'Stage PFE informatique', keyword: 'stage PFE informatique' },
  { label: 'Développement web', keyword: 'stage développeur web' },
  { label: 'Java / Spring', keyword: 'stage java spring' },
  { label: 'Data / IA', keyword: 'stage data science intelligence artificielle' },
  { label: 'DevOps / Cloud', keyword: 'stage devops cloud' },
  { label: 'Cybersécurité', keyword: 'stage cybersécurité' },
  { label: 'Réseaux', keyword: 'stage réseaux informatique' },
  { label: 'Mobile', keyword: 'stage développeur mobile' },
];

/** Lieux proposés dans le champ « Lieu » (pays ou ville, saisie libre possible). */
export const SUGGESTED_LOCATIONS: string[] = [
  'Maroc', 'Oujda', 'Casablanca', 'Rabat', 'Tanger', 'Fès', 'Marrakech', 'Agadir',
  'France', 'Paris', 'Lyon', 'Canada', 'Montréal', 'Belgique', 'Bruxelles', 'Espagne', 'Madrid', 'Télétravail',
];
