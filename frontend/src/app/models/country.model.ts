export interface Country {
  code: string;
  name: string;
}

export const COUNTRIES: Country[] = [
  { code: 'MA', name: 'Maroc' },
  { code: 'FR', name: 'France' },
  { code: 'CA', name: 'Canada' },
  { code: 'BE', name: 'Belgique' },
  { code: 'DE', name: 'Allemagne' },
  { code: 'ES', name: 'Espagne' },
  { code: 'CH', name: 'Suisse' },
  { code: 'US', name: 'États-Unis' },
  { code: 'GB', name: 'Royaume-Uni' },
  { code: 'AE', name: 'Émirats Arabes Unis' },
  { code: 'QA', name: 'Qatar' },
  { code: 'SA', name: 'Arabie Saoudite' },
  { code: 'TN', name: 'Tunisie' },
  { code: 'DZ', name: 'Algérie' },
  { code: 'SN', name: 'Sénégal' },
  { code: 'CI', name: "Côte d'Ivoire" },
  { code: 'LU', name: 'Luxembourg' },
  { code: 'NL', name: 'Pays-Bas' },
  { code: 'IT', name: 'Italie' },
  { code: 'TR', name: 'Turquie' },
  { code: 'PT', name: 'Portugal' },
  { code: 'SE', name: 'Suède' },
  { code: 'NO', name: 'Norvège' },
  { code: 'XX', name: 'Autre pays' },
];

export function getCountryName(code?: string | null): string {
  if (!code) return '';
  const found = COUNTRIES.find((c) => c.code.toUpperCase() === code.toUpperCase());
  return found ? found.name : code;
}
