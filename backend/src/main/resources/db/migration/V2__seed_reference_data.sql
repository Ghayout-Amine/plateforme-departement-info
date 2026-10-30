-- =============================================================================
--  V2 : données de référence
--  v02 (28/10/2026) : ROLE_COMPANY supprimé (les entreprises n'ont plus de compte) ;
--  ajout des sources d'offres externes (module 6) et des flux d'actualités (module 7).
--  Les permissions fines (table permissions / role_permissions) sont à définir
--  par le Membre 1 (Sécurité) selon la matrice rôles/fonctionnalités.
-- =============================================================================
INSERT INTO roles (name, description) VALUES
  ('ROLE_STUDENT',         'Étudiant : espace personnel, recherche d''offres, suivi de candidatures, déclaration de stage'),
  ('ROLE_TEACHER',         'Enseignant : ressources, notes, absences, groupes, encadrement'),
  ('ROLE_DEPARTMENT_HEAD', 'Responsable du département : gestion pédagogique, sources d''offres, stages/PFE, flux d''actualités, statistiques'),
  ('ROLE_ADMIN',           'Administrateur : utilisateurs, rôles, permissions, journaux, paramètres, supervision des API');

INSERT INTO system_parameters (param_key, param_value, description) VALUES
  ('department.name',  'Département Informatique – Faculté des Sciences d''Oujda', 'Nom affiché sur la partie publique'),
  ('department.email', NULL, 'Email de contact du département'),
  ('department.phone', NULL, 'Téléphone de contact du département'),
  ('department.address', NULL, 'Adresse du département'),
  ('offers.retention.days', '45', 'Durée de conservation d''une offre non revue avant passage à EXPIRED'),
  ('offers.quota.alert.percent', '80', 'Seuil d''alerte de consommation de quota (en %)'),
  ('articles.retention.days', '30', 'Durée de conservation des articles externes non promus');

-- Sources d'offres. Les clés NE sont PAS stockées ici : api_key_ref = nom de la variable
-- d'environnement lue par le backend. Quotas = valeurs publiées au 28/10/2026, à reconfirmer.
INSERT INTO job_sources (code, name, website_url, access_mode, enabled, api_key_ref, sync_interval_minutes, default_keywords, default_locations, quota_limit, quota_period, attribution_text) VALUES
  ('JOOBLE',        'Jooble',                     'https://ma.jooble.org',         'CACHED', TRUE,  'JOOBLE_API_KEY',        360, 'stage informatique,PFE informatique,développeur,data,développeur java', 'Maroc', NULL, NULL, 'Offres via Jooble'),
  ('CAREERJET',     'Careerjet (Optioncarrière)', 'https://www.optioncarriere.ma', 'LIVE',   TRUE,  'CAREERJET_API_KEY',     NULL, NULL, NULL, NULL, NULL, 'Offres via Careerjet'),
  ('JSEARCH',       'JSearch (Google for Jobs)',  'https://www.openwebninja.com',  'CACHED', TRUE,  'JSEARCH_RAPIDAPI_KEY',  1440, 'stage informatique Maroc,PFE développeur Maroc', 'Morocco', 200, 'MONTH', 'Offres via JSearch (LinkedIn, Indeed, Glassdoor…)'),
  ('FRANCE_TRAVAIL','France Travail',             'https://francetravail.io',      'CACHED', FALSE, 'FRANCE_TRAVAIL_CLIENT', 360, 'développeur,informatique', 'France', 10, 'MINUTE', 'Source : France Travail'),
  ('ADZUNA',        'Adzuna',                     'https://www.adzuna.com',        'CACHED', FALSE, 'ADZUNA_APP_KEY',        720, 'software developer,data engineer', 'fr,gb', 250, 'DAY', 'Jobs by Adzuna'),
  ('DEPARTEMENT',   'Département informatique',   NULL,                            'MANUAL', TRUE,  NULL,                    NULL, NULL, NULL, NULL, NULL, 'Offre transmise au département');

-- Flux d'actualités techniques (API publiques gratuites). Les flux RSS de l'université et de la
-- faculté sont ajoutés par le responsable depuis l'interface (URL à vérifier sur leurs sites).
INSERT INTO news_feeds (name, feed_url, feed_type, category, enabled, sync_interval_minutes) VALUES
  ('Hacker News - Top stories', 'https://hacker-news.firebaseio.com/v0/topstories.json', 'API', 'TECHNOLOGIE', TRUE, 60),
  ('DEV Community - Articles',  'https://dev.to/api/articles',                           'API', 'TECHNOLOGIE', TRUE, 60);
