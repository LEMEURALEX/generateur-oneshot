# ADR-0001 : monolithe modulaire dans un monorepo

- Statut : accepté
- Date : 2026-09-29

## Contexte
Il y a un seul développeur, l'objectif est l'apprentissage et l'application tourne sur une seule VM gratuite.
Il faut tout de même des frontières claires entre les domaines : identité, génération, IA, plan,
bibliothèque et export.

## Décision
- Un **monorepo** contenant `backend/` (Maven, Spring Boot), `frontend/` (Vite), `infra/`,
  `docs/` et `.github/`.
- Le backend est un **monolithe modulaire**, avec un package par module sous `com.oneshot`.
  Chaque module n'expose qu'un package `api` (interfaces et DTO publics). Tout le reste est
  interne (`internal`).
- Les règles de dépendance sont vérifiées par **ArchUnit** en CI :
  - `rules` ne dépend d'aucun autre module ni d'aucun framework (Java pur) ;
  - `ai` ne dépend que de `rules.api` et de `shared` ;
  - aucun module n'accède directement aux repositories d'un autre module ;
  - les contrôleurs n'exposent jamais d'entités JPA.
- Les interactions entre modules passent par des appels de services (`api`) ou par des
  événements applicatifs Spring pour les effets secondaires (ex. `OneShotVersionCreated`).

## Alternatives écartées
- **Microservices** : trop coûteux à exploiter pour une seule personne et une seule VM, sans
  besoin de montée en charge indépendante.
- **Spring Modulith** : intéressant, mais ArchUnit rend les règles explicites et mieux
  comprises. On pourra y migrer plus tard.

## Conséquences
- Un seul artefact à déployer et une seule base de données.
- Les frontières sont testées en CI, ce qui permet d'extraire un module plus tard si besoin
  (par exemple un worker IA séparé).
