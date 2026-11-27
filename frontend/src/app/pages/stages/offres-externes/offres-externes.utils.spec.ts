import { JobOffer } from '../../../models/job-offer.model';
import { filterOffers, matchesText, relativeDate, sortOffers, sourcesOf } from './offres-externes.utils';

const offer = (p: Partial<JobOffer>): JobOffer => ({
  id: p.id ?? '1', title: p.title ?? 'Stage', company: p.company ?? '', location: p.location ?? '',
  url: 'https://x', snippet: p.snippet ?? '', source: p.source ?? 'Jooble', postedDate: p.postedDate ?? '',
});

describe('PGDI2026-93 : filtrage des offres externes', () => {
  const offers = [
    offer({ id: 'a', title: 'Stage Développeur Java', company: 'Acme', location: 'Casablanca', postedDate: '2026-11-10' }),
    offer({ id: 'b', title: 'Stage Data Science', company: 'Beta', location: 'Rabat', source: 'JSearch', postedDate: '2026-11-22' }),
    offer({ id: 'c', title: 'Alternance DevOps', company: '', location: 'Oujda', snippet: 'Docker et Kubernetes' }),
  ];

  it('ignore les accents et la casse, tous les mots doivent correspondre', () => {
    expect(matchesText(offers[0], 'developpeur JAVA')).toBe(true);
    expect(matchesText(offers[0], 'developpeur python')).toBe(false);
    expect(matchesText(offers[2], 'kubernetes oujda')).toBe(true);
  });

  it('filtre par texte et par source', () => {
    expect(filterOffers(offers, '', 'JSearch').map((o) => o.id)).toEqual(['b']);
    expect(filterOffers(offers, 'stage', '').map((o) => o.id)).toEqual(['a', 'b']);
  });

  it('trie par date (sans date à la fin), par titre ou par entreprise', () => {
    expect(sortOffers(offers, 'recent').map((o) => o.id)).toEqual(['b', 'a', 'c']);
    expect(sortOffers(offers, 'title').map((o) => o.id)).toEqual(['c', 'b', 'a']);
    expect(sortOffers(offers, 'company').map((o) => o.id)).toEqual(['a', 'b', 'c']);
  });

  it('affiche une date relative lisible', () => {
    const today = new Date(2026, 10, 24);
    expect(relativeDate('2026-11-24', today)).toBe("aujourd'hui");
    expect(relativeDate('2026-11-23', today)).toBe('hier');
    expect(relativeDate('2026-11-20', today)).toBe('il y a 4 jours');
    expect(relativeDate('2026-11-10', today)).toBe('il y a 2 semaines');
    expect(relativeDate('2026-09-01', today)).toBe('le 01/09/2026');
    expect(relativeDate('', today)).toBe('');
  });

  it('liste les sources présentes', () => {
    expect(sourcesOf(offers)).toEqual(['JSearch', 'Jooble']);
  });
});
