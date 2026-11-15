// PGDI2026-44 (US22) : groupes d'étudiants par filière — même forme que GroupeResponseDTO
export interface Groupe {
  id: number;
  name: string;
  capacity?: number;
  filiereId: number;
  filiereCode: string;
  filiereNom: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface GroupeRequest {
  name: string;
  capacity?: number;
  filiereId: number;
}
