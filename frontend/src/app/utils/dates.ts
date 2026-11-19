// PGDI2026-75 : petites fonctions de dates en heure locale (format AAAA-MM-JJ, sans fuseau horaire)

export function toIso(date: Date): string {
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  return `${date.getFullYear()}-${m}-${d}`;
}

export function fromIso(iso: string): Date {
  const [y, m, d] = iso.split('-').map(Number);
  return new Date(y, m - 1, d);
}

export function addDays(iso: string, days: number): string {
  const date = fromIso(iso);
  date.setDate(date.getDate() + days);
  return toIso(date);
}

// Lundi de la semaine qui contient la date
export function mondayOf(iso: string): string {
  const date = fromIso(iso);
  const shift = (date.getDay() + 6) % 7;
  return addDays(iso, -shift);
}

export function today(): string {
  return toIso(new Date());
}

// "08:30:00" -> 510 minutes depuis minuit
export function minutes(time: string): number {
  const [h, m] = time.split(':').map(Number);
  return h * 60 + m;
}

// "08:30:00" -> "08:30"
export function hhmm(time: string): string {
  return time.substring(0, 5);
}

export function longDate(iso: string): string {
  return fromIso(iso).toLocaleDateString('fr-FR', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' });
}

export function shortDate(iso: string): string {
  return fromIso(iso).toLocaleDateString('fr-FR', { day: 'numeric', month: 'short' });
}
