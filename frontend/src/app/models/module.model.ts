export interface ModuleCours {
  id: number;
  code: string;
  nom: string;
  semestre: string;
  volumeHoraire?: number;
  filiereId: number;
  filiereCode?: string;
  filiereNom?: string;
  enseignantId?: number;
  enseignantNomComplet?: string;
  createdAt?: string;
}

export interface ModuleCoursCreatePayload {
  code: string;
  nom: string;
  semestre: string;
  volumeHoraire?: number;
  filiereId: number;
  enseignantId?: number;
}
