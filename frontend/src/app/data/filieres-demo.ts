import { Filiere } from '../models/filiere.model';

// Filières de démonstration (mêmes données que backend/.../db/testdata/data-test.sql).
// Utilisées par la vitrine publique tant que GET /api/filieres n'est pas ouvert aux visiteurs
// non connectés ou si le backend est injoignable.
export const FILIERES_DEMO: Filiere[] = [
  {
    id: 1,
    code: 'M2I-ISI',
    nom: "Ingénierie des Systèmes d'Information",
    description: "Parcours génie logiciel et systèmes d'information du Master Ingénierie Informatique.",
    niveau: 'MASTER',
  },
  {
    id: 2,
    code: 'M2I-IA',
    nom: 'Intelligence Artificielle et Données',
    description: 'Parcours IA, apprentissage automatique et big data.',
    niveau: 'MASTER',
  },
  {
    id: 3,
    code: 'LSI-GL',
    nom: 'Licence Génie Logiciel',
    description: 'Licence fondamentale, parcours génie logiciel.',
    niveau: 'LICENCE',
  },
];
