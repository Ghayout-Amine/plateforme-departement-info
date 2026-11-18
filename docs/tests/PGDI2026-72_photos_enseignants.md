# PGDI2026-72 — US16 : Photo de profil des enseignants & Workflow Avatar

Sprint 3 · Epic E6 Gestion pédagogique · 2 points · Bilal Amrani  
Statut : **Terminé (Validé de bout en bout le 18/11/2026)**

---

## 1. Ce qui est livré

| Couche | Fichiers & Composants |
| --- | --- |
| **Backend Service** | `TeacherPhotoService.java` (gestion MIME, stockage fichier sécurisé, limite 5 Mo) |
| **API REST** | `TeacherController.java` (`POST /api/teachers/{id}/photo`, `GET /api/teachers/{id}/photo`), `UserController.java` (`POST /api/users/profile/photo`, `GET /api/users/{id}/photo`) |
| **Sécurité** | `SecurityConfig.java` : accès public en lecture `GET` aux photos pour affichage direct dans les balises `<img>` du navigateur sans 401 |
| **Persistance Docker** | Volume persistant `photos-data:/app/data/photos` dans `docker-compose.yml` et configuration permissions `appuser:appgroup` dans `backend/Dockerfile` |
| **Reverse Proxy Nginx** | `frontend/nginx.conf` : ajout de `client_max_body_size 10M;` pour éliminer l'erreur HTTP 413 |
| **Frontend Formulaire** | `teacher-form/` : zone d'upload dédiée avec prévisualisation dynamique en direct |
| **Frontend Profil** | `user-profile/` : self-service au survol de l'avatar (`📷 Modifier`), téléversement direct sans quitter la page |
| **Frontend Listes** | `teacher-list/`, `user-list/` : affichage de la photo avec fallback dynamique par initiales colorées en cas d'absence d'image |

---

## 2. Workflows Utilisateur Implémentés

```
                      Création Enseignant (Admin)
                                  │
         ┌────────────────────────┴────────────────────────┐
         ▼                                                 ▼
     Option 1                                          Option 2
Upload direct dès la création                      Création sans photo
   (/teachers/new)                               (Avatar initiales par défaut)
                                                           │
                                ┌──────────────────────────┴──────────────────────────┐
                                ▼                                                     ▼
                     Connexion Enseignant                              L'Admin consulte la fiche
                                │                                           (/teachers/:id)
                                ▼                                                     │
                   L'Enseignant va sur Mon Profil                                     ▼
                              (/profile)                               L'Admin téléverse / modifie
                                │                                       la photo à tout moment
                                ▼
                           Self-Service :
                   Clique sur l'avatar pour uploader
                          sa propre photo
```

---

## 3. Endpoints REST

| Méthode | URL | Accès | Description |
| --- | --- | --- | --- |
| `POST` | `/api/teachers/{id}/photo` | `ADMIN`, `CHEF_DEPARTEMENT` | Téléverse la photo d'un enseignant |
| `GET` | `/api/teachers/{id}/photo` | Public (`permitAll`) | Récupère le binaire de la photo de l'enseignant |
| `POST` | `/api/users/profile/photo` | Connecté (`authenticated`) | Téléversement Self-Service de sa propre photo |
| `GET` | `/api/users/{id}/photo` | Public (`permitAll`) | Récupère la photo d'un utilisateur |

---

## 4. Tests & Validation

- **Upload direct lors de l'ajout** : validé sur `/teachers/new`.
- **Upload en self-service** : validé sur `/profile`.
- **Résilience** : gestion du quota max 5 Mo avec alerte explicite et gestion d'erreur `(error)` masquant les images manquantes au profit de l'avatar initiales.
- **Persistance** : validé après redémarrage des conteneurs via le volume Docker nommé `photos-data`.
