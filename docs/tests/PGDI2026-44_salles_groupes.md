# PGDI2026-44 — US21-22 : gestion des salles et des groupes

Sprint 2 · Epic E6 Gestion pédagogique · 5 points · Mohammed Mrabet

## Ce qui est livré

| Couche | Fichiers |
| --- | --- |
| Entités JPA | `entities/Salle.java`, `entities/RoomType.java`, `entities/Groupe.java` |
| Accès aux données | `repositories/SalleRepository.java`, `repositories/GroupeRepository.java` |
| DTO | `dtos/SalleRequestDTO.java`, `dtos/SalleResponseDTO.java`, `dtos/GroupeRequestDTO.java`, `dtos/GroupeResponseDTO.java` |
| Règles métier | `services/SalleService.java`, `services/GroupeService.java` |
| API REST | `controllers/SalleController.java`, `controllers/GroupeController.java` |
| Tests unitaires | `SalleServiceTest.java` (9 tests), `GroupeServiceTest.java` (10 tests) |
| Jeu de test | `db/testdata/data-test.sql` : 6 salles (dont 1 hors service) et 5 groupes |
| Tests d'intégration | Collection Postman, dossier `06 - Salles et groupes` (24 requêtes) |
| Angular | pages `salles/` (liste + formulaire) et `groupes/` (liste + formulaire), services, modèles, routes, liens du menu |
| Conception | `docs/conception/sprint2/` : classes, cas d'utilisation, séquence, tables |

## Modèle (conforme à l'UML v02, CL_2 Structure pédagogique)

- **Salle** : `code` (unique, stocké en majuscules), `name`, `building`, `capacity` (> 0), `roomType` (`AMPHI`, `CLASSROOM`, `LAB`, `OTHER`), `isActive`.
- **Groupe** : `name` (unique **dans une filière**), `capacity` (facultatif), `@ManyToOne` vers `Filiere`.
- Prévu pour plus tard : le lien `Groupe` → `AcademicYear` et l'affectation des étudiants (`Enrollment`), qui dépendent d'entités pas encore codées.

## Endpoints

| Méthode | URL | Rôles | Réponses |
| --- | --- | --- | --- |
| GET | `/api/salles` (`?type=LAB`, `?active=false`) | tout utilisateur connecté | 200 |
| GET | `/api/salles/{id}` | connecté | 200, 404 |
| POST | `/api/salles` | ADMIN, CHEF_DEPARTEMENT | 201, 400, 409 |
| PUT | `/api/salles/{id}` | ADMIN, CHEF_DEPARTEMENT | 200, 400, 404, 409 |
| DELETE | `/api/salles/{id}` | ADMIN, CHEF_DEPARTEMENT | 204, 404 |
| GET | `/api/groupes` (`?filiereId=1`) | connecté | 200, 404 si la filière n'existe pas |
| GET | `/api/groupes/filiere/{filiereId}` | connecté | 200, 404 |
| GET | `/api/groupes/{id}` | connecté | 200, 404 |
| POST | `/api/groupes` | ADMIN, CHEF_DEPARTEMENT | 201, 400, 404 (filière), 409 (nom déjà pris dans la filière) |
| PUT | `/api/groupes/{id}` | ADMIN, CHEF_DEPARTEMENT | 200, 400, 404, 409 |
| DELETE | `/api/groupes/{id}` | ADMIN, CHEF_DEPARTEMENT | 204, 404 |

Sans jeton : 401. Avec un rôle non autorisé : 403. Les deux contrôleurs apparaissent dans Swagger UI sous les étiquettes **Salles** et **Groupes**.

Exemples de corps :

```json
{ "code": "AMPHI_C", "name": "Amphithéâtre Ibn Rochd", "building": "Bloc Central", "capacity": 200, "roomType": "AMPHI", "isActive": true }
{ "name": "Groupe 1 - GL", "capacity": 30, "filiereId": 3 }
```

## Comment tester

1. Backend avec le jeu de test : `cd backend` puis `./mvnw spring-boot:run -Dspring-boot.run.profiles=test`
2. Tests unitaires : `./mvnw test` (dont `SalleServiceTest` et `GroupeServiceTest`)
3. Swagger : http://localhost:8080/swagger-ui.html → « Authorize » avec le jeton de `chef.departement@pgdi.test` / `Test@2026`
4. Postman : lancer la collection, dossier `06 - Salles et groupes` (il nettoie ce qu'il crée)
5. Frontend : `cd frontend` puis `npm start` → http://localhost:4200/salles et http://localhost:4200/groupes
