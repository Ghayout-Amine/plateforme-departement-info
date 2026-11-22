# PGDI2026-80 — US66 / US21 : Déclaration et validation des stages et conventions PFE

Sprint 3 · Epic E7 Stages, PFE et offres externes · Rida Boubechri  
Statut : **Terminé (Validé de bout en bout le 22/11/2026)**

---

## 1. Ce qui est livré

| Couche | Fichiers |
| --- | --- |
| **Entités JPA** | `entities/Internship.java`, `entities/InternshipStatus.java`, `entities/InternshipType.java` |
| **Accès aux données** | `repositories/InternshipRepository.java`, `repositories/StudentRepository.java` (ajout de `findByUserEmail`) |
| **DTOs** | `dtos/InternshipRequestDTO.java`, `dtos/InternshipResponseDTO.java`, `dtos/InternshipValidationDTO.java` |
| **Règles métier** | `services/InternshipService.java`, `services/AgreementStorageService.java` |
| **API REST** | `controllers/InternshipController.java` (`/api/internships`) |
| **Tests unitaires** | `InternshipServiceTest.java` (12 tests), `AgreementStorageServiceTest.java` (5 tests) |
| **Sécurité & Stockage** | Stockage persistant Docker (`conventions-data:/app/data/conventions`), contrôle des types MIME réels (octets magiques `%PDF-`), nommage sécurisé anti-path-traversal (UUID aléatoire) |
| **Frontend Angular** | `pages/stages/mes-stages/` (dépôt convention étudiant) et `pages/stages/stages-validation/` (validation département), services, modèles, routing |

---

## 2. Règles métier validées

- **Déclaration Étudiant (US66)** :
  - Dépôt obligatoire de la convention au format PDF réel (taille maximale 5 Mo).
  - Unicité du dossier actif : un étudiant ne peut pas avoir deux dossiers de même type en cours (code `409 Conflict`).
  - Statut initial fixé automatiquement à `PENDING_VALIDATION`.
- **Validation Département (US21)** :
  - Seuls les rôles `ADMIN` et `CHEF_DEPARTEMENT` peuvent valider ou refuser les conventions.
  - En cas de refus, un motif textuel explicatif est obligatoirement exigé (`400 Bad Request` si absent).
  - Téléchargement sécurisé du PDF réservé au propriétaire et aux gestionnaires du département.

---

## 3. Endpoints REST

| Méthode | URL | Rôles autorisés | Description |
| --- | --- | --- | --- |
| `POST` | `/api/internships/me` | `ETUDIANT` | Dépose un dossier de stage avec convention PDF |
| `GET` | `/api/internships/me` | `ETUDIANT` | Consulte la liste et l'état de ses stages |
| `GET` | `/api/internships` | `ADMIN`, `CHEF_DEPARTEMENT` | Liste tous les stages avec filtre optionnel par statut |
| `GET` | `/api/internships/{id}` | `ADMIN`, `CHEF_DEPARTEMENT`, `ETUDIANT` | Détail d'un stage (contrôle de propriété) |
| `GET` | `/api/internships/{id}/agreement` | `ADMIN`, `CHEF_DEPARTEMENT`, `ETUDIANT` | Télécharge le binaire PDF de la convention |
| `PUT` | `/api/internships/{id}/validation` | `ADMIN`, `CHEF_DEPARTEMENT` | Valide ou refuse la convention |

---

## 4. Scénario de Test Validé de bout en bout (22/11/2026)

1. Connexion en tant qu'étudiant : `omar.tazi@etu.pgdi.test` / `Test@2026`.
2. Dépôt d'un PFE (*Plateforme Cloud et DevOps*, entreprise *Capgemini Maroc*, fichier PDF convention) ➔ Statut `PENDING_VALIDATION`.
3. Connexion en tant que chef de département : `chef.departement@pgdi.test` / `Test@2026`.
4. Accès à `/stages/validation` ➔ Visualisation de la convention PDF ➔ Validation du dossier.
5. Vérification base MySQL : `status = VALIDATED`, `validated_at` horodaté, `validated_by = 2` (Karim El Fassi).
