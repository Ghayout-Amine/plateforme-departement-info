/** Correspond à FiliereResponseDTO (backend) */
export interface Filiere {
  id: number;
  code: string;
  nom: string;
  description: string | null;
  niveau: string;
  responsableId: number | null;
  responsableNomComplet: string | null;
  createdAt: string;
}

/** Correspond à FiliereRequestDTO (backend) */
export interface FiliereRequest {
  code: string;
  nom: string;
  description: string | null;
  niveau: string;
  responsableId: number | null;
}

export const NIVEAUX: { value: string; label: string; semestres: number }[] = [
  { value: 'LICENCE', label: 'Licence', semestres: 6 },
  { value: 'MASTER', label: 'Master', semestres: 4 },
  { value: 'CYCLE_INGENIEUR', label: 'Cycle ingénieur', semestres: 6 },
];

export function libelleNiveau(niveau: string): string {
  return NIVEAUX.find((n) => n.value === niveau)?.label ?? niveau;
}

/** Semestres proposés pour une filière : ["S1", "S2", ...] selon son niveau */
export function semestresDuNiveau(niveau: string): string[] {
  const nb = NIVEAUX.find((n) => n.value === niveau)?.semestres ?? 6;
  return Array.from({ length: nb }, (_, i) => `S${i + 1}`);
}
