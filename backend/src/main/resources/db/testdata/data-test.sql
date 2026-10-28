-- =============================================================================
--  PGDI2026-18 : jeu de données de test (Sprint 1)
--  Aligné sur les entités JPA de la branche develop : User, Filiere, ModuleCours
--  (tables users / filieres / modules générées par Hibernate, ddl-auto=update).
--
--  Chargé automatiquement au démarrage avec le profil "test" :
--      mvn spring-boot:run -Dspring-boot.run.profiles=test
--  (voir application-test.properties). Le script est IDEMPOTENT : à chaque
--  démarrage, les comptes et référentiels de test sont remis dans leur état
--  initial (mots de passe, comptes actifs/désactivés), sans doublon.
--
--  Mot de passe de TOUS les comptes : Test@2026
--  Données fictives — ne jamais charger en production.
-- =============================================================================

-- ---------------------------------------------------------------------
-- Utilisateurs (ids 1 à 11)
--   1 ADMIN | 2 CHEF_DEPARTEMENT | 3-6 ENSEIGNANT | 7-11 ETUDIANT
--   10 = compte désactivé (enabled = 0), 11 = compte supprimé (deleted_at)
-- ---------------------------------------------------------------------
INSERT INTO users (id, email, password_hash, first_name, last_name, phone, role, enabled, deleted_at, created_at, updated_at) VALUES
  ( 1, 'admin@pgdi.test',               '$2a$10$nhUMd52T.QMY.A0My3P8Ye.FK7zNgEsWAX9BUb/XmJtF0vB5mrZfe', 'Amine',   'Ghayout',  '0600000001', 'ADMIN',            1, NULL, NOW(), NOW()),
  ( 2, 'chef.departement@pgdi.test',    '$2a$10$nhUMd52T.QMY.A0My3P8Ye.FK7zNgEsWAX9BUb/XmJtF0vB5mrZfe', 'Karim',   'El Fassi', '0600000002', 'CHEF_DEPARTEMENT', 1, NULL, NOW(), NOW()),
  ( 3, 'n.alami@pgdi.test',             '$2a$10$nhUMd52T.QMY.A0My3P8Ye.FK7zNgEsWAX9BUb/XmJtF0vB5mrZfe', 'Nadia',   'Alami',    '0600000003', 'ENSEIGNANT',       1, NULL, NOW(), NOW()),
  ( 4, 'y.bennani@pgdi.test',           '$2a$10$nhUMd52T.QMY.A0My3P8Ye.FK7zNgEsWAX9BUb/XmJtF0vB5mrZfe', 'Youssef', 'Bennani',  '0600000004', 'ENSEIGNANT',       1, NULL, NOW(), NOW()),
  ( 5, 's.chakir@pgdi.test',            '$2a$10$nhUMd52T.QMY.A0My3P8Ye.FK7zNgEsWAX9BUb/XmJtF0vB5mrZfe', 'Samir',   'Chakir',   '0600000005', 'ENSEIGNANT',       1, NULL, NOW(), NOW()),
  ( 6, 'h.idrissi@pgdi.test',           '$2a$10$nhUMd52T.QMY.A0My3P8Ye.FK7zNgEsWAX9BUb/XmJtF0vB5mrZfe', 'Hanae',   'Idrissi',  '0600000006', 'ENSEIGNANT',       1, NULL, NOW(), NOW()),
  ( 7, 'omar.tazi@etu.pgdi.test',       '$2a$10$nhUMd52T.QMY.A0My3P8Ye.FK7zNgEsWAX9BUb/XmJtF0vB5mrZfe', 'Omar',    'Tazi',     '0611000007', 'ETUDIANT',         1, NULL, NOW(), NOW()),
  ( 8, 'salma.berrada@etu.pgdi.test',   '$2a$10$nhUMd52T.QMY.A0My3P8Ye.FK7zNgEsWAX9BUb/XmJtF0vB5mrZfe', 'Salma',   'Berrada',  '0611000008', 'ETUDIANT',         1, NULL, NOW(), NOW()),
  ( 9, 'mehdi.ouazzani@etu.pgdi.test',  '$2a$10$nhUMd52T.QMY.A0My3P8Ye.FK7zNgEsWAX9BUb/XmJtF0vB5mrZfe', 'Mehdi',   'Ouazzani', '0611000009', 'ETUDIANT',         1, NULL, NOW(), NOW()),
  (10, 'sara.mansouri@etu.pgdi.test',   '$2a$10$nhUMd52T.QMY.A0My3P8Ye.FK7zNgEsWAX9BUb/XmJtF0vB5mrZfe', 'Sara',    'Mansouri', '0611000010', 'ETUDIANT',         0, NULL, NOW(), NOW()),
  (11, 'hamza.naciri@etu.pgdi.test',    '$2a$10$nhUMd52T.QMY.A0My3P8Ye.FK7zNgEsWAX9BUb/XmJtF0vB5mrZfe', 'Hamza',   'Naciri',   '0611000011', 'ETUDIANT',         1, '2026-10-01 10:00:00', NOW(), NOW())
ON DUPLICATE KEY UPDATE
  email = VALUES(email), password_hash = VALUES(password_hash), first_name = VALUES(first_name),
  last_name = VALUES(last_name), phone = VALUES(phone), role = VALUES(role),
  enabled = VALUES(enabled), deleted_at = VALUES(deleted_at), updated_at = NOW();

-- ---------------------------------------------------------------------
-- Filières (ids 1 à 3) — responsable = chef de département (user 2)
-- ---------------------------------------------------------------------
INSERT INTO filieres (id, code, nom, description, niveau, responsable_id, created_at, updated_at) VALUES
  (1, 'M2I-ISI', 'Ingénierie des Systèmes d''Information', 'Parcours génie logiciel et systèmes d''information du Master Ingénierie Informatique.', 'MASTER',  2,    NOW(), NOW()),
  (2, 'M2I-IA',  'Intelligence Artificielle et Données',  'Parcours IA, apprentissage automatique et big data.',                                 'MASTER',  2,    NOW(), NOW()),
  (3, 'LSI-GL',  'Licence Génie Logiciel',                'Licence fondamentale, parcours génie logiciel.',                                      'LICENCE', NULL, NOW(), NOW())
ON DUPLICATE KEY UPDATE
  code = VALUES(code), nom = VALUES(nom), description = VALUES(description),
  niveau = VALUES(niveau), responsable_id = VALUES(responsable_id), updated_at = NOW();

-- ---------------------------------------------------------------------
-- Modules (ids 1 à 8) — M2I-ISI : 4 modules | M2I-IA : 2 | LSI-GL : 2
-- Module 8 sans enseignant (enseignant_id NULL)
-- ---------------------------------------------------------------------
INSERT INTO modules (id, code, nom, semestre, volume_horaire, filiere_id, enseignant_id, created_at, updated_at) VALUES
  (1, 'GL501',  'Génie Logiciel',               'S3', 48, 1, 3,    NOW(), NOW()),
  (2, 'BDA502', 'Bases de Données Avancées',    'S3', 48, 1, 4,    NOW(), NOW()),
  (3, 'JEE503', 'Développement JEE / Spring',   'S3', 48, 1, 5,    NOW(), NOW()),
  (4, 'SEC506', 'Sécurité des Applications',    'S3', 42, 1, 5,    NOW(), NOW()),
  (5, 'ML504',  'Machine Learning',             'S3', 48, 2, 6,    NOW(), NOW()),
  (6, 'DL505',  'Deep Learning',                'S3', 48, 2, 6,    NOW(), NOW()),
  (7, 'ALG101', 'Algorithmique',                'S1', 48, 3, 4,    NOW(), NOW()),
  (8, 'POO201', 'Programmation Orientée Objet', 'S3', 48, 3, NULL, NOW(), NOW())
ON DUPLICATE KEY UPDATE
  code = VALUES(code), nom = VALUES(nom), semestre = VALUES(semestre), volume_horaire = VALUES(volume_horaire),
  filiere_id = VALUES(filiere_id), enseignant_id = VALUES(enseignant_id), updated_at = NOW();
