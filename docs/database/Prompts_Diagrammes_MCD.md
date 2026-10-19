# Prompts pour générer les 6 diagrammes MCD (style PowerDesigner)

Projet PGDI2026 — Story PGDI2026-11. Ces prompts sont générés à partir du même modèle que le dossier de conception et le script SQL (36 tables), donc cohérents avec eux.

## Comment les utiliser

1. Ouvrez l'outil de diagrammes IA de votre choix (par exemple Eraser, Lucidchart, Miro, draw.io ou un chatbot qui produit un diagramme), puis collez **un prompt à la fois** (un prompt = un diagramme).
2. Si le résultat est trop chargé, demandez : « garde la même structure mais agrandis l'espacement et évite les croisements de traits ».
3. **Vérifiez toujours les cardinalités** : les outils IA les inversent souvent. Utilisez la phrase de contrôle en fin de prompt, et comparez avec le tableau du §3.3 du dossier.
4. Je n'ai pas testé ces sites : le rendu exact dépend de chacun. Pour un MCD strictement identique à PowerDesigner, il faut le logiciel lui-même (Modèle conceptuel de données) ou un outil Merise dédié (Looping, JMerise) : saisissez-y les entités et relations listées ci-dessous.

Légende des cardinalités (min,max) : **0,1** zéro ou une ; **1,1** exactement une ; **0,n** zéro ou plusieurs ; **1,n** au moins une. La cardinalité écrite près d'une entité dit combien de fois une occurrence de cette entité participe à la relation. Exemple : FORMATION (0,n) — CONTIENT — FILIERE (1,1) = une formation contient 0 à n filières ; une filière appartient à exactement une formation.


---

## Prompt 1/6 — Sécurité, administration et partie publique

Fichier image correspondant : `MCD_1_Securite_Public.png`

```text
Génère un diagramme MCD (Modèle Conceptuel de Données, formalisme Merise) dans le style de SAP PowerDesigner (Conceptual Data Model). Titre du diagramme : « MCD 1/6 — Sécurité, administration et partie publique ».

STYLE À RESPECTER
- Chaque entité = rectangle jaune clair, nom en gras et centré en en-tête, puis l'attribut identifiant `id` (type Long integer) marqué <pi> dans une zone séparée par un trait, puis la liste des autres attributs avec leur type.
- Chaque relation = un trait entre deux entités, avec le nom de la relation au milieu du trait, et la cardinalité (min,max) écrite à CHAQUE extrémité, près de l'entité concernée. Pas d'ovale, pas de losange.
- Une association porteuse d'attributs = rectangle arrondi orange pâle contenant ses attributs, relié aux deux entités par deux traits portant chacun une cardinalité.
- Les entités qui appartiennent à un autre bloc sont de petits rectangles gris en pointillés, nommés « (raccourci) », sans attributs.
- Modèle conceptuel : n'affiche aucune clé étrangère dans les entités.
- Disposition aérée, lisible, sans croisement de traits si possible, police lisible.

ENTITÉS (nom : attributs avec types)
- UTILISATEUR : id <pi> [Long integer], email [Variable characters (150)], password_hash [Variable characters (255)], first_name [Variable characters (80)], last_name [Variable characters (80)], phone [Variable characters (30)], avatar_url [Variable characters (255)], enabled [Boolean], last_login_at [Date & time], deleted_at [Date & time]
- ROLE : id <pi> [Long integer], name [Variable characters (50)], description [Variable characters (255)]
- PERMISSION : id <pi> [Long integer], code [Variable characters (80)], description [Variable characters (255)]
- JETON : id <pi> [Long integer], token_type [Variable characters (20)], token_hash [Variable characters (255)], expires_at [Date & time], revoked_at [Date & time]
- JOURNAL_ACTIVITE : id <pi> [Long integer], action [Variable characters (80)], entity_type [Variable characters (80)], entity_id [Long integer], details [Text], ip_address [Variable characters (45)]
- PARAMETRE_SYSTEME : id <pi> [Long integer], param_key [Variable characters (100)], param_value [Variable characters (1000)], description [Variable characters (255)]
- ACTUALITE : id <pi> [Long integer], title [Variable characters (200)], slug [Variable characters (220)], summary [Variable characters (500)], content [Text], category [Variable characters (20)], visibility [Variable characters (20)], status [Variable characters (20)], cover_image_url [Variable characters (255)], published_at [Date & time]
- MESSAGE_CONTACT : id <pi> [Long integer], full_name [Variable characters (150)], email [Variable characters (150)], subject [Variable characters (200)], message [Text], status [Variable characters (20)]

RELATIONS (Entité A (cardinalité de A) —[NOM]— Entité B (cardinalité de B))
- UTILISATEUR (1,n) —[POSSEDE_ROLE]— ROLE (0,n)
- ROLE (0,n) —[ACCORDE]— PERMISSION (0,n)
- UTILISATEUR (0,n) —[DETIENT]— JETON (1,1)
- UTILISATEUR (0,n) —[EFFECTUE]— JOURNAL_ACTIVITE (0,1)
- UTILISATEUR (0,n) —[MODIFIE]— PARAMETRE_SYSTEME (0,1)
- UTILISATEUR (0,n) —[REDIGE]— ACTUALITE (1,1)
- UTILISATEUR (0,n) —[TRAITE]— MESSAGE_CONTACT (0,1)

CONTRÔLE FINAL : relis chaque relation et vérifie que chaque cardinalité est écrite près de la bonne entité, exactement comme dans la liste ci-dessus (la cardinalité d'une entité est près de cette entité, pas de l'autre). Ne rajoute ni entité, ni attribut, ni relation.

```


---

## Prompt 2/6 — Structure pédagogique

Fichier image correspondant : `MCD_2_Structure_pedagogique.png`

```text
Génère un diagramme MCD (Modèle Conceptuel de Données, formalisme Merise) dans le style de SAP PowerDesigner (Conceptual Data Model). Titre du diagramme : « MCD 2/6 — Structure pédagogique ».

STYLE À RESPECTER
- Chaque entité = rectangle jaune clair, nom en gras et centré en en-tête, puis l'attribut identifiant `id` (type Long integer) marqué <pi> dans une zone séparée par un trait, puis la liste des autres attributs avec leur type.
- Chaque relation = un trait entre deux entités, avec le nom de la relation au milieu du trait, et la cardinalité (min,max) écrite à CHAQUE extrémité, près de l'entité concernée. Pas d'ovale, pas de losange.
- Une association porteuse d'attributs = rectangle arrondi orange pâle contenant ses attributs, relié aux deux entités par deux traits portant chacun une cardinalité.
- Les entités qui appartiennent à un autre bloc sont de petits rectangles gris en pointillés, nommés « (raccourci) », sans attributs.
- Modèle conceptuel : n'affiche aucune clé étrangère dans les entités.
- Disposition aérée, lisible, sans croisement de traits si possible, police lisible.

ENTITÉS (nom : attributs avec types)
- ANNEE_UNIVERSITAIRE : id <pi> [Long integer], label [Variable characters (9)], start_date [Date], end_date [Date], is_current [Boolean]
- FORMATION : id <pi> [Long integer], code [Variable characters (30)], name [Variable characters (150)], degree_level [Variable characters (20)], duration_semesters [Short integer], description [Text], is_active [Boolean]
- FILIERE : id <pi> [Long integer], code [Variable characters (30)], name [Variable characters (150)], description [Text], is_active [Boolean]
- MODULE : id <pi> [Long integer], code [Variable characters (30)], name [Variable characters (150)], description [Text], hours_lecture [Short integer], hours_tutorial [Short integer], hours_practical [Short integer]
- GROUPE : id <pi> [Long integer], name [Variable characters (50)], capacity [Short integer]
- SALLE : id <pi> [Long integer], code [Variable characters (20)], name [Variable characters (100)], building [Variable characters (100)], capacity [Short integer], room_type [Variable characters (20)], is_active [Boolean]

RELATIONS (Entité A (cardinalité de A) —[NOM]— Entité B (cardinalité de B))
- FORMATION (0,n) —[CONTIENT]— FILIERE (1,1)
- FILIERE (0,n) —[SE_DIVISE_EN]— GROUPE (1,1)
- ANNEE_UNIVERSITAIRE (0,n) —[EST_CREE_POUR]— GROUPE (1,1)

ASSOCIATIONS PORTEUSES D'ATTRIBUTS (rectangle arrondi)
- COMPREND — attributs : semester [Short integer], credits [Short integer], coefficient [Decimal (4,2)] ; reliée à FILIERE (cardinalité 0,n) et à MODULE (cardinalité 0,n)

CONTRÔLE FINAL : relis chaque relation et vérifie que chaque cardinalité est écrite près de la bonne entité, exactement comme dans la liste ci-dessus (la cardinalité d'une entité est près de cette entité, pas de l'autre). Ne rajoute ni entité, ni attribut, ni relation.

```


---

## Prompt 3/6 — Étudiants, enseignants, inscriptions

Fichier image correspondant : `MCD_3_Etudiants_Enseignants.png`

```text
Génère un diagramme MCD (Modèle Conceptuel de Données, formalisme Merise) dans le style de SAP PowerDesigner (Conceptual Data Model). Titre du diagramme : « MCD 3/6 — Étudiants, enseignants, inscriptions ».

STYLE À RESPECTER
- Chaque entité = rectangle jaune clair, nom en gras et centré en en-tête, puis l'attribut identifiant `id` (type Long integer) marqué <pi> dans une zone séparée par un trait, puis la liste des autres attributs avec leur type.
- Chaque relation = un trait entre deux entités, avec le nom de la relation au milieu du trait, et la cardinalité (min,max) écrite à CHAQUE extrémité, près de l'entité concernée. Pas d'ovale, pas de losange.
- Une association porteuse d'attributs = rectangle arrondi orange pâle contenant ses attributs, relié aux deux entités par deux traits portant chacun une cardinalité.
- Les entités qui appartiennent à un autre bloc sont de petits rectangles gris en pointillés, nommés « (raccourci) », sans attributs.
- Modèle conceptuel : n'affiche aucune clé étrangère dans les entités.
- Disposition aérée, lisible, sans croisement de traits si possible, police lisible.

ENTITÉS (nom : attributs avec types)
- ETUDIANT : id <pi> [Long integer], student_number [Variable characters (30)], cne [Variable characters (20)], birth_date [Date], birth_place [Variable characters (100)], gender [Variable characters (10)], address [Variable characters (255)], city [Variable characters (80)]
- ENSEIGNANT : id <pi> [Long integer], employee_number [Variable characters (30)], academic_title [Variable characters (30)], specialty [Variable characters (150)], office [Variable characters (50)], hire_date [Date], bio [Text]
- INSCRIPTION : id <pi> [Long integer], status [Variable characters (20)]

ENTITÉS RACCOURCIS d'autres blocs (sans attributs) : ANNEE_UNIVERSITAIRE, FILIERE, GROUPE, UTILISATEUR

RELATIONS (Entité A (cardinalité de A) —[NOM]— Entité B (cardinalité de B))
- UTILISATEUR (0,1) —[A_POUR_COMPTE]— ETUDIANT (1,1)
- UTILISATEUR (0,1) —[A_POUR_COMPTE]— ENSEIGNANT (1,1)
- ETUDIANT (0,n) —[S_INSCRIT]— INSCRIPTION (1,1)
- FILIERE (0,n) —[CONCERNE]— INSCRIPTION (1,1)
- ANNEE_UNIVERSITAIRE (0,n) —[POUR_ANNEE]— INSCRIPTION (1,1)
- GROUPE (0,n) —[EST_AFFECTE_A]— INSCRIPTION (0,1)

CONTRÔLE FINAL : relis chaque relation et vérifie que chaque cardinalité est écrite près de la bonne entité, exactement comme dans la liste ci-dessus (la cardinalité d'une entité est près de cette entité, pas de l'autre). Ne rajoute ni entité, ni attribut, ni relation.

```


---

## Prompt 4/6 — Enseignement, emploi du temps, examens

Fichier image correspondant : `MCD_4_Enseignement_Planning_Examens.png`

```text
Génère un diagramme MCD (Modèle Conceptuel de Données, formalisme Merise) dans le style de SAP PowerDesigner (Conceptual Data Model). Titre du diagramme : « MCD 4/6 — Enseignement, emploi du temps, examens ».

STYLE À RESPECTER
- Chaque entité = rectangle jaune clair, nom en gras et centré en en-tête, puis l'attribut identifiant `id` (type Long integer) marqué <pi> dans une zone séparée par un trait, puis la liste des autres attributs avec leur type.
- Chaque relation = un trait entre deux entités, avec le nom de la relation au milieu du trait, et la cardinalité (min,max) écrite à CHAQUE extrémité, près de l'entité concernée. Pas d'ovale, pas de losange.
- Une association porteuse d'attributs = rectangle arrondi orange pâle contenant ses attributs, relié aux deux entités par deux traits portant chacun une cardinalité.
- Les entités qui appartiennent à un autre bloc sont de petits rectangles gris en pointillés, nommés « (raccourci) », sans attributs.
- Modèle conceptuel : n'affiche aucune clé étrangère dans les entités.
- Disposition aérée, lisible, sans croisement de traits si possible, police lisible.

ENTITÉS (nom : attributs avec types)
- AFFECTATION_ENSEIGNEMENT : id <pi> [Long integer], session_type [Variable characters (10)]
- SEANCE : id <pi> [Long integer], session_date [Date], start_time [Time], end_time [Time], status [Variable characters (20)], notes [Variable characters (255)]
- EXAMEN : id <pi> [Long integer], title [Variable characters (150)], exam_type [Variable characters (20)], exam_date [Date], start_time [Time], end_time [Time], weight [Decimal (5,2)]

ENTITÉS RACCOURCIS d'autres blocs (sans attributs) : ANNEE_UNIVERSITAIRE, ENSEIGNANT, GROUPE, MODULE, SALLE

RELATIONS (Entité A (cardinalité de A) —[NOM]— Entité B (cardinalité de B))
- ENSEIGNANT (0,n) —[ASSURE]— AFFECTATION_ENSEIGNEMENT (1,1)
- MODULE (0,n) —[PORTE_SUR]— AFFECTATION_ENSEIGNEMENT (1,1)
- GROUPE (0,n) —[DESTINE_A]— AFFECTATION_ENSEIGNEMENT (1,1)
- AFFECTATION_ENSEIGNEMENT (0,n) —[PLANIFIE]— SEANCE (1,1)
- SALLE (0,n) —[SE_TIENT_EN]— SEANCE (1,1)
- MODULE (0,n) —[EVALUE]— EXAMEN (1,1)
- ANNEE_UNIVERSITAIRE (0,n) —[ORGANISE_EN]— EXAMEN (1,1)
- EXAMEN (0,n) —[SE_DEROULE_DANS]— SALLE (0,n)

CONTRÔLE FINAL : relis chaque relation et vérifie que chaque cardinalité est écrite près de la bonne entité, exactement comme dans la liste ci-dessus (la cardinalité d'une entité est près de cette entité, pas de l'autre). Ne rajoute ni entité, ni attribut, ni relation.

```


---

## Prompt 5/6 — Notes, absences, ressources

Fichier image correspondant : `MCD_5_Notes_Absences_Ressources.png`

```text
Génère un diagramme MCD (Modèle Conceptuel de Données, formalisme Merise) dans le style de SAP PowerDesigner (Conceptual Data Model). Titre du diagramme : « MCD 5/6 — Notes, absences, ressources ».

STYLE À RESPECTER
- Chaque entité = rectangle jaune clair, nom en gras et centré en en-tête, puis l'attribut identifiant `id` (type Long integer) marqué <pi> dans une zone séparée par un trait, puis la liste des autres attributs avec leur type.
- Chaque relation = un trait entre deux entités, avec le nom de la relation au milieu du trait, et la cardinalité (min,max) écrite à CHAQUE extrémité, près de l'entité concernée. Pas d'ovale, pas de losange.
- Une association porteuse d'attributs = rectangle arrondi orange pâle contenant ses attributs, relié aux deux entités par deux traits portant chacun une cardinalité.
- Les entités qui appartiennent à un autre bloc sont de petits rectangles gris en pointillés, nommés « (raccourci) », sans attributs.
- Modèle conceptuel : n'affiche aucune clé étrangère dans les entités.
- Disposition aérée, lisible, sans croisement de traits si possible, police lisible.

ENTITÉS (nom : attributs avec types)
- NOTE : id <pi> [Long integer], score [Decimal (5,2)], status [Variable characters (20)], review_comment [Variable characters (255)]
- ABSENCE : id <pi> [Long integer], absence_type [Variable characters (10)], is_justified [Boolean], justification [Variable characters (500)], justification_file [Variable characters (500)]
- RESSOURCE : id <pi> [Long integer], title [Variable characters (200)], description [Text], resource_type [Variable characters (20)], file_name [Variable characters (255)], file_path [Variable characters (500)], mime_type [Variable characters (100)], file_size [Long integer], external_url [Variable characters (500)], is_published [Boolean]

ENTITÉS RACCOURCIS d'autres blocs (sans attributs) : ENSEIGNANT, ETUDIANT, EXAMEN, MODULE, SEANCE

RELATIONS (Entité A (cardinalité de A) —[NOM]— Entité B (cardinalité de B))
- ETUDIANT (0,n) —[OBTIENT]— NOTE (1,1)
- EXAMEN (0,n) —[PORTE_NOTE]— NOTE (1,1)
- ENSEIGNANT (0,n) —[SAISIT]— NOTE (0,1)
- ETUDIANT (0,n) —[EST_ABSENT]— ABSENCE (1,1)
- SEANCE (0,n) —[CONSTATEE_LORS_DE]— ABSENCE (1,1)
- ENSEIGNANT (0,n) —[ENREGISTRE]— ABSENCE (0,1)
- MODULE (0,n) —[EST_RATTACHEE_A]— RESSOURCE (1,1)
- ENSEIGNANT (0,n) —[PUBLIE]— RESSOURCE (1,1)

CONTRÔLE FINAL : relis chaque relation et vérifie que chaque cardinalité est écrite près de la bonne entité, exactement comme dans la liste ci-dessus (la cardinalité d'une entité est près de cette entité, pas de l'autre). Ne rajoute ni entité, ni attribut, ni relation.

```


---

## Prompt 6/6 — Stages, PFE, entreprises

Fichier image correspondant : `MCD_6_Stages_PFE.png`

```text
Génère un diagramme MCD (Modèle Conceptuel de Données, formalisme Merise) dans le style de SAP PowerDesigner (Conceptual Data Model). Titre du diagramme : « MCD 6/6 — Stages, PFE, entreprises ».

STYLE À RESPECTER
- Chaque entité = rectangle jaune clair, nom en gras et centré en en-tête, puis l'attribut identifiant `id` (type Long integer) marqué <pi> dans une zone séparée par un trait, puis la liste des autres attributs avec leur type.
- Chaque relation = un trait entre deux entités, avec le nom de la relation au milieu du trait, et la cardinalité (min,max) écrite à CHAQUE extrémité, près de l'entité concernée. Pas d'ovale, pas de losange.
- Une association porteuse d'attributs = rectangle arrondi orange pâle contenant ses attributs, relié aux deux entités par deux traits portant chacun une cardinalité.
- Les entités qui appartiennent à un autre bloc sont de petits rectangles gris en pointillés, nommés « (raccourci) », sans attributs.
- Modèle conceptuel : n'affiche aucune clé étrangère dans les entités.
- Disposition aérée, lisible, sans croisement de traits si possible, police lisible.

ENTITÉS (nom : attributs avec types)
- ENTREPRISE : id <pi> [Long integer], name [Variable characters (150)], registration_number [Variable characters (50)], sector [Variable characters (100)], description [Text], address [Variable characters (255)], city [Variable characters (80)], country [Variable characters (80)], website [Variable characters (255)], logo_url [Variable characters (255)], status [Variable characters (20)]
- OFFRE : id <pi> [Long integer], title [Variable characters (200)], description [Text], offer_type [Variable characters (20)], required_skills [Variable characters (500)], location [Variable characters (150)], duration_months [Short integer], start_date [Date], application_deadline [Date], stipend [Decimal (10,2)], positions [Short integer], status [Variable characters (20)], published_at [Date & time]
- CANDIDATURE : id <pi> [Long integer], cover_letter [Text], status [Variable characters (20)], company_feedback [Variable characters (500)], decided_at [Date & time]
- DOCUMENT_CANDIDATURE : id <pi> [Long integer], doc_type [Variable characters (20)], file_name [Variable characters (255)], file_path [Variable characters (500)], mime_type [Variable characters (100)], file_size [Long integer]
- STAGE : id <pi> [Long integer], subject [Variable characters (255)], internship_type [Variable characters (20)], start_date [Date], end_date [Date], status [Variable characters (30)], company_tutor_name [Variable characters (150)], company_tutor_email [Variable characters (150)], company_tutor_phone [Variable characters (30)], validation_comment [Variable characters (500)], validated_at [Date & time]
- SUIVI_STAGE : id <pi> [Long integer], followup_type [Variable characters (20)], followup_date [Date], progress_percent [Short integer], content [Text], attachment_path [Variable characters (500)]
- SOUTENANCE : id <pi> [Long integer], defense_date [Date], start_time [Time], end_time [Time], mode [Variable characters (10)], meeting_link [Variable characters (255)], final_score [Decimal (5,2)], mention [Variable characters (30)], status [Variable characters (20)]

ENTITÉS RACCOURCIS d'autres blocs (sans attributs) : ENSEIGNANT, ETUDIANT, SALLE, UTILISATEUR

RELATIONS (Entité A (cardinalité de A) —[NOM]— Entité B (cardinalité de B))
- UTILISATEUR (0,1) —[A_POUR_COMPTE]— ENTREPRISE (1,1)
- ENTREPRISE (0,n) —[PROPOSE]— OFFRE (1,1)
- OFFRE (0,n) —[RECOIT]— CANDIDATURE (1,1)
- ETUDIANT (0,n) —[DEPOSE]— CANDIDATURE (1,1)
- CANDIDATURE (0,n) —[JOINT]— DOCUMENT_CANDIDATURE (1,1)
- CANDIDATURE (0,1) —[DONNE_LIEU_A]— STAGE (1,1)
- UTILISATEUR (0,n) —[VALIDE]— STAGE (0,1)
- STAGE (0,n) —[FAIT_L_OBJET_DE]— SUIVI_STAGE (1,1)
- UTILISATEUR (0,n) —[REDIGE_SUIVI]— SUIVI_STAGE (1,1)
- STAGE (0,1) —[CONCLUT]— SOUTENANCE (1,1)
- SALLE (0,n) —[SE_DEROULE_EN]— SOUTENANCE (0,1)

ASSOCIATIONS PORTEUSES D'ATTRIBUTS (rectangle arrondi)
- ENCADRE — attributs : supervisor_role [Variable characters (20)], assigned_at [Date & time] ; reliée à STAGE (cardinalité 0,n) et à ENSEIGNANT (cardinalité 0,n)
- COMPOSE_LE_JURY — attributs : jury_role [Variable characters (20)] ; reliée à SOUTENANCE (cardinalité 1,n) et à ENSEIGNANT (cardinalité 0,n)

CONTRÔLE FINAL : relis chaque relation et vérifie que chaque cardinalité est écrite près de la bonne entité, exactement comme dans la liste ci-dessus (la cardinalité d'une entité est près de cette entité, pas de l'autre). Ne rajoute ni entité, ni attribut, ni relation.

```
