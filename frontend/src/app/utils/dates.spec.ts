import { addDays, hhmm, minutes, mondayOf } from './dates';

// PGDI2026-75 : calculs de semaine utilisés par la grille de l'emploi du temps
describe('dates (emploi du temps)', () => {
  it('trouve le lundi de la semaine, y compris un dimanche', () => {
    expect(mondayOf('2026-11-25')).toBe('2026-11-23'); // mercredi
    expect(mondayOf('2026-11-23')).toBe('2026-11-23'); // lundi
    expect(mondayOf('2026-11-29')).toBe('2026-11-23'); // dimanche
  });

  it('ajoute des jours en changeant de mois et d’année', () => {
    expect(addDays('2026-11-30', 1)).toBe('2026-12-01');
    expect(addDays('2026-12-28', 7)).toBe('2027-01-04');
    expect(addDays('2026-11-23', -7)).toBe('2026-11-16');
  });

  it('convertit les heures du serveur', () => {
    expect(minutes('08:30:00')).toBe(510);
    expect(minutes('10:45')).toBe(645);
    expect(hhmm('14:30:00')).toBe('14:30');
  });
});
