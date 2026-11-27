# PGDI2026-66 — US47-48 : Saisie des notes et gestion des absences

Sprint 3 · Epic E6 Gestion pédagogique · 6 points · Bilal Amrani  
Statut : **Terminé (Validé de bout en bout le 18/11/2026)**

---

## 1. Ce qui est livré

| Couche | Fichiers |
| --- | --- |
| **Entités JPA** | `entities/Grade.java`, `entities/GradeStatus.java`, `entities/GradeType.java`, `entities/Absence.java` |
| **Accès aux données** | `repositories/GradeRepository.java`, `repositories/AbsenceRepository.java` |
| **DTOs** | `dtos/GradeCreateRequestDTO.java`, `dtos/GradeResponseDTO.java`, `dtos/GradeAverageResponseDTO.java`, `dtos/AbsenceCreateRequestDTO.java`, `dtos/AbsenceResponseDTO.java`, `dtos/AbsenceJustifyRequestDTO.java` |
| **Règles métier** | `services/GradeService.java`, `services/GradeAverageCalculator.java`, `services/AbsenceService.java`, `services/EnrollmentEligibilityValidator.java` |
| **API REST** | `controllers/GradeController.java`, `controllers/AbsenceController.java` |
| **Sécurité & Contrôle d'accès** | Contrôles par rôles (`ROLE_ENSEIGNANT`, `ROLE_ETUDIANT`, `ROLE_ADMIN`, `ROLE_CHEF_DEPARTEMENT`) et vérification d'assignation enseignant-module |
| **Frontend Angular** | Pages `grades/` (saisie enseignant brouillon/publication + consultation étudiant) et `absences/` (saisie enseignant + justification + consultation étudiant), services, modèles, routing |

---

## 2. Règles métier & Règles d'éligibilité

- **Saisie de note (US47)** :
  - Seul l'enseignant affecté au module (`module.enseignant.id == teacher.user_id`) peut saisir ou publier une note.
  - La note commence obligatoirement à l'état `DRAFT` (brouillon) et ne peut être vue par l'étudiant qu'après passage explicite à `PUBLISHED`.
  - L'étudiant doit avoir une inscription active (`Enrollment.status == ACTIVE`) dans l'année universitaire courante et dans la filière du module (`EnrollmentEligibilityValidator`).
- **Consultation des notes (US48)** :
  - L'étudiant consulte uniquement **ses** notes avec le statut `PUBLISHED`.
- **Absences** :
  - L'enseignant saisit les absences horodatées par séance pour ses étudiants.
  - Justification possible avec motif (motif stocké et statut basculé en justifié).
  - L'étudiant consulte la liste complète de ses absences et leur statut de justification.

---

## 3. Endpoints REST

| Méthode | URL | Rôles autorisés | Description |
| --- | --- | --- | --- |
| `POST` | `/api/grades` | `ENSEIGNANT` | Enregistre une note en brouillon (`DRAFT`) |
| `POST` | `/api/grades/{id}/publish` | `ENSEIGNANT` | Publie officiellement une note (`PUBLISHED`) |
| `GET` | `/api/grades/my` | `ETUDIANT` | Liste des notes publiées de l'étudiant connecté |
| `GET` | `/api/grades/students/{id}/average` | `ADMIN`, `CHEF_DEPARTEMENT` | Moyenne pondérée des notes publiées de l'étudiant |
| `POST` | `/api/absences` | `ENSEIGNANT` | Saisie d'une absence sur une séance |
| `PUT` | `/api/absences/{id}/justify` | `ADMIN`, `CHEF_DEPARTEMENT`, `ENSEIGNANT` | Justification d'une absence avec motif |
| `GET` | `/api/absences/my` | `ETUDIANT` | Liste des absences de l'étudiant connecté |

---

## 4. Données de test & Validation de bout en bout

Les tests manuels et d'intégration ont été validés sur les données réelles suivantes :
- **Enseignant** : Nadia Alami (`n.alami@pgdi.test` / `Test@2026`, ID: `3`)
- **Étudiant** : Omar Tazi (`omar.tazi@etu.pgdi.test` / `Test@2026`, ID: `3`)
- **Module** : `GL-301` - Génie Logiciel et DevOps (ID: `1`)
- **Année Académique** : `2026-2027` (ID: `1`, active)

### Scénario exécuté avec succès :
1. Connexion en tant que `n.alami@pgdi.test`.
2. Saisie d'une note de `16.5 / 20` (Contrôle continu, coeff 1) pour l'étudiant `3` sur le module `1`.
3. Enregistrement en `DRAFT` ➔ Publication ➔ Statut `PUBLISHED`.
4. Connexion en tant que `omar.tazi@etu.pgdi.test` sur `/grades` ➔ Note affichée dans la table de l'étudiant.
