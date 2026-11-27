// PGDI2026-88 / PGDI2026-89 : annonces officielles et veille technologique

export type AnnouncementPriority = 'NORMALE' | 'IMPORTANTE' | 'URGENTE';

export const PRIORITY_LABELS: Record<AnnouncementPriority, string> = {
  NORMALE: 'Normale',
  IMPORTANTE: 'Importante',
  URGENTE: 'Urgente'
};

export interface Announcement {
  id: number;
  title: string;
  content: string;
  priority: AnnouncementPriority;
  pinned: boolean;
  publishedAt: string;
  authorName?: string | null;
  updatedAt?: string | null;
}

// POST et PUT /api/actualites/departement
export interface AnnouncementRequest {
  title: string;
  content: string;
  priority: AnnouncementPriority;
  pinned: boolean;
}

export interface TechArticle {
  title: string;
  summary: string;
  link: string;
  imageUrl?: string | null;
  publishedAt?: string | null;
  source: string;
}

// GET /api/actualites/veille-externe
export interface TechWatchResponse {
  articles: TechArticle[];
  unavailableSources: string[];
  updatedAt?: string | null;
}
