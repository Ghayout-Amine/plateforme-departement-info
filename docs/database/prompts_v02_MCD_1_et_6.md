## Prompt 1/6 — Sécurité, administration, partie publique et veille (v02)

Fichier image correspondant : `MCD_1_Securite_Public.png`

```text
Génère un diagramme MCD (Modèle Conceptuel de Données, formalisme Merise) dans le style de SAP PowerDesigner (Conceptual Data Model). Titre du diagramme : « MCD 1/6 — Sécurité, administration, partie publique et veille (v02) ».

STYLE À RESPECTER : identique aux autres prompts (entités jaunes avec id <pi>, cardinalités à chaque extrémité, associations porteuses en rectangle arrondi orange, raccourcis gris en pointillés, aucune clé étrangère).

ENTITÉS (nom : attributs avec types)
- UTILISATEUR : id <pi> [Long integer], email [Variable characters (150)], password_hash [Variable characters (255)], first_name [Variable characters (80)], last_name [Variable characters (80)], phone [Variable characters (30)], avatar_url [Variable characters (255)], enabled [Boolean], last_login_at [Date & time], deleted_at [Date & time]
- ROLE : id <pi> [Long integer], name [Variable characters (50)], description [Variable characters (255)]
- PERMISSION : id <pi> [Long integer], code [Variable characters (80)], description [Variable characters (255)]
- JETON : id <pi> [Long integer], token_type [Variable characters (20)], token_hash [Variable characters (255)], expires_at [Date & time], revoked_at [Date & time]
- JOURNAL_ACTIVITE : id <pi> [Long integer], action [Variable characters (80)], entity_type [Variable characters (80)], entity_id [Long integer], details [Text], ip_address [Variable characters (45)]
- PARAMETRE_SYSTEME : id <pi> [Long integer], param_key [Variable characters (100)], param_value [Variable characters (1000)], description [Variable characters (255)]
- ACTUALITE : id <pi> [Long integer], title [Variable characters (200)], slug [Variable characters (220)], summary [Variable characters (500)], content [Text], category [Variable characters (20)], visibility [Variable characters (20)], status [Variable characters (20)], is_pinned [Boolean], cover_image_url [Variable characters (255)], published_at [Date & time]
- MESSAGE_CONTACT : id <pi> [Long integer], full_name [Variable characters (150)], email [Variable characters (150)], subject [Variable characters (200)], message [Text], status [Variable characters (20)]
- FLUX_ACTUALITES : id <pi> [Long integer], name [Variable characters (150)], feed_url [Variable characters (500)], feed_type [Variable characters (10)], category [Variable characters (20)], enabled [Boolean], sync_interval_minutes [Short integer], last_sync_at [Date & time], last_status [Variable characters (10)], last_error [Variable characters (500)]
- ARTICLE_EXTERNE : id <pi> [Long integer], title [Variable characters (300)], summary [Variable characters (1000)], image_url [Variable characters (500)], original_url [Variable characters (700)], url_hash [Characters (64)], author [Variable characters (150)], category [Variable characters (20)], status [Variable characters (10)], published_at [Date & time], fetched_at [Date & time]
- ABONNEMENT : id <pi> [Long integer], category [Variable characters (20)], channel [Variable characters (10)]

ENTITÉS RACCOURCIS d'autres blocs (sans attributs) : (aucune)

RELATIONS (Entité A (cardinalité de A) —[NOM]— Entité B (cardinalité de B))
- UTILISATEUR (1,n) —[POSSEDE_ROLE]— ROLE (0,n)
- ROLE (0,n) —[ACCORDE]— PERMISSION (0,n)
- UTILISATEUR (0,n) —[DETIENT]— JETON (1,1)
- UTILISATEUR (0,n) —[EFFECTUE]— JOURNAL_ACTIVITE (0,1)
- UTILISATEUR (0,n) —[MODIFIE]— PARAMETRE_SYSTEME (0,1)
- UTILISATEUR (0,n) —[REDIGE]— ACTUALITE (1,1)
- UTILISATEUR (0,n) —[TRAITE]— MESSAGE_CONTACT (0,1)
- UTILISATEUR (0,n) —[AJOUTE_FLUX]— FLUX_ACTUALITES (0,1)
- FLUX_ACTUALITES (0,n) —[FOURNIT]— ARTICLE_EXTERNE (1,1)
- ARTICLE_EXTERNE (0,1) —[PROMU_EN]— ACTUALITE (0,1)
- UTILISATEUR (0,n) —[S_ABONNE]— ABONNEMENT (1,1)

ASSOCIATIONS PORTEUSES D'ATTRIBUTS (rectangle arrondi)
- (aucune)

CONTRÔLE FINAL : relis chaque relation et vérifie que chaque cardinalité est écrite près de la bonne entité. Ne rajoute ni entité, ni attribut, ni relation.
```


---

## Prompt 6/6 — Stages, PFE et offres externes (v02)

Fichier image correspondant : `MCD_6_Stages_PFE.png`

```text
Génère un diagramme MCD (Modèle Conceptuel de Données, formalisme Merise) dans le style de SAP PowerDesigner (Conceptual Data Model). Titre du diagramme : « MCD 6/6 — Stages, PFE et offres externes (v02) ».

STYLE À RESPECTER : identique aux autres prompts (entités jaunes avec id <pi>, cardinalités à chaque extrémité, associations porteuses en rectangle arrondi orange, raccourcis gris en pointillés, aucune clé étrangère).

ENTITÉS (nom : attributs avec types)
- SOURCE_OFFRES : id <pi> [Long integer], code [Variable characters (30)], name [Variable characters (100)], website_url [Variable characters (255)], access_mode [Variable characters (10)], enabled [Boolean], api_key_ref [Variable characters (80)], sync_interval_minutes [Short integer], default_keywords [Variable characters (500)], default_locations [Variable characters (500)], quota_limit [Integer], quota_period [Variable characters (10)], attribution_text [Variable characters (255)], logo_url [Variable characters (255)], last_sync_at [Date & time]
- JOURNAL_SYNCHRO : id <pi> [Long integer], started_at [Date & time], finished_at [Date & time], status [Variable characters (10)], fetched_count [Integer], created_count [Integer], updated_count [Integer], expired_count [Integer], quota_remaining [Integer], error_message [Variable characters (1000)]
- OFFRE_EXTERNE : id <pi> [Long integer], external_id [Variable characters (150)], title [Variable characters (255)], company_name [Variable characters (200)], city [Variable characters (100)], country [Characters (2)], contract_type [Variable characters (20)], is_remote [Boolean], snippet [Variable characters (1000)], salary_text [Variable characters (150)], original_url [Variable characters (700)], fingerprint [Characters (64)], status [Variable characters (10)], is_featured [Boolean], published_at [Date & time], expires_at [Date & time], first_seen_at [Date & time], last_seen_at [Date & time]
- ALERTE : id <pi> [Long integer], name [Variable characters (100)], keywords [Variable characters (255)], contract_types [Variable characters (100)], city [Variable characters (100)], country [Characters (2)], remote_only [Boolean], frequency [Variable characters (10)], active [Boolean], last_notified_at [Date & time]
- SUIVI_CANDIDATURE : id <pi> [Long integer], position_title [Variable characters (255)], company_name [Variable characters (200)], offer_url [Variable characters (700)], status [Variable characters (20)], applied_at [Date], next_step_at [Date], notes [Text]
- STAGE : id <pi> [Long integer], subject [Variable characters (255)], internship_type [Variable characters (20)], company_name [Variable characters (200)], company_city [Variable characters (100)], company_country [Characters (2)], start_date [Date], end_date [Date], status [Variable characters (30)], company_tutor_name [Variable characters (150)], company_tutor_email [Variable characters (150)], company_tutor_phone [Variable characters (30)], agreement_file_path [Variable characters (500)], declared_at [Date & time], validation_comment [Variable characters (500)], validated_at [Date & time]
- SUIVI_STAGE : id <pi> [Long integer], followup_type [Variable characters (20)], followup_date [Date], progress_percent [Short integer], content [Text], attachment_path [Variable characters (500)]
- SOUTENANCE : id <pi> [Long integer], defense_date [Date], start_time [Time], end_time [Time], mode [Variable characters (10)], meeting_link [Variable characters (255)], final_score [Decimal (5,2)], mention [Variable characters (30)], status [Variable characters (20)]

ENTITÉS RACCOURCIS d'autres blocs (sans attributs) : UTILISATEUR, ETUDIANT, ENSEIGNANT, SALLE

RELATIONS (Entité A (cardinalité de A) —[NOM]— Entité B (cardinalité de B))
- SOURCE_OFFRES (0,n) —[JOURNALISE]— JOURNAL_SYNCHRO (1,1)
- SOURCE_OFFRES (0,n) —[FOURNIT]— OFFRE_EXTERNE (1,1)
- UTILISATEUR (0,n) —[SAISIT]— OFFRE_EXTERNE (0,1)
- ETUDIANT (0,n) —[CREE_ALERTE]— ALERTE (1,1)
- ETUDIANT (0,n) —[SUIT]— SUIVI_CANDIDATURE (1,1)
- OFFRE_EXTERNE (0,n) —[CONCERNE]— SUIVI_CANDIDATURE (0,1)
- ETUDIANT (0,n) —[DECLARE]— STAGE (1,1)
- SUIVI_CANDIDATURE (0,1) —[ABOUTIT_A]— STAGE (0,1)
- UTILISATEUR (0,n) —[VALIDE]— STAGE (0,1)
- STAGE (0,n) —[FAIT_L_OBJET_DE]— SUIVI_STAGE (1,1)
- UTILISATEUR (0,n) —[REDIGE_SUIVI]— SUIVI_STAGE (1,1)
- STAGE (0,1) —[CONCLUT]— SOUTENANCE (1,1)
- SALLE (0,n) —[SE_DEROULE_EN]— SOUTENANCE (0,1)

ASSOCIATIONS PORTEUSES D'ATTRIBUTS (rectangle arrondi)
- FAVORI — attributs : saved_at [Date & time] ; reliée à ETUDIANT (cardinalité 0,n) et à OFFRE_EXTERNE (cardinalité 0,n)
- ENCADRE — attributs : supervisor_role [Variable characters (20)], assigned_at [Date & time] ; reliée à STAGE (cardinalité 0,n) et à ENSEIGNANT (cardinalité 0,n)
- COMPOSE_LE_JURY — attributs : jury_role [Variable characters (20)] ; reliée à SOUTENANCE (cardinalité 1,n) et à ENSEIGNANT (cardinalité 0,n)

CONTRÔLE FINAL : relis chaque relation et vérifie que chaque cardinalité est écrite près de la bonne entité. Ne rajoute ni entité, ni attribut, ni relation.
```
