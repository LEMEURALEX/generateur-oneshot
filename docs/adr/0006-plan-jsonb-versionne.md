# ADR-0006 : plan versionné en JSONB, verrous et commentaires relationnels

- Statut : accepté
- Date : 2026-09-29

## Contexte
Le plan est un document hiérarchique produit par l'IA, lu en entier et rarement interrogé
champ par champ. L'utilisateur doit pouvoir revenir à une version précédente. Les verrous et
les commentaires portent sur des éléments du plan qui gardent leur identité d'une version à
l'autre.

## Décision
- Le **contenu du plan** est stocké en `jsonb` dans `oneshot_version.content`, une ligne par
  version, en ajout seul (append-only). `oneshot.current_version` pointe vers la version
  courante.
- Mapping Hibernate : `@JdbcTypeCode(SqlTypes.JSON)` vers des `record` Java. Un test vérifie
  que leur sérialisation respecte `plan.schema.json`.
- Les **métadonnées** interrogeables (titre, langue, dates, propriétaire, expiration) sont
  stockées dans des colonnes relationnelles.
- Les **identifiants d'élément** sont stables et lisibles (`scene-1`, `npc-3`, etc.). Ils sont
  attribués par le moteur de règles, conservés par l'IA et vérifiés par le contrôle de
  cohérence.
- **Verrous** (`element_lock`) et **commentaires** (`plan_comment`) sont rattachés au one-shot
  et à l'`element_id`, et non à une version. Verrouiller un élément ne crée pas de version.
- **Restaurer** une version copie son contenu dans une nouvelle version (`RESTORE`), sans
  jamais réécrire l'historique.
- **Verrouillage optimiste** : colonne `version` sur `oneshot` et sur les entités de la bibliothèque. En cas
  de conflit, l'API répond 409 `STALE_VERSION`.

## Alternatives écartées
- **Modèle entièrement relationnel** (tables `scene`, `npc`, etc.) : il faudrait dupliquer
  toutes les tables pour chaque version et écrire des jointures lourdes pour un document lu en
  bloc.
- **Base orientée documents (MongoDB)** : une deuxième base à exploiter, et PostgreSQL JSONB
  couvre déjà le besoin.

## Conséquences
- Une migration de `schemaVersion` impose de transformer les JSON existants, avec une
  migration Flyway Java ou une conversion à la lecture.
- Volume : environ 30 Ko par version. C'est négligeable, mais le nombre de versions pourra être
  plafonné (par exemple 50 par one-shot).
