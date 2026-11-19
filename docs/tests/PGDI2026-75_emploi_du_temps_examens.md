# PGDI2026-75 — US36 / US49 : emploi du temps et calendrier des examens

Sprint 3 · Epic E6 Gestion pédagogique · 8 points · Mohammed Mrabet

> En tant qu'étudiant et enseignant, je souhaite consulter mon emploi du temps hebdomadaire
> et le planning des sessions d'examens par salle et groupe.

## Ce qui est livré

| Couche | Fichiers |
| --- | --- |
| Entités JPA | `Planning`, `Examen`, énumérations `SessionType` (CM, TD, TP), `PlanningStatus` (PLANNED, CANCELLED), `ExamType` (CC, TP, FINAL, RATTRAPAGE) |
| Accès aux données | `PlanningRepository`, `ExamenRepository` (requêtes JPQL de chevauchement, filtres, « mon emploi du temps ») |
| DTO | `PlanningRequestDTO`, `PlanningResponseDTO`, `ExamenRequestDTO`, `ExamenResponseDTO` |
| Règles métier | `PlanningService`, `ExamenService` |
| API REST | `PlanningController` (`/api/plannings`), `ExamenController` (`/api/examens`) |
| Tests unitaires | `PlanningServiceTest` (21 tests), `ExamenServiceTest` (16 tests), sans base de données (Mockito) |
| Tests de requêtes | `PlanningExamenRepositoryTest` (4 tests) : les requêtes JPQL exécutées sur H2 |
| Jeu de test | `data-test.sql` : année 2026-2027, 4 enseignants, 3 étudiants inscrits, 12 séances dans la **semaine en cours**, 4 examens la semaine suivante |
| Tests d'intégration | Collection Postman, dossier `07 - Emploi du temps et examens` (43 requêtes) |
| Angular | pages `emploi-du-temps/` (grille hebdomadaire + formulaire de séance) et `examens/` (calendrier + formulaire), chargées à la demande (lazy loading) |
| Conception | `docs/conception/sprint3/` : cas d'utilisation, classes, tables, séquence |

## Modèle

- **Planning** (table `plannings`) : une séance réunit un **enseignant**, un **module**, un **groupe** et une **salle**, une **date** et un créneau `startTime` → `endTime`, un type (CM, TD, TP) et un état (PLANNED, CANCELLED).
  Simplification par rapport à la conception v2 : l'affectation `TeachingAssignment` est portée directement par la séance (un seul formulaire au lieu de deux).
- **Examen** (table `examens`) : titre, type, date et horaire, **module**, **groupe**, **année universitaire**, et **une ou plusieurs salles** (table d'association `exam_salles`, comme dans le schéma V1).
- Les deux entités héritent de `BaseEntity` (id, dates de création et de modification).

## Règles métier

| Règle | Séance | Examen | Code HTTP |
| --- | --- | --- | --- |
| Enseignant, module, groupe, salle, année existants | ✔ | ✔ | 404 |
| L'heure de fin suit l'heure de début | ✔ | ✔ | 400 |
| Le module appartient à la filière du groupe | ✔ | ✔ | 400 |
| La salle est en service | ✔ | ✔ (chaque salle) | 400 |
| Assez de places : salle ≥ effectif du groupe / total des salles ≥ effectif | ✔ | ✔ | 400 |
| La date tombe dans l'année universitaire | | ✔ | 400 |
| **Salle libre** sur le créneau (séances actives et examens) | ✔ | ✔ | 409 |
| **Enseignant libre** sur le créneau | ✔ | | 409 |
| **Groupe libre** sur le créneau (séances actives et examens) | ✔ | ✔ | 409 |

- Deux créneaux se chevauchent si l'un commence avant la fin de l'autre **et** finit après son début : `10:30-12:30` peut suivre `08:30-10:30`.
- Une séance **annulée** reste visible (barrée) mais ne bloque plus le créneau.
- Le message 409 liste **tous** les conflits trouvés, par exemple :
  `Conflit le 2026-11-23 : la salle S12 est déjà occupée 08:30-10:30 (GL501) ; l'enseignant Nadia Alami a déjà cours 08:30-10:30 (GL501)`.
- **Série hebdomadaire** (`POST /api/plannings/serie`, `weeks` de 1 à 16) : tout ou rien, un seul conflit annule toute la série.

## Endpoints

| Méthode | URL | Rôles | Réponses |
| --- | --- | --- | --- |
| GET | `/api/plannings?semaine=2026-11-23&groupeId=1` (ou `teacherId`, `salleId`, ou `from` + `to`) | tout utilisateur connecté | 200, 400 |
| GET | `/api/plannings/me?semaine=...` | ETUDIANT (son groupe), ENSEIGNANT (ses cours) | 200, 403 |
| GET | `/api/plannings/{id}` | connecté | 200, 404 |
| POST | `/api/plannings` | ADMIN, CHEF_DEPARTEMENT | 201, 400, 404, 409 |
| POST | `/api/plannings/serie` | ADMIN, CHEF_DEPARTEMENT | 201, 400, 404, 409 |
| PUT | `/api/plannings/{id}` | ADMIN, CHEF_DEPARTEMENT | 200, 400, 404, 409 |
| PATCH | `/api/plannings/{id}/annuler` | ADMIN, CHEF_DEPARTEMENT | 200, 404 |
| DELETE | `/api/plannings/{id}` | ADMIN, CHEF_DEPARTEMENT | 204, 404 |
| GET | `/api/examens?from=&to=&groupeId=&moduleId=&salleId=` | connecté | 200, 400 |
| GET | `/api/examens/me` | ETUDIANT (son groupe), ENSEIGNANT (ses modules) | 200, 403 |
| GET | `/api/examens/{id}` | connecté | 200, 404 |
| POST | `/api/examens` | ADMIN, CHEF_DEPARTEMENT | 201, 400, 404, 409 |
| PUT | `/api/examens/{id}` | ADMIN, CHEF_DEPARTEMENT | 200, 400, 404, 409 |
| DELETE | `/api/examens/{id}` | ADMIN, CHEF_DEPARTEMENT | 204, 404 |

Sans jeton : 401. Rôle non autorisé : 403. Swagger UI : étiquettes **Emploi du temps** et **Examens**.

Exemples de corps :

```json
{ "teacherId": 1, "moduleId": 1, "groupeId": 1, "salleId": 3, "sessionType": "TD",
  "sessionDate": "2026-11-23", "startTime": "08:30", "endTime": "10:30", "notes": "Apporter son ordinateur" }

{ "title": "Examen final Bases de Données", "examType": "FINAL", "moduleId": 2, "groupeId": 1,
  "salleIds": [4, 5], "examDate": "2026-11-25", "startTime": "14:00", "endTime": "16:00" }
```

`academicYearId` est facultatif pour un examen : l'année marquée « en cours » est utilisée.
Ajouter `"weeks": 12` au corps d'une séance et l'envoyer à `/api/plannings/serie` pour la répéter 12 semaines.

## Comment tester

1. Backend avec le jeu de test : `cd backend` puis `./mvnw spring-boot:run -Dspring-boot.run.profiles=test`
   (ou `docker compose up --build` à la racine).
2. Tests unitaires et couverture : `./mvnw test` → rapport JaCoCo dans `backend/target/site/jacoco/index.html`.
3. Swagger : http://localhost:8080/swagger-ui.html → « Authorize » avec le jeton de `chef.departement@pgdi.test` / `Test@2026`.
4. Postman : dossiers `01 - Authentification` puis `07 - Emploi du temps et examens` (le dossier 07 nettoie ce qu'il crée).
5. Frontend : `cd frontend` puis `npm start` →
   - http://localhost:4200/emploi-du-temps (chef : par groupe, enseignant ou salle ; clic dans la grille = nouvelle séance)
   - http://localhost:4200/examens
   - connecté en `omar.tazi@etu.pgdi.test` : « Mon emploi du temps » et « Mes examens » ;
     en `n.alami@pgdi.test` : ses cours et les examens de son module GL501.

## Comptes de démonstration (mot de passe `Test@2026`)

| Compte | Rôle | Ce qu'il voit |
| --- | --- | --- |
| `chef.departement@pgdi.test` | CHEF_DEPARTEMENT | tout, et peut planifier |
| `n.alami@pgdi.test` | ENSEIGNANT | ses 3 séances de la semaine, les examens de GL501 |
| `omar.tazi@etu.pgdi.test` | ETUDIANT | les 6 séances et les 2 examens du Groupe 1 - ISI |
