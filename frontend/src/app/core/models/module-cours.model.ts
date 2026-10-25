/** Correspond à ModuleCoursResponseDTO (backend) */
export interface ModuleCours {
  id: number;
  code: string;
  nom: string;
  semestre: string; // "S1", "S2", ...
  volumeHoraire: number | null;
  filiereId: number;
  filiereCode: string;
  filiereNom: string;
  enseignantId: number | null;
  enseignantNomComplet: string | null;
  createdAt: string;
}

/** Correspond à ModuleCoursRequestDTO (backend) */
export interface ModuleCoursRequest {
  code: string;
  nom: string;
  semestre: string;
  volumeHoraire: number | null;
  filiereId: number;
  enseignantId: number | null;
}

/** "S10" -> 10, pour trier les semestres dans le bon ordre */
export function numeroSemestre(semestre: string): number {
  const n = parseInt(semestre.replace(/\D/g, ''), 10);
  return Number.isNaN(n) ? 99 : n;
}
