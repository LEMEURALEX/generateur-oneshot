# ADR-0005 : file de traitements en base de données et progression en SSE

- Statut : accepté
- Date : 2026-09-29

## Contexte
Un appel au LLM dure plusieurs minutes sur CPU, et le serveur ne doit exécuter qu'une seule
inférence à la fois. Les requêtes HTTP ne doivent pas rester bloquées. L'utilisateur doit voir
la progression et pouvoir annuler.

## Décision
- Chaque opération IA (`GENERATE`, `REROLL`, `APPLY_COMMENTS`, `CHAT`) crée une ligne dans
  `ai_job`, dans la même transaction que ses effets métier (création du one-shot, message du
  chat). L'API répond `202 Accepted` avec l'identifiant du job.
- **Worker** : un pool de taille `app.jobs.worker-threads` (1 en production) réclame les jobs
  avec `SELECT ... FOR UPDATE SKIP LOCKED` par ordre de création. Un **heartbeat** est mis à
  jour pendant l'exécution. Au démarrage et toutes les 5 min, les jobs `RUNNING` sans heartbeat
  depuis plus de 10 min sont remis en file (au maximum 2 tentatives) ou passés en échec.
- **Unicité** :
  - un seul job actif par one-shot (index unique partiel) ; sinon 409 `JOB_ALREADY_ACTIVE` ;
  - l'en-tête `Idempotency-Key` évite les doublons lors des nouvelles tentatives du client.
- **Quotas** : ils sont comptés sur `ai_job`, par utilisateur et par jour, et par hash HMAC de
  l'IP pour le mode anonyme.
- **Annulation** : le champ `cancel_requested` est lu entre chaque étape et chaque appel au LLM.
- **Diffusion** : un bus d'événements en mémoire (instance unique) alimente des `SseEmitter`.
  - Événements : `progress`, `token`, `completed`, `failed`, `heartbeat`.
  - À la connexion, l'état courant est lu en base puis envoyé en premier (reprise après
    coupure grâce à `Last-Event-ID`).
- Côté frontend, `@microsoft/fetch-event-source` est utilisé : l'`EventSource` natif ne permet
  pas d'envoyer l'en-tête `Authorization`.

## Alternatives écartées
- **RabbitMQ, Kafka ou Redis** : un composant de plus à exploiter, pour un débit de quelques jobs par heure.
- **WebSocket** : bidirectionnel, ce qui n'est pas nécessaire ici. SSE suffit et reste plus simple derrière Caddy.
- **`@Async` sans persistance** : les jobs seraient perdus au redémarrage, sans position dans la file ni quotas fiables.

## Conséquences
- Le passage à plusieurs instances imposerait de remplacer le bus en mémoire, par exemple par
  PostgreSQL `LISTEN/NOTIFY`.
- Caddy doit désactiver la mise en mémoire tampon (buffering) sur `/api/v1/jobs/*/events`.
