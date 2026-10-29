# PGDI2026-18 — Jeu de données SQL de test et collection Postman d'intégration

Sprint 1 · 4 points · aligné sur la branche `develop` au 28/10/2026 (entités `User`, `Filiere`, `ModuleCours` ; controllers Auth, Users, Filières, Modules).

## Fichiers ajoutés au dépôt

| Fichier | Rôle |
|---|---|
| `backend/src/main/resources/db/testdata/data-test.sql` | Jeu de données : 11 utilisateurs, 3 filières, 8 modules. Idempotent. |
| `backend/src/main/resources/application-test.properties` | Profil `test` : base `pgdi_test_db` et chargement automatique du jeu de données |
| `tests/api/PGDI2026-18.postman_collection.json` | 68 requêtes : 54 de non-régression (dossiers 00 à 05) et 14 d'anomalies (dossier 99) |
| `tests/api/PGDI2026-local.postman_environment.json` | `baseUrl`, comptes de test, mots de passe |
| `docs/tests/PGDI2026-18_tests_integration.md` | Ce document |
| `docs/tests/cible-conception/` | Version complète, écrite pour le schéma V1 et les diagrammes SQ1 à SQ11 (voir §6) |

## 1. Lancer le backend avec les données de test

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=test
```

Ce qui se passe au démarrage :
1. Hibernate crée ou complète les tables dans `pgdi_test_db` (`ddl-auto=update`).
2. Spring exécute `data-test.sql`, grâce à `spring.jpa.defer-datasource-initialization=true`.
3. `DataInitializer` crée le compte de démo `test@pgdi.ma`.

Le script est **idempotent** : à chaque redémarrage, les comptes de test retrouvent leur état initial (mot de passe, compte actif ou désactivé). Les données que vous créez vous-mêmes ne sont pas touchées.

## 2. Comptes de test

Mot de passe commun : **`Test@2026`**.

| id | Email | Rôle (`users.role`) | Remarque |
|---|---|---|---|
| 1 | admin@pgdi.test | ADMIN | |
| 2 | chef.departement@pgdi.test | CHEF_DEPARTEMENT | responsable des filières 1 et 2 |
| 3 à 6 | n.alami@ · y.bennani@ · s.chakir@ · h.idrissi@ `pgdi.test` | ENSEIGNANT | |
| 7 à 9 | omar.tazi@ · salma.berrada@ · mehdi.ouazzani@ `etu.pgdi.test` | ETUDIANT | |
| 10 | sara.mansouri@etu.pgdi.test | ETUDIANT | **désactivé** (`enabled = 0`) |
| 11 | hamza.naciri@etu.pgdi.test | ETUDIANT | **supprimé logiquement** (`deleted_at`) |
| auto | test@pgdi.ma / `Test1234!` | ENSEIGNANT | créé par `DataInitializer` |

Filières : `1 M2I-ISI` (4 modules : GL501, BDA502, JEE503, SEC506), `2 M2I-IA` (2 modules : ML504, DL505), `3 LSI-GL` (2 modules : ALG101, POO201 ; aucun responsable). Le module 8 (POO201) n'a pas d'enseignant.

## 3. Lancer la collection

**Postman** : importer les deux fichiers de `tests/api/`, choisir l'environnement « PGDI2026 - local », puis *Run collection* sur les dossiers 00 à 05.

**Newman** :

```bash
npm install -g newman
newman run tests/api/PGDI2026-18.postman_collection.json \
  -e tests/api/PGDI2026-local.postman_environment.json \
  --folder "00 - Disponibilité" --folder "01 - Authentification" \
  --folder "02 - Protection des endpoints (JWT)" --folder "03 - Utilisateurs (/api/users)" \
  --folder "04 - Filières (/api/filieres)" --folder "05 - Modules (/api/modules)"
```

Résultat attendu : **54 requêtes, 222 assertions, 0 échec**. La collection est rejouable sans redémarrer : elle utilise des emails et codes aléatoires, supprime ce qu'elle crée, et réactive le compte qu'elle désactive.

| Dossier | Ce qui est vérifié |
|---|---|
| 00 | `GET /api/auth/test` |
| 01 | Connexion des 6 profils : JWT valide, `sub` égal à l'email, expiration d'environ 1 h. Échecs (mot de passe faux, email inconnu, compte désactivé, compte supprimé) avec le même message générique. Corps vide, JSON mal formé |
| 02 | `/api/test/protected` avec jeton valide, sans jeton, falsifié, mal formé ou expiré. `/api/users`, `/api/filieres` et `/api/modules` sans jeton. Un jeton émis avant la désactivation du compte doit être refusé, puis accepté après réactivation |
| 03 | CRUD `/api/users` : liste, détail, création (hash BCrypt vérifié par une connexion), rôle par défaut ENSEIGNANT, validation `@NotBlank`/`@Email`, rôle inconnu, modification, suppression. Aucun hash exposé dans les réponses |
| 04 | CRUD `/api/filieres` : responsable et `responsableNomComplet`, validation |
| 05 | CRUD `/api/modules`, `/api/modules/filiere/{id}`, liens filière/enseignant, validation, suppression du module puis de la filière |

## 4. Anomalies détectées (dossier 99, à créer dans Jira)

Ces tests décrivent le comportement **attendu** par le cahier des charges et le backlog. Ils sont rouges sur `develop` aujourd'hui et passeront au vert une fois chaque anomalie corrigée.

```bash
newman run tests/api/PGDI2026-18.postman_collection.json -e tests/api/PGDI2026-local.postman_environment.json \
  --folder "01 - Authentification" --folder "99 - Anomalies à corriger"
```

| # | Anomalie | Aujourd'hui | Attendu | Responsable suggéré |
|---|---|---|---|---|
| A1 | Requête sans jeton | 403 | 401 (`HttpStatusEntryPoint(UNAUTHORIZED)`) | Membre 1 |
| A2 | `/api/auth/me` accessible sans jeton (`/api/auth/**` en `permitAll`) | 200 | 401 | Membre 1 |
| A3 | La connexion ne renvoie pas le rôle, donc le frontend ne peut pas rediriger selon le rôle | `{token, tokenType}` | rôle dans la réponse ou dans le JWT | Membre 1 |
| A4 | Un étudiant peut créer un compte ADMIN : aucun contrôle de rôle, tout le monde a `ROLE_USER` | 201 | 403 | Membre 1 |
| A5 | Un enseignant peut lister tous les comptes | 200 | 403 | Membre 1 |
| A6 | Un étudiant peut supprimer une filière | 204 | 403 | Membres 1 et 4 |
| A7 | Email déjà utilisé : `RuntimeException` non gérée | 500 | 409 | Membre 1 (gestion globale des erreurs) |
| A8 | Code de filière déjà utilisé | 500 | 409 | Membre 4 |
| A9 / A10 | Utilisateur ou filière inexistant | 500 | 404 | Membres 1 et 4 |
| A11 | Module rattaché à une filière inexistante | 500 | 404 ou 422 | Membre 4 |
| A12 | Suppression d'une filière qui a des modules (violation de clé étrangère) | 500 | 409 | Membre 4 |
| A13 | Erreurs de validation non détaillées par champ (story commune du Sprint 1) | 400 sans détail | 400 avec un message par champ | Membre 1 (`@RestControllerAdvice`) |

A7 à A13 se corrigent presque tous d'un coup : exceptions métier (`NotFoundException`, `ConflictException`) plus un `@RestControllerAdvice` global.

## 5. Vérifications effectuées

- `data-test.sql` a été exécuté sur **MySQL 8.0.46**, sur le schéma que Hibernate génère pour les trois entités. Il passe deux fois de suite sans erreur ni doublon, et remet bien les comptes dans leur état initial.
- Je n'ai pas pu compiler le backend dans mon environnement, car Maven Central y est bloqué. La collection a donc tourné avec Newman contre une **simulation fidèle du code `develop`**, qui reproduit la sécurité, les controllers, les services, la validation et les erreurs 500 sur cette base MySQL. Résultat : dossiers 00 à 05 à **222/222, deux fois de suite**, et dossier 99 rouge sur les 13 anomalies. **Vérifié le 28/10/2026 sur le vrai backend (branche develop, XAMPP/MariaDB) : 54 requêtes, 222 assertions, 0 échec.**
- Seule incertitude : le statut renvoyé sans jeton (403 par défaut dans Spring Security). Les tests de non-régression acceptent 401 et 403, et l'anomalie A1 exige 401.

## 6. Point à trancher par le chef de projet : deux schémas divergents

`db/migration/V1__init_schema.sql` (MCD/MLD, PGDI2026-11) n'est **pas exécuté** : Flyway n'est pas dans le `pom.xml`, et c'est Hibernate (`ddl-auto=update`) qui crée les tables à partir des entités. Or les deux ne correspondent pas :

| | V1 (conception) | Entités `develop` |
|---|---|---|
| Rôle de l'utilisateur | tables `roles` et `user_roles` (`ROLE_STUDENT`…, avec `ROLE_COMPANY`) | colonne `users.role` (enum ADMIN, CHEF_DEPARTEMENT, ENSEIGNANT, ETUDIANT ; pas d'entreprise) |
| Filière | `filieres.name`, `formation_id` obligatoire | `filieres.nom`, `niveau`, `responsable_id` |
| Modules | `course_modules` et `filiere_modules` (relation N-N) | `modules` (un seul `filiere_id`, `semestre`) |

Si une base a été créée avec V1 puis démarrée par Hibernate, les insertions échoueront (par exemple, `formation_id` est NOT NULL). Il faut choisir **une** source de vérité avant le Sprint 2 :
- soit adopter Flyway avec V1 et aligner les entités ;
- soit abandonner V1 et mettre à jour le MLD.

Le dossier `docs/tests/cible-conception/` contient un jeu de données (30 tables) et une collection de 94 requêtes pour SQ1 à SQ11, écrits pour V1 et prêts à servir si l'équipe choisit la première option.

Remarque mineure : `app.init-test-user=truew` dans `application.properties` contient une faute de frappe. Cette propriété n'est de toute façon lue par aucun code.
