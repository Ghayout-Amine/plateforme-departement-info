export interface Filiere {
  id: number;
  code: string;
  nom: string;
  description?: string;
  niveau: string;
  responsableId?: number;
  responsableNomComplet?: string;
  createdAt?: string;
}

export interface FiliereCreatePayload {
  code: string;
  nom: string;
  description?: string;
  niveau: string;
  responsableId?: number;
}
