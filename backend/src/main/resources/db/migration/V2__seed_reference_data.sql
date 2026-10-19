-- =============================================================================
--  V2 : données de référence (rôles applicatifs)
--  Les permissions fines (table permissions / role_permissions) sont à définir
--  par le Membre 1 (Sécurité) selon la matrice rôles/fonctionnalités.
-- =============================================================================
INSERT INTO roles (name, description) VALUES
  ('ROLE_STUDENT',         'Étudiant : espace personnel, candidatures, consultation des notes et absences'),
  ('ROLE_TEACHER',         'Enseignant : ressources, notes, absences, groupes'),
  ('ROLE_DEPARTMENT_HEAD', 'Responsable du département : gestion pédagogique, stages/PFE, statistiques'),
  ('ROLE_ADMIN',           'Administrateur : utilisateurs, rôles, permissions, journaux, paramètres'),
  ('ROLE_COMPANY',         'Entreprise : profil, offres de stage/PFE, candidatures');

INSERT INTO system_parameters (param_key, param_value, description) VALUES
  ('department.name',  'Département Informatique – Faculté des Sciences d''Oujda', 'Nom affiché sur la partie publique'),
  ('department.email', NULL, 'Email de contact du département'),
  ('department.phone', NULL, 'Téléphone de contact du département'),
  ('department.address', NULL, 'Adresse du département');
