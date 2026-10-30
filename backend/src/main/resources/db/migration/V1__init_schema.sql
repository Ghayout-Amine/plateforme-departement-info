-- =============================================================================
--  Plateforme de Gestion du Département Informatique (PGDI2026)
--  Story PGDI2026-11 : Conception de la base de données (MCD & MLD)
--  v02 (28/10/2026) : refonte des modules 6 (offres externes via API) et 7 (veille externe)
--    - supprimées : companies, internship_offers, applications, application_documents
--    - ajoutées   : job_sources, sync_logs, external_offers, favorite_offers, saved_searches,
--                   application_trackings, news_feeds, external_articles, news_subscriptions
--    - modifiées  : internships (déclaré par l'étudiant + convention), news (is_pinned, source_article_id)
--  Script de création du schéma — MySQL 8.0+ (InnoDB, utf8mb4)
--  Compatible Flyway : à placer dans src/main/resources/db/migration/
--
--  Conventions :
--    * Tables en snake_case pluriel ; clé primaire technique `id` BIGINT AUTO_INCREMENT
--      (mapping JPA : @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id)
--    * Énumérations stockées en VARCHAR + contrainte CHECK (JPA : @Enumerated(EnumType.STRING))
--    * Suppression : RESTRICT par défaut, CASCADE pour les dépendances pures,
--      SET NULL pour les références de traçabilité
--    * created_at / updated_at gérés par la base (+ @CreationTimestamp côté JPA si souhaité)
-- =============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;


-- ---------------------------------------------------------------------
-- BLOC : Sécurité et administration
-- ---------------------------------------------------------------------

CREATE TABLE users (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Identifiant technique (AUTO_INCREMENT)',
  email VARCHAR(150) NOT NULL COMMENT 'Identifiant de connexion',
  password_hash VARCHAR(255) NOT NULL COMMENT 'Mot de passe haché (BCrypt)',
  first_name VARCHAR(80) NOT NULL COMMENT 'Prénom',
  last_name VARCHAR(80) NOT NULL COMMENT 'Nom',
  phone VARCHAR(30) NULL COMMENT 'Téléphone',
  avatar_url VARCHAR(255) NULL COMMENT 'Photo de profil',
  enabled BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Compte actif (FALSE = désactivé)',
  last_login_at DATETIME NULL COMMENT 'Dernière connexion',
  deleted_at DATETIME NULL COMMENT 'Suppression logique (NULL = compte existant)',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Date de création',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Dernière modification',
  PRIMARY KEY (id),
  CONSTRAINT uq_users_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Compte de connexion de tout acteur authentifié (étudiant, enseignant, responsable, admin).';

CREATE TABLE roles (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Identifiant technique (AUTO_INCREMENT)',
  name VARCHAR(50) NOT NULL COMMENT 'Nom du rôle, ex. ROLE_ADMIN',
  description VARCHAR(255) NULL COMMENT 'Description',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Date de création',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Dernière modification',
  PRIMARY KEY (id),
  CONSTRAINT uq_roles_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Rôle applicatif (ROLE_STUDENT, ROLE_TEACHER, ROLE_DEPARTMENT_HEAD, ROLE_ADMIN).';

CREATE TABLE permissions (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Identifiant technique (AUTO_INCREMENT)',
  code VARCHAR(80) NOT NULL COMMENT 'Code de permission, ex. STUDENT_READ',
  description VARCHAR(255) NULL COMMENT 'Description',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Date de création',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Dernière modification',
  PRIMARY KEY (id),
  CONSTRAINT uq_permissions_code UNIQUE (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Droit fin attribuable à un rôle (ex. STUDENT_READ, GRADE_WRITE).';

CREATE TABLE user_roles (
  user_id BIGINT NOT NULL COMMENT 'Utilisateur',
  role_id BIGINT NOT NULL COMMENT 'Rôle',
  PRIMARY KEY (user_id, role_id),
  CONSTRAINT fk_user_roles_user_id FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
  CONSTRAINT fk_user_roles_role_id FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Association N-N UTILISATEUR–ROLE.';

CREATE TABLE role_permissions (
  role_id BIGINT NOT NULL COMMENT 'Rôle',
  permission_id BIGINT NOT NULL COMMENT 'Permission',
  PRIMARY KEY (role_id, permission_id),
  CONSTRAINT fk_role_permissions_role_id FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE,
  CONSTRAINT fk_role_permissions_permission_id FOREIGN KEY (permission_id) REFERENCES permissions (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Association N-N ROLE–PERMISSION.';

CREATE TABLE user_tokens (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Identifiant technique (AUTO_INCREMENT)',
  user_id BIGINT NOT NULL COMMENT 'Propriétaire du jeton',
  token_type VARCHAR(20) NOT NULL COMMENT 'Type de jeton',
  token_hash VARCHAR(255) NOT NULL COMMENT 'Empreinte du jeton (jamais en clair)',
  expires_at DATETIME NOT NULL COMMENT 'Date d''expiration',
  revoked_at DATETIME NULL COMMENT 'Révocation ou consommation (NULL = valide)',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Date de création',
  PRIMARY KEY (id),
  CONSTRAINT uq_user_tokens_token_hash UNIQUE (token_hash),
  CONSTRAINT fk_user_tokens_user_id FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
  CONSTRAINT ck_user_tokens_token_type CHECK (token_type IN ('REFRESH', 'PASSWORD_RESET')),
  INDEX idx_tokens_user (user_id, token_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Jetons de rafraîchissement JWT et de réinitialisation de mot de passe.';

CREATE TABLE audit_logs (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Identifiant technique (AUTO_INCREMENT)',
  user_id BIGINT NULL COMMENT 'Auteur de l''action (NULL si anonyme/système)',
  action VARCHAR(80) NOT NULL COMMENT 'Action, ex. LOGIN, CREATE_USER, GRADE_UPDATE',
  entity_type VARCHAR(80) NULL COMMENT 'Type d''objet concerné',
  entity_id BIGINT NULL COMMENT 'Identifiant de l''objet concerné',
  details JSON NULL COMMENT 'Détails complémentaires (ancienne/nouvelle valeur)',
  ip_address VARCHAR(45) NULL COMMENT 'Adresse IP (IPv4/IPv6)',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Date de création',
  PRIMARY KEY (id),
  CONSTRAINT fk_audit_logs_user_id FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL,
  INDEX idx_audit_created (created_at),
  INDEX idx_audit_action (action)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Journal des actions importantes (traçabilité, sécurité).';

CREATE TABLE system_parameters (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Identifiant technique (AUTO_INCREMENT)',
  param_key VARCHAR(100) NOT NULL COMMENT 'Clé du paramètre',
  param_value VARCHAR(1000) NULL COMMENT 'Valeur',
  description VARCHAR(255) NULL COMMENT 'Description',
  updated_by BIGINT NULL COMMENT 'Dernier utilisateur ayant modifié',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Date de création',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Dernière modification',
  PRIMARY KEY (id),
  CONSTRAINT uq_system_parameters_param_key UNIQUE (param_key),
  CONSTRAINT fk_system_parameters_updated_by FOREIGN KEY (updated_by) REFERENCES users (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Paramètres modifiables du système (coordonnées du département, options).';

-- ---------------------------------------------------------------------
-- BLOC : Partie publique, actualités et veille externe (module 7 — v02)
-- ---------------------------------------------------------------------

CREATE TABLE news_feeds (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Identifiant technique (AUTO_INCREMENT)',
  name VARCHAR(150) NOT NULL COMMENT 'Nom affiché de la source',
  feed_url VARCHAR(500) NOT NULL COMMENT 'URL du flux RSS/Atom ou du point d''accès API',
  feed_type VARCHAR(10) NOT NULL DEFAULT 'RSS' COMMENT 'Format du flux',
  category VARCHAR(20) NOT NULL COMMENT 'Catégorie appliquée aux articles du flux',
  enabled BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Flux synchronisé ou non',
  sync_interval_minutes SMALLINT NOT NULL DEFAULT 60 COMMENT 'Fréquence de synchronisation',
  last_sync_at DATETIME NULL COMMENT 'Dernière synchronisation',
  last_status VARCHAR(10) NULL COMMENT 'Résultat de la dernière synchronisation',
  last_error VARCHAR(500) NULL COMMENT 'Dernier message d''erreur',
  created_by BIGINT NULL COMMENT 'Responsable ayant ajouté le flux',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Date de création',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Dernière modification',
  PRIMARY KEY (id),
  CONSTRAINT uq_news_feeds_feed_url UNIQUE (feed_url),
  CONSTRAINT fk_news_feeds_created_by FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE SET NULL,
  CONSTRAINT ck_news_feeds_feed_type CHECK (feed_type IN ('RSS', 'ATOM', 'API')),
  CONSTRAINT ck_news_feeds_category CHECK (category IN ('DEPARTEMENT', 'UNIVERSITE', 'TECHNOLOGIE', 'EMPLOI', 'EVENEMENTS')),
  CONSTRAINT ck_news_feeds_last_status CHECK (last_status IS NULL OR last_status IN ('SUCCESS', 'PARTIAL', 'FAILED')),
  CONSTRAINT ck_news_feeds_interval CHECK (sync_interval_minutes >= 15)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Flux externe d''actualités (RSS/Atom ou API) agrégé par la plateforme.';

CREATE TABLE external_articles (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Identifiant technique (AUTO_INCREMENT)',
  title VARCHAR(300) NOT NULL COMMENT 'Titre',
  summary VARCHAR(1000) NULL COMMENT 'Résumé (HTML nettoyé)',
  image_url VARCHAR(500) NULL COMMENT 'Image',
  original_url VARCHAR(700) NOT NULL COMMENT 'Lien vers l''article d''origine',
  url_hash CHAR(64) NOT NULL COMMENT 'SHA-256 de l''URL (dédoublonnage)',
  author VARCHAR(150) NULL COMMENT 'Auteur',
  category VARCHAR(20) NOT NULL COMMENT 'Catégorie (héritée du flux)',
  status VARCHAR(10) NOT NULL DEFAULT 'VISIBLE' COMMENT 'Visible ou masqué par le responsable',
  published_at DATETIME NULL COMMENT 'Date de publication à la source',
  fetched_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Date de récupération',
  feed_id BIGINT NOT NULL COMMENT 'Flux d''origine',
  PRIMARY KEY (id),
  CONSTRAINT uq_external_articles_url_hash UNIQUE (url_hash),
  CONSTRAINT fk_external_articles_feed_id FOREIGN KEY (feed_id) REFERENCES news_feeds (id) ON DELETE CASCADE,
  CONSTRAINT ck_external_articles_category CHECK (category IN ('DEPARTEMENT', 'UNIVERSITE', 'TECHNOLOGIE', 'EMPLOI', 'EVENEMENTS')),
  CONSTRAINT ck_external_articles_status CHECK (status IN ('VISIBLE', 'HIDDEN')),
  INDEX idx_articles_feed_pub (category, status, published_at),
  FULLTEXT INDEX ft_articles_search (title, summary)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Article récupéré depuis un flux externe (purgé après 30 jours, sauf s''il est promu en annonce).';

CREATE TABLE news (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Identifiant technique (AUTO_INCREMENT)',
  title VARCHAR(200) NOT NULL COMMENT 'Titre',
  slug VARCHAR(220) NOT NULL COMMENT 'Identifiant lisible pour l''URL',
  summary VARCHAR(500) NULL COMMENT 'Résumé',
  content LONGTEXT NOT NULL COMMENT 'Contenu',
  category VARCHAR(20) NOT NULL DEFAULT 'NEWS' COMMENT 'Actualité ou annonce',
  visibility VARCHAR(20) NOT NULL DEFAULT 'PUBLIC' COMMENT 'Public cible',
  status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT 'Statut de publication',
  is_pinned BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Annonce épinglée en tête du fil',
  cover_image_url VARCHAR(255) NULL COMMENT 'Image de couverture',
  published_at DATETIME NULL COMMENT 'Date de publication',
  author_id BIGINT NOT NULL COMMENT 'Auteur',
  source_article_id BIGINT NULL COMMENT 'Article externe promu en annonce (NULL si rédaction interne)',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Date de création',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Dernière modification',
  PRIMARY KEY (id),
  CONSTRAINT uq_news_slug UNIQUE (slug),
  CONSTRAINT uq_news_source_article_id UNIQUE (source_article_id),
  CONSTRAINT fk_news_author_id FOREIGN KEY (author_id) REFERENCES users (id) ON DELETE RESTRICT,
  CONSTRAINT fk_news_source_article_id FOREIGN KEY (source_article_id) REFERENCES external_articles (id) ON DELETE SET NULL,
  CONSTRAINT ck_news_category CHECK (category IN ('NEWS', 'ANNOUNCEMENT')),
  CONSTRAINT ck_news_visibility CHECK (visibility IN ('PUBLIC', 'AUTHENTICATED', 'STUDENTS', 'TEACHERS')),
  CONSTRAINT ck_news_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
  INDEX idx_news_status_pub (status, published_at),
  FULLTEXT INDEX ft_news_search (title, summary)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Actualités et annonces publiées par le département.';

CREATE TABLE news_subscriptions (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Identifiant technique (AUTO_INCREMENT)',
  category VARCHAR(20) NOT NULL COMMENT 'Catégorie suivie',
  channel VARCHAR(10) NOT NULL DEFAULT 'EMAIL' COMMENT 'Canal de notification',
  user_id BIGINT NOT NULL COMMENT 'Abonné',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Date de création',
  PRIMARY KEY (id),
  CONSTRAINT uq_subscription_user_category UNIQUE (user_id, category),
  CONSTRAINT fk_news_subscriptions_user_id FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
  CONSTRAINT ck_news_subscriptions_category CHECK (category IN ('DEPARTEMENT', 'UNIVERSITE', 'TECHNOLOGIE', 'EMPLOI', 'EVENEMENTS')),
  CONSTRAINT ck_news_subscriptions_channel CHECK (channel IN ('EMAIL', 'IN_APP'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Abonnement d''un utilisateur à une catégorie d''actualités.';

CREATE TABLE contact_messages (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Identifiant technique (AUTO_INCREMENT)',
  full_name VARCHAR(150) NOT NULL COMMENT 'Nom de l''expéditeur',
  email VARCHAR(150) NOT NULL COMMENT 'Email de l''expéditeur',
  subject VARCHAR(200) NOT NULL COMMENT 'Objet',
  message TEXT NOT NULL COMMENT 'Message',
  status VARCHAR(20) NOT NULL DEFAULT 'NEW' COMMENT 'Statut de traitement',
  handled_by BIGINT NULL COMMENT 'Utilisateur ayant traité le message',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Date de création',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Dernière modification',
  PRIMARY KEY (id),
  CONSTRAINT fk_contact_messages_handled_by FOREIGN KEY (handled_by) REFERENCES users (id) ON DELETE SET NULL,
  CONSTRAINT ck_contact_messages_status CHECK (status IN ('NEW', 'READ', 'REPLIED', 'ARCHIVED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Messages envoyés via le formulaire de contact public.';

-- ---------------------------------------------------------------------
-- BLOC : Structure pédagogique
-- ---------------------------------------------------------------------

CREATE TABLE academic_years (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Identifiant technique (AUTO_INCREMENT)',
  label VARCHAR(9) NOT NULL COMMENT 'Libellé, ex. 2026-2027',
  start_date DATE NOT NULL COMMENT 'Début',
  end_date DATE NOT NULL COMMENT 'Fin',
  is_current BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Année en cours (une seule à TRUE, règle applicative)',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Date de création',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Dernière modification',
  PRIMARY KEY (id),
  CONSTRAINT uq_academic_years_label UNIQUE (label),
  CONSTRAINT ck_acyear_dates CHECK (end_date > start_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Année universitaire (ex. 2026-2027).';

CREATE TABLE formations (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Identifiant technique (AUTO_INCREMENT)',
  code VARCHAR(30) NOT NULL COMMENT 'Code, ex. M2I',
  name VARCHAR(150) NOT NULL COMMENT 'Intitulé',
  degree_level VARCHAR(20) NOT NULL COMMENT 'Niveau de diplôme',
  duration_semesters TINYINT NOT NULL COMMENT 'Durée en semestres',
  description TEXT NULL COMMENT 'Présentation (page publique)',
  is_active BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Formation ouverte/visible',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Date de création',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Dernière modification',
  PRIMARY KEY (id),
  CONSTRAINT uq_formations_code UNIQUE (code),
  CONSTRAINT ck_formations_degree_level CHECK (degree_level IN ('LICENCE', 'MASTER', 'CYCLE_INGENIEUR', 'DOCTORAT')),
  CONSTRAINT ck_formations_duration_semesters CHECK (duration_semesters > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Formation proposée par le département (ex. Master Ingénierie Informatique).';

CREATE TABLE filieres (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Identifiant technique (AUTO_INCREMENT)',
  code VARCHAR(30) NOT NULL COMMENT 'Code de la filière',
  name VARCHAR(150) NOT NULL COMMENT 'Intitulé',
  description TEXT NULL COMMENT 'Description',
  is_active BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Filière active',
  formation_id BIGINT NOT NULL COMMENT 'Formation d''appartenance',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Date de création',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Dernière modification',
  PRIMARY KEY (id),
  CONSTRAINT uq_filieres_code UNIQUE (code),
  CONSTRAINT uq_filiere_formation_name UNIQUE (formation_id, name),
  CONSTRAINT fk_filieres_formation_id FOREIGN KEY (formation_id) REFERENCES formations (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Filière (parcours) d''une formation.';

CREATE TABLE course_modules (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Identifiant technique (AUTO_INCREMENT)',
  code VARCHAR(30) NOT NULL COMMENT 'Code du module',
  name VARCHAR(150) NOT NULL COMMENT 'Intitulé',
  description TEXT NULL COMMENT 'Description / programme',
  hours_lecture SMALLINT NOT NULL DEFAULT 0 COMMENT 'Volume horaire cours (CM)',
  hours_tutorial SMALLINT NOT NULL DEFAULT 0 COMMENT 'Volume horaire TD',
  hours_practical SMALLINT NOT NULL DEFAULT 0 COMMENT 'Volume horaire TP',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Date de création',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Dernière modification',
  PRIMARY KEY (id),
  CONSTRAINT uq_course_modules_code UNIQUE (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Module d''enseignement (matière), éventuellement commun à plusieurs filières.';

CREATE TABLE filiere_modules (
  filiere_id BIGINT NOT NULL COMMENT 'Filière',
  course_module_id BIGINT NOT NULL COMMENT 'Module',
  semester TINYINT NOT NULL COMMENT 'Semestre du module dans cette filière (1 à 10)',
  credits TINYINT NOT NULL DEFAULT 0 COMMENT 'Crédits ECTS dans cette filière',
  coefficient DECIMAL(4,2) NOT NULL DEFAULT 1.00 COMMENT 'Coefficient dans cette filière',
  PRIMARY KEY (filiere_id, course_module_id),
  CONSTRAINT fk_filiere_modules_filiere_id FOREIGN KEY (filiere_id) REFERENCES filieres (id) ON DELETE CASCADE,
  CONSTRAINT fk_filiere_modules_course_module_id FOREIGN KEY (course_module_id) REFERENCES course_modules (id) ON DELETE RESTRICT,
  CONSTRAINT ck_filiere_modules_semester CHECK (semester BETWEEN 1 AND 10),
  CONSTRAINT ck_filiere_modules_credits CHECK (credits >= 0),
  CONSTRAINT ck_filiere_modules_coefficient CHECK (coefficient > 0),
  INDEX idx_filmod_module (course_module_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Association N-N FILIERE–MODULE : programme d''une filière, avec le semestre propre à cette filière.';

CREATE TABLE groupes (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Identifiant technique (AUTO_INCREMENT)',
  name VARCHAR(50) NOT NULL COMMENT 'Nom du groupe, ex. G1',
  capacity SMALLINT NULL COMMENT 'Effectif maximal',
  filiere_id BIGINT NOT NULL COMMENT 'Filière',
  academic_year_id BIGINT NOT NULL COMMENT 'Année universitaire',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Date de création',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Dernière modification',
  PRIMARY KEY (id),
  CONSTRAINT uq_groupe_filiere_year_name UNIQUE (filiere_id, academic_year_id, name),
  CONSTRAINT fk_groupes_filiere_id FOREIGN KEY (filiere_id) REFERENCES filieres (id) ON DELETE RESTRICT,
  CONSTRAINT fk_groupes_academic_year_id FOREIGN KEY (academic_year_id) REFERENCES academic_years (id) ON DELETE RESTRICT,
  CONSTRAINT ck_groupes_capacity CHECK (capacity IS NULL OR capacity > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Groupe d''étudiants d''une filière pour une année universitaire.';

CREATE TABLE salles (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Identifiant technique (AUTO_INCREMENT)',
  code VARCHAR(20) NOT NULL COMMENT 'Code, ex. AMPHI-A',
  name VARCHAR(100) NOT NULL COMMENT 'Désignation',
  building VARCHAR(100) NULL COMMENT 'Bâtiment',
  capacity SMALLINT NOT NULL COMMENT 'Capacité d''accueil',
  room_type VARCHAR(20) NOT NULL COMMENT 'Type de salle',
  is_active BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Salle utilisable',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Date de création',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Dernière modification',
  PRIMARY KEY (id),
  CONSTRAINT uq_salles_code UNIQUE (code),
  CONSTRAINT ck_salles_capacity CHECK (capacity > 0),
  CONSTRAINT ck_salles_room_type CHECK (room_type IN ('AMPHI', 'CLASSROOM', 'LAB', 'OTHER'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Salle (amphi, salle de cours, laboratoire).';

-- ---------------------------------------------------------------------
-- BLOC : Étudiants, enseignants, inscriptions
-- ---------------------------------------------------------------------

CREATE TABLE students (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Identifiant technique (AUTO_INCREMENT)',
  student_number VARCHAR(30) NOT NULL COMMENT 'Numéro d''étudiant (code Apogée)',
  cne VARCHAR(20) NULL COMMENT 'CNE / code Massar',
  birth_date DATE NULL COMMENT 'Date de naissance',
  birth_place VARCHAR(100) NULL COMMENT 'Lieu de naissance',
  gender VARCHAR(10) NULL COMMENT 'Genre',
  address VARCHAR(255) NULL COMMENT 'Adresse',
  city VARCHAR(80) NULL COMMENT 'Ville',
  user_id BIGINT NOT NULL COMMENT 'Compte utilisateur associé',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Date de création',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Dernière modification',
  PRIMARY KEY (id),
  CONSTRAINT uq_students_student_number UNIQUE (student_number),
  CONSTRAINT uq_students_cne UNIQUE (cne),
  CONSTRAINT uq_students_user_id UNIQUE (user_id),
  CONSTRAINT fk_students_user_id FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE RESTRICT,
  CONSTRAINT ck_students_gender CHECK (gender IN ('MALE', 'FEMALE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Profil étudiant (1-1 avec un compte utilisateur).';

CREATE TABLE teachers (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Identifiant technique (AUTO_INCREMENT)',
  employee_number VARCHAR(30) NOT NULL COMMENT 'Matricule / PPR',
  academic_title VARCHAR(30) NULL COMMENT 'Grade (PES, PH, PA, vacataire…)',
  specialty VARCHAR(150) NULL COMMENT 'Spécialité',
  office VARCHAR(50) NULL COMMENT 'Bureau',
  hire_date DATE NULL COMMENT 'Date de recrutement',
  bio TEXT NULL COMMENT 'Présentation',
  user_id BIGINT NOT NULL COMMENT 'Compte utilisateur associé',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Date de création',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Dernière modification',
  PRIMARY KEY (id),
  CONSTRAINT uq_teachers_employee_number UNIQUE (employee_number),
  CONSTRAINT uq_teachers_user_id UNIQUE (user_id),
  CONSTRAINT fk_teachers_user_id FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Profil enseignant (1-1 avec un compte utilisateur).';

CREATE TABLE enrollments (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Identifiant technique (AUTO_INCREMENT)',
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'Situation de l''étudiant',
  student_id BIGINT NOT NULL COMMENT 'Étudiant',
  filiere_id BIGINT NOT NULL COMMENT 'Filière (donne la formation)',
  academic_year_id BIGINT NOT NULL COMMENT 'Année universitaire',
  groupe_id BIGINT NULL COMMENT 'Groupe (NULL tant que non affecté)',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Date de création',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Dernière modification',
  PRIMARY KEY (id),
  CONSTRAINT uq_enrollment_student_year UNIQUE (student_id, academic_year_id),
  CONSTRAINT fk_enrollments_student_id FOREIGN KEY (student_id) REFERENCES students (id) ON DELETE RESTRICT,
  CONSTRAINT fk_enrollments_filiere_id FOREIGN KEY (filiere_id) REFERENCES filieres (id) ON DELETE RESTRICT,
  CONSTRAINT fk_enrollments_academic_year_id FOREIGN KEY (academic_year_id) REFERENCES academic_years (id) ON DELETE RESTRICT,
  CONSTRAINT fk_enrollments_groupe_id FOREIGN KEY (groupe_id) REFERENCES groupes (id) ON DELETE SET NULL,
  CONSTRAINT ck_enrollments_status CHECK (status IN ('ACTIVE', 'VALIDATED', 'REPEATING', 'SUSPENDED', 'WITHDRAWN'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Inscription annuelle d''un étudiant à une filière, avec son groupe.';

-- ---------------------------------------------------------------------
-- BLOC : Enseignement, emploi du temps, examens
-- ---------------------------------------------------------------------

CREATE TABLE teaching_assignments (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Identifiant technique (AUTO_INCREMENT)',
  session_type VARCHAR(10) NOT NULL COMMENT 'Cours magistral, TD ou TP',
  teacher_id BIGINT NOT NULL COMMENT 'Enseignant',
  course_module_id BIGINT NOT NULL COMMENT 'Module',
  groupe_id BIGINT NOT NULL COMMENT 'Groupe',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Date de création',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Dernière modification',
  PRIMARY KEY (id),
  CONSTRAINT uq_assignment UNIQUE (teacher_id, course_module_id, groupe_id, session_type),
  CONSTRAINT fk_teaching_assignments_teacher_id FOREIGN KEY (teacher_id) REFERENCES teachers (id) ON DELETE RESTRICT,
  CONSTRAINT fk_teaching_assignments_course_module_id FOREIGN KEY (course_module_id) REFERENCES course_modules (id) ON DELETE RESTRICT,
  CONSTRAINT fk_teaching_assignments_groupe_id FOREIGN KEY (groupe_id) REFERENCES groupes (id) ON DELETE RESTRICT,
  CONSTRAINT ck_teaching_assignments_session_type CHECK (session_type IN ('CM', 'TD', 'TP'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Affectation d''un enseignant à un module pour un groupe et un type de séance.';

CREATE TABLE plannings (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Identifiant technique (AUTO_INCREMENT)',
  session_date DATE NOT NULL COMMENT 'Date de la séance',
  start_time TIME NOT NULL COMMENT 'Heure de début',
  end_time TIME NOT NULL COMMENT 'Heure de fin',
  status VARCHAR(20) NOT NULL DEFAULT 'PLANNED' COMMENT 'Statut de la séance',
  notes VARCHAR(255) NULL COMMENT 'Remarques',
  teaching_assignment_id BIGINT NOT NULL COMMENT 'Affectation (enseignant + module + groupe)',
  salle_id BIGINT NOT NULL COMMENT 'Salle',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Date de création',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Dernière modification',
  PRIMARY KEY (id),
  CONSTRAINT uq_planning_room_slot UNIQUE (salle_id, session_date, start_time),
  CONSTRAINT fk_plannings_teaching_assignment_id FOREIGN KEY (teaching_assignment_id) REFERENCES teaching_assignments (id) ON DELETE RESTRICT,
  CONSTRAINT fk_plannings_salle_id FOREIGN KEY (salle_id) REFERENCES salles (id) ON DELETE RESTRICT,
  CONSTRAINT ck_plannings_status CHECK (status IN ('PLANNED', 'CANCELLED', 'DONE')),
  CONSTRAINT ck_planning_times CHECK (end_time > start_time),
  INDEX idx_planning_date (session_date),
  INDEX idx_planning_assignment_date (teaching_assignment_id, session_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Séance datée de l''emploi du temps.';

CREATE TABLE examens (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Identifiant technique (AUTO_INCREMENT)',
  title VARCHAR(150) NOT NULL COMMENT 'Intitulé',
  exam_type VARCHAR(20) NOT NULL COMMENT 'Type d''évaluation',
  exam_date DATE NOT NULL COMMENT 'Date',
  start_time TIME NULL COMMENT 'Heure de début',
  end_time TIME NULL COMMENT 'Heure de fin',
  weight DECIMAL(5,2) NOT NULL DEFAULT 100.00 COMMENT 'Poids dans la moyenne du module (%)',
  course_module_id BIGINT NOT NULL COMMENT 'Module évalué',
  academic_year_id BIGINT NOT NULL COMMENT 'Année universitaire',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Date de création',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Dernière modification',
  PRIMARY KEY (id),
  CONSTRAINT fk_examens_course_module_id FOREIGN KEY (course_module_id) REFERENCES course_modules (id) ON DELETE RESTRICT,
  CONSTRAINT fk_examens_academic_year_id FOREIGN KEY (academic_year_id) REFERENCES academic_years (id) ON DELETE RESTRICT,
  CONSTRAINT ck_examens_exam_type CHECK (exam_type IN ('CC', 'TP', 'FINAL', 'RATTRAPAGE')),
  CONSTRAINT ck_examens_weight CHECK (weight BETWEEN 0 AND 100),
  CONSTRAINT ck_examen_times CHECK (end_time IS NULL OR start_time IS NULL OR end_time > start_time),
  INDEX idx_examen_date (exam_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Évaluation d''un module (contrôle continu, TP, examen final, rattrapage).';

CREATE TABLE exam_salles (
  examen_id BIGINT NOT NULL COMMENT 'Examen',
  salle_id BIGINT NOT NULL COMMENT 'Salle',
  PRIMARY KEY (examen_id, salle_id),
  CONSTRAINT fk_exam_salles_examen_id FOREIGN KEY (examen_id) REFERENCES examens (id) ON DELETE CASCADE,
  CONSTRAINT fk_exam_salles_salle_id FOREIGN KEY (salle_id) REFERENCES salles (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Association N-N EXAMEN–SALLE (un examen peut occuper plusieurs salles).';

-- ---------------------------------------------------------------------
-- BLOC : Notes, absences, ressources
-- ---------------------------------------------------------------------

CREATE TABLE grades (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Identifiant technique (AUTO_INCREMENT)',
  score DECIMAL(5,2) NULL COMMENT 'Note sur 20 (NULL si absent)',
  status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT 'Brouillon, publiée (visible) ou absent',
  review_comment VARCHAR(255) NULL COMMENT 'Appréciation',
  student_id BIGINT NOT NULL COMMENT 'Étudiant',
  examen_id BIGINT NOT NULL COMMENT 'Évaluation',
  entered_by BIGINT NULL COMMENT 'Enseignant ayant saisi la note',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Date de création',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Dernière modification',
  PRIMARY KEY (id),
  CONSTRAINT uq_grade_student_exam UNIQUE (student_id, examen_id),
  CONSTRAINT fk_grades_student_id FOREIGN KEY (student_id) REFERENCES students (id) ON DELETE RESTRICT,
  CONSTRAINT fk_grades_examen_id FOREIGN KEY (examen_id) REFERENCES examens (id) ON DELETE RESTRICT,
  CONSTRAINT fk_grades_entered_by FOREIGN KEY (entered_by) REFERENCES teachers (id) ON DELETE SET NULL,
  CONSTRAINT ck_grades_score CHECK (score IS NULL OR score BETWEEN 0 AND 20),
  CONSTRAINT ck_grades_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'ABSENT'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Note d''un étudiant à une évaluation.';

CREATE TABLE absences (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Identifiant technique (AUTO_INCREMENT)',
  absence_type VARCHAR(10) NOT NULL DEFAULT 'ABSENT' COMMENT 'Absence ou retard',
  is_justified BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Absence justifiée',
  justification VARCHAR(500) NULL COMMENT 'Motif de justification',
  justification_file VARCHAR(500) NULL COMMENT 'Chemin du justificatif',
  student_id BIGINT NOT NULL COMMENT 'Étudiant',
  planning_id BIGINT NOT NULL COMMENT 'Séance concernée',
  recorded_by BIGINT NULL COMMENT 'Enseignant ayant saisi',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Date de création',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Dernière modification',
  PRIMARY KEY (id),
  CONSTRAINT uq_absence_student_planning UNIQUE (student_id, planning_id),
  CONSTRAINT fk_absences_student_id FOREIGN KEY (student_id) REFERENCES students (id) ON DELETE RESTRICT,
  CONSTRAINT fk_absences_planning_id FOREIGN KEY (planning_id) REFERENCES plannings (id) ON DELETE RESTRICT,
  CONSTRAINT fk_absences_recorded_by FOREIGN KEY (recorded_by) REFERENCES teachers (id) ON DELETE SET NULL,
  CONSTRAINT ck_absences_absence_type CHECK (absence_type IN ('ABSENT', 'LATE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Absence ou retard d''un étudiant à une séance.';

CREATE TABLE resources (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Identifiant technique (AUTO_INCREMENT)',
  title VARCHAR(200) NOT NULL COMMENT 'Titre',
  description TEXT NULL COMMENT 'Description',
  resource_type VARCHAR(20) NOT NULL COMMENT 'Nature de la ressource',
  file_name VARCHAR(255) NULL COMMENT 'Nom du fichier d''origine',
  file_path VARCHAR(500) NULL COMMENT 'Chemin de stockage du fichier',
  mime_type VARCHAR(100) NULL COMMENT 'Type MIME',
  file_size BIGINT NULL COMMENT 'Taille en octets',
  external_url VARCHAR(500) NULL COMMENT 'Lien externe (si pas de fichier)',
  is_published BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Visible par les étudiants',
  course_module_id BIGINT NOT NULL COMMENT 'Module concerné',
  teacher_id BIGINT NOT NULL COMMENT 'Enseignant auteur',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Date de création',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Dernière modification',
  PRIMARY KEY (id),
  CONSTRAINT fk_resources_course_module_id FOREIGN KEY (course_module_id) REFERENCES course_modules (id) ON DELETE RESTRICT,
  CONSTRAINT fk_resources_teacher_id FOREIGN KEY (teacher_id) REFERENCES teachers (id) ON DELETE RESTRICT,
  CONSTRAINT ck_resources_resource_type CHECK (resource_type IN ('COURSE', 'TD', 'TP', 'EXAM_ARCHIVE', 'VIDEO', 'LINK', 'OTHER')),
  CONSTRAINT ck_resource_source CHECK (file_path IS NOT NULL OR external_url IS NOT NULL)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Ressource pédagogique (cours, TD, TP, archive d''examen, lien).';

-- ---------------------------------------------------------------------
-- BLOC : Stages, PFE et offres d'emploi externes (module 6 — v02)
--   Les entreprises n'ont plus de compte : les offres viennent d'API externes
--   (Jooble, Careerjet, JSearch, France Travail, Adzuna) ou d'une saisie
--   manuelle du département. L'étudiant postule sur le site d'origine,
--   suit ses candidatures, puis déclare le stage obtenu.
-- ---------------------------------------------------------------------

CREATE TABLE job_sources (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Identifiant technique (AUTO_INCREMENT)',
  code VARCHAR(30) NOT NULL COMMENT 'Code du connecteur, ex. JOOBLE, CAREERJET, JSEARCH, FRANCE_TRAVAIL, ADZUNA, DEPARTEMENT',
  name VARCHAR(100) NOT NULL COMMENT 'Nom affiché',
  website_url VARCHAR(255) NULL COMMENT 'Site du fournisseur',
  access_mode VARCHAR(10) NOT NULL COMMENT 'CACHED = synchronisé en base, LIVE = interrogé en direct, MANUAL = saisie',
  enabled BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Source active',
  api_key_ref VARCHAR(80) NULL COMMENT 'NOM de la variable d''environnement contenant la clé (jamais la clé elle-même)',
  sync_interval_minutes SMALLINT NULL COMMENT 'Fréquence de synchronisation (mode CACHED)',
  default_keywords VARCHAR(500) NULL COMMENT 'Mots-clés surveillés, séparés par des virgules',
  default_locations VARCHAR(500) NULL COMMENT 'Villes/pays surveillés, séparés par des virgules',
  quota_limit INT NULL COMMENT 'Nombre maximal d''appels par période',
  quota_period VARCHAR(10) NULL COMMENT 'Période du quota',
  attribution_text VARCHAR(255) NULL COMMENT 'Mention obligatoire à afficher (ex. Jobs by Adzuna)',
  logo_url VARCHAR(255) NULL COMMENT 'Logo exigé par le fournisseur',
  last_sync_at DATETIME NULL COMMENT 'Dernière synchronisation réussie',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Date de création',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Dernière modification',
  PRIMARY KEY (id),
  CONSTRAINT uq_job_sources_code UNIQUE (code),
  CONSTRAINT ck_job_sources_access_mode CHECK (access_mode IN ('CACHED', 'LIVE', 'MANUAL')),
  CONSTRAINT ck_job_sources_quota_period CHECK (quota_period IS NULL OR quota_period IN ('MINUTE', 'DAY', 'MONTH')),
  CONSTRAINT ck_job_sources_interval CHECK (sync_interval_minutes IS NULL OR sync_interval_minutes >= 15)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Fournisseur d''offres (API externe ou saisie du département) et sa configuration.';

CREATE TABLE sync_logs (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Identifiant technique (AUTO_INCREMENT)',
  started_at DATETIME NOT NULL COMMENT 'Début de la synchronisation',
  finished_at DATETIME NULL COMMENT 'Fin (NULL si en cours)',
  status VARCHAR(10) NOT NULL COMMENT 'Résultat',
  fetched_count INT NOT NULL DEFAULT 0 COMMENT 'Offres reçues',
  created_count INT NOT NULL DEFAULT 0 COMMENT 'Offres nouvelles',
  updated_count INT NOT NULL DEFAULT 0 COMMENT 'Offres mises à jour',
  expired_count INT NOT NULL DEFAULT 0 COMMENT 'Offres passées à EXPIRED',
  quota_remaining INT NULL COMMENT 'Quota restant annoncé par le fournisseur',
  error_message VARCHAR(1000) NULL COMMENT 'Erreur éventuelle',
  source_id BIGINT NOT NULL COMMENT 'Source synchronisée',
  PRIMARY KEY (id),
  CONSTRAINT fk_sync_logs_source_id FOREIGN KEY (source_id) REFERENCES job_sources (id) ON DELETE CASCADE,
  CONSTRAINT ck_sync_logs_status CHECK (status IN ('RUNNING', 'SUCCESS', 'PARTIAL', 'FAILED')),
  CONSTRAINT ck_sync_logs_dates CHECK (finished_at IS NULL OR finished_at >= started_at),
  INDEX idx_sync_source_date (source_id, started_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Journal d''exécution des synchronisations (tableau de bord admin).';

CREATE TABLE external_offers (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Identifiant technique (AUTO_INCREMENT)',
  external_id VARCHAR(150) NOT NULL COMMENT 'Identifiant de l''offre chez le fournisseur',
  title VARCHAR(255) NOT NULL COMMENT 'Intitulé du poste',
  company_name VARCHAR(200) NULL COMMENT 'Entreprise (texte, pas de compte)',
  city VARCHAR(100) NULL COMMENT 'Ville',
  country CHAR(2) NULL COMMENT 'Pays (ISO 3166-1, ex. MA, FR)',
  contract_type VARCHAR(20) NOT NULL DEFAULT 'OTHER' COMMENT 'Type de contrat normalisé',
  is_remote BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Télétravail possible',
  snippet VARCHAR(1000) NULL COMMENT 'Extrait de description (HTML nettoyé)',
  salary_text VARCHAR(150) NULL COMMENT 'Salaire tel que fourni par la source',
  original_url VARCHAR(700) NOT NULL COMMENT 'Lien vers l''annonce d''origine (candidature)',
  fingerprint CHAR(64) NOT NULL COMMENT 'SHA-256(titre + entreprise + ville normalisés) : dédoublonnage inter-sources',
  status VARCHAR(10) NOT NULL DEFAULT 'ACTIVE' COMMENT 'Statut de l''offre',
  is_featured BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Mise en avant par le département',
  published_at DATETIME NULL COMMENT 'Date de publication à la source',
  expires_at DATETIME NULL COMMENT 'Date d''expiration (source ou calculée)',
  first_seen_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Première récupération',
  last_seen_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Dernière fois vue dans la source',
  source_id BIGINT NOT NULL COMMENT 'Source de l''offre',
  created_by BIGINT NULL COMMENT 'Responsable (offre saisie manuellement uniquement)',
  PRIMARY KEY (id),
  CONSTRAINT uq_offer_source_external UNIQUE (source_id, external_id),
  CONSTRAINT fk_external_offers_source_id FOREIGN KEY (source_id) REFERENCES job_sources (id) ON DELETE RESTRICT,
  CONSTRAINT fk_external_offers_created_by FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE SET NULL,
  CONSTRAINT ck_external_offers_contract_type CHECK (contract_type IN ('INTERNSHIP', 'PFE', 'CDI', 'CDD', 'ALTERNANCE', 'FREELANCE', 'OTHER')),
  CONSTRAINT ck_external_offers_status CHECK (status IN ('ACTIVE', 'EXPIRED', 'HIDDEN')),
  INDEX idx_offer_search (status, contract_type, country, published_at),
  INDEX idx_offer_fingerprint (fingerprint),
  FULLTEXT INDEX ft_offer_search (title, company_name, snippet)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Offre de stage/PFE/emploi agrégée (cache local, sources CACHED et MANUAL).';

CREATE TABLE favorite_offers (
  student_id BIGINT NOT NULL COMMENT 'Étudiant',
  offer_id BIGINT NOT NULL COMMENT 'Offre',
  saved_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Date d''ajout aux favoris',
  PRIMARY KEY (student_id, offer_id),
  CONSTRAINT fk_favorite_offers_student_id FOREIGN KEY (student_id) REFERENCES students (id) ON DELETE CASCADE,
  CONSTRAINT fk_favorite_offers_offer_id FOREIGN KEY (offer_id) REFERENCES external_offers (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Association N-N ETUDIANT–OFFRE (favoris).';

CREATE TABLE saved_searches (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Identifiant technique (AUTO_INCREMENT)',
  name VARCHAR(100) NOT NULL COMMENT 'Nom de l''alerte',
  keywords VARCHAR(255) NULL COMMENT 'Mots-clés',
  contract_types VARCHAR(100) NULL COMMENT 'Types de contrat (liste séparée par des virgules)',
  city VARCHAR(100) NULL COMMENT 'Ville',
  country CHAR(2) NULL COMMENT 'Pays (ISO 3166-1)',
  remote_only BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Télétravail uniquement',
  frequency VARCHAR(10) NOT NULL DEFAULT 'DAILY' COMMENT 'Fréquence de notification',
  active BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Alerte active',
  last_notified_at DATETIME NULL COMMENT 'Dernier envoi',
  student_id BIGINT NOT NULL COMMENT 'Propriétaire',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Date de création',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Dernière modification',
  PRIMARY KEY (id),
  CONSTRAINT fk_saved_searches_student_id FOREIGN KEY (student_id) REFERENCES students (id) ON DELETE CASCADE,
  CONSTRAINT ck_saved_searches_frequency CHECK (frequency IN ('DAILY', 'WEEKLY')),
  INDEX idx_saved_search_active (active, frequency)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Recherche enregistrée (alerte email des nouvelles offres).';

CREATE TABLE application_trackings (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Identifiant technique (AUTO_INCREMENT)',
  position_title VARCHAR(255) NOT NULL COMMENT 'Poste (copié de l''offre ou saisi)',
  company_name VARCHAR(200) NOT NULL COMMENT 'Entreprise (copiée de l''offre ou saisie)',
  offer_url VARCHAR(700) NULL COMMENT 'Lien de l''annonce',
  status VARCHAR(20) NOT NULL DEFAULT 'SENT' COMMENT 'Étape de la candidature',
  applied_at DATE NULL COMMENT 'Date d''envoi',
  next_step_at DATE NULL COMMENT 'Prochaine échéance (entretien, relance)',
  notes TEXT NULL COMMENT 'Notes personnelles',
  student_id BIGINT NOT NULL COMMENT 'Étudiant',
  offer_id BIGINT NULL COMMENT 'Offre de la plateforme (NULL si candidature trouvée ailleurs)',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Date de création',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Dernière modification',
  PRIMARY KEY (id),
  CONSTRAINT uq_tracking_student_offer UNIQUE (student_id, offer_id),
  CONSTRAINT fk_application_trackings_student_id FOREIGN KEY (student_id) REFERENCES students (id) ON DELETE CASCADE,
  CONSTRAINT fk_application_trackings_offer_id FOREIGN KEY (offer_id) REFERENCES external_offers (id) ON DELETE SET NULL,
  CONSTRAINT ck_application_trackings_status CHECK (status IN ('SENT', 'INTERVIEW', 'OFFER_RECEIVED', 'ACCEPTED', 'REJECTED', 'WITHDRAWN'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Suivi personnel d''une candidature envoyée sur un site externe.';

CREATE TABLE internships (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Identifiant technique (AUTO_INCREMENT)',
  subject VARCHAR(255) NOT NULL COMMENT 'Sujet du stage/PFE',
  internship_type VARCHAR(20) NOT NULL COMMENT 'Stage ou PFE',
  company_name VARCHAR(200) NOT NULL COMMENT 'Entreprise d''accueil',
  company_city VARCHAR(100) NULL COMMENT 'Ville',
  company_country CHAR(2) NULL COMMENT 'Pays (ISO 3166-1)',
  start_date DATE NOT NULL COMMENT 'Début',
  end_date DATE NOT NULL COMMENT 'Fin',
  status VARCHAR(30) NOT NULL DEFAULT 'PENDING_VALIDATION' COMMENT 'État d''avancement du dossier',
  company_tutor_name VARCHAR(150) NULL COMMENT 'Tuteur en entreprise',
  company_tutor_email VARCHAR(150) NULL COMMENT 'Email du tuteur',
  company_tutor_phone VARCHAR(30) NULL COMMENT 'Téléphone du tuteur',
  agreement_file_path VARCHAR(500) NOT NULL COMMENT 'Convention de stage déposée (PDF)',
  declared_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Date de déclaration par l''étudiant',
  validation_comment VARCHAR(500) NULL COMMENT 'Commentaire du département',
  validated_at DATETIME NULL COMMENT 'Date de validation',
  student_id BIGINT NOT NULL COMMENT 'Étudiant déclarant',
  tracking_id BIGINT NULL COMMENT 'Candidature suivie d''origine (facultatif)',
  validated_by BIGINT NULL COMMENT 'Responsable ayant validé',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Date de création',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Dernière modification',
  PRIMARY KEY (id),
  CONSTRAINT uq_internships_tracking_id UNIQUE (tracking_id),
  CONSTRAINT fk_internships_student_id FOREIGN KEY (student_id) REFERENCES students (id) ON DELETE RESTRICT,
  CONSTRAINT fk_internships_tracking_id FOREIGN KEY (tracking_id) REFERENCES application_trackings (id) ON DELETE SET NULL,
  CONSTRAINT fk_internships_validated_by FOREIGN KEY (validated_by) REFERENCES users (id) ON DELETE SET NULL,
  CONSTRAINT ck_internships_internship_type CHECK (internship_type IN ('INTERNSHIP', 'PFE')),
  CONSTRAINT ck_internships_status CHECK (status IN ('PENDING_VALIDATION', 'VALIDATED', 'REJECTED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
  CONSTRAINT ck_internship_dates CHECK (end_date >= start_date),
  INDEX idx_internship_student (student_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Stage/PFE déclaré par l''étudiant (avec convention), à valider par le département.';

CREATE TABLE internship_supervisors (
  internship_id BIGINT NOT NULL COMMENT 'Stage/PFE',
  teacher_id BIGINT NOT NULL COMMENT 'Enseignant encadrant',
  supervisor_role VARCHAR(20) NOT NULL DEFAULT 'MAIN' COMMENT 'Encadrant principal ou co-encadrant',
  assigned_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Date d''affectation',
  PRIMARY KEY (internship_id, teacher_id),
  CONSTRAINT fk_internship_supervisors_internship_id FOREIGN KEY (internship_id) REFERENCES internships (id) ON DELETE CASCADE,
  CONSTRAINT fk_internship_supervisors_teacher_id FOREIGN KEY (teacher_id) REFERENCES teachers (id) ON DELETE RESTRICT,
  CONSTRAINT ck_internship_supervisors_supervisor_role CHECK (supervisor_role IN ('MAIN', 'CO_SUPERVISOR'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Association N-N ENSEIGNANT–STAGE (encadrement académique) porteuse du rôle.';

CREATE TABLE internship_followups (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Identifiant technique (AUTO_INCREMENT)',
  followup_type VARCHAR(20) NOT NULL COMMENT 'Nature du suivi',
  followup_date DATE NOT NULL COMMENT 'Date',
  progress_percent TINYINT NULL COMMENT 'Avancement (%)',
  content TEXT NULL COMMENT 'Contenu / compte rendu',
  attachment_path VARCHAR(500) NULL COMMENT 'Pièce jointe (rapport…)',
  internship_id BIGINT NOT NULL COMMENT 'Stage/PFE',
  author_id BIGINT NOT NULL COMMENT 'Auteur de l''entrée',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Date de création',
  PRIMARY KEY (id),
  CONSTRAINT fk_internship_followups_internship_id FOREIGN KEY (internship_id) REFERENCES internships (id) ON DELETE CASCADE,
  CONSTRAINT fk_internship_followups_author_id FOREIGN KEY (author_id) REFERENCES users (id) ON DELETE RESTRICT,
  CONSTRAINT ck_internship_followups_followup_type CHECK (followup_type IN ('MEETING', 'PROGRESS_REPORT', 'FINAL_REPORT', 'COMMENT')),
  CONSTRAINT ck_internship_followups_progress_percent CHECK (progress_percent IS NULL OR progress_percent BETWEEN 0 AND 100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Étape de suivi d''un stage/PFE (réunion, rapport, avancement).';

CREATE TABLE defenses (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Identifiant technique (AUTO_INCREMENT)',
  defense_date DATE NOT NULL COMMENT 'Date de soutenance',
  start_time TIME NOT NULL COMMENT 'Heure de début',
  end_time TIME NULL COMMENT 'Heure de fin',
  mode VARCHAR(10) NOT NULL DEFAULT 'ONSITE' COMMENT 'Présentiel ou en ligne',
  meeting_link VARCHAR(255) NULL COMMENT 'Lien de visioconférence',
  final_score DECIMAL(5,2) NULL COMMENT 'Note finale /20',
  mention VARCHAR(30) NULL COMMENT 'Mention',
  status VARCHAR(20) NOT NULL DEFAULT 'PLANNED' COMMENT 'Statut',
  internship_id BIGINT NOT NULL COMMENT 'Stage/PFE soutenu',
  salle_id BIGINT NULL COMMENT 'Salle (NULL si en ligne)',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Date de création',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Dernière modification',
  PRIMARY KEY (id),
  CONSTRAINT uq_defenses_internship_id UNIQUE (internship_id),
  CONSTRAINT fk_defenses_internship_id FOREIGN KEY (internship_id) REFERENCES internships (id) ON DELETE RESTRICT,
  CONSTRAINT fk_defenses_salle_id FOREIGN KEY (salle_id) REFERENCES salles (id) ON DELETE SET NULL,
  CONSTRAINT ck_defenses_mode CHECK (mode IN ('ONSITE', 'ONLINE')),
  CONSTRAINT ck_defenses_final_score CHECK (final_score IS NULL OR final_score BETWEEN 0 AND 20),
  CONSTRAINT ck_defenses_status CHECK (status IN ('PLANNED', 'DONE', 'POSTPONED', 'CANCELLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Soutenance d''un stage/PFE.';

CREATE TABLE defense_jury_members (
  defense_id BIGINT NOT NULL COMMENT 'Soutenance',
  teacher_id BIGINT NOT NULL COMMENT 'Membre du jury',
  jury_role VARCHAR(20) NOT NULL COMMENT 'Rôle dans le jury',
  PRIMARY KEY (defense_id, teacher_id),
  CONSTRAINT fk_defense_jury_members_defense_id FOREIGN KEY (defense_id) REFERENCES defenses (id) ON DELETE CASCADE,
  CONSTRAINT fk_defense_jury_members_teacher_id FOREIGN KEY (teacher_id) REFERENCES teachers (id) ON DELETE RESTRICT,
  CONSTRAINT ck_defense_jury_members_jury_role CHECK (jury_role IN ('PRESIDENT', 'EXAMINER', 'SUPERVISOR', 'RAPPORTEUR'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Association N-N ENSEIGNANT–SOUTENANCE (composition du jury).';


-- -----------------------------------------------------------------------------
-- Vue : moyenne pondérée par module et par étudiant (notes publiées uniquement).
-- Les résultats sont DÉRIVÉS des notes : ils ne sont pas stockés (3FN).
-- -----------------------------------------------------------------------------
CREATE OR REPLACE VIEW v_student_module_averages AS
SELECT g.student_id,
       e.course_module_id,
       e.academic_year_id,
       ROUND(SUM(g.score * e.weight) / NULLIF(SUM(e.weight), 0), 2) AS average_score
FROM grades g
JOIN examens e ON e.id = g.examen_id
WHERE g.status = 'PUBLISHED' AND g.score IS NOT NULL
GROUP BY g.student_id, e.course_module_id, e.academic_year_id;


SET FOREIGN_KEY_CHECKS = 1;
