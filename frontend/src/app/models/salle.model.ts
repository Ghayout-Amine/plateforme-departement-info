// PGDI2026-44 (US21) : salles du département — même forme que SalleResponseDTO côté Spring Boot
export type RoomType = 'AMPHI' | 'CLASSROOM' | 'LAB' | 'OTHER';

export const ROOM_TYPE_LABELS: Record<RoomType, string> = {
  AMPHI: 'Amphithéâtre',
  CLASSROOM: 'Salle de cours',
  LAB: 'Salle de TP',
  OTHER: 'Autre'
};

export const ROOM_TYPES: RoomType[] = ['AMPHI', 'CLASSROOM', 'LAB', 'OTHER'];

export interface Salle {
  id: number;
  code: string;
  name: string;
  building?: string;
  capacity: number;
  roomType: RoomType;
  isActive: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface SalleRequest {
  code: string;
  name: string;
  building?: string;
  capacity: number;
  roomType: RoomType;
  isActive?: boolean;
}
