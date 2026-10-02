# ADR-0002 : contrat d'API d'abord (contract-first) avec génération de code

- Statut : accepté
- Date : 2026-09-29

## Contexte
Le frontend et le backend évoluent en parallèle. Le plan de partie est une structure riche,
partagée par l'IA (validation), l'API et le frontend. Il faut éviter les écarts de types entre
ces trois parties.

## Décision
- La **source de vérité** de l'API est [openapi.yaml](../api/openapi.yaml) (OpenAPI 3.1).
- La **source de vérité** du plan est [plan.schema.json](../schemas/plan.schema.json)
  (JSON Schema 2020-12), référencé par l'OpenAPI.
- Backend : `openapi-generator-maven-plugin` (générateur `spring`, `interfaceOnly=true`,
  `useJakartaEe=true`, option Spring Boot correspondant à la version utilisée). Les contrôleurs
  implémentent les interfaces générées. Le code généré n'est pas versionné.
- Frontend : `openapi-typescript` génère les types et `openapi-fetch` sert de client HTTP typé.
- Le backend valide la sortie de l'IA avec le même `plan.schema.json` (bibliothèque
  `networknt/json-schema-validator`).
- En CI :
  - le linting de l'OpenAPI (Redocly CLI) est bloquant ;
  - la génération du code échoue si le contrat est invalide ;
  - un test vérifie que `/v3/api-docs` (springdoc) reste compatible avec le contrat.

## Alternatives écartées
- **Code d'abord (code-first) avec springdoc seul** : c'est plus simple, mais le frontend
  dépend alors du backend déjà codé et le schéma du plan serait dupliqué.

## Conséquences
- Toute modification de l'API commence par le contrat, ce qui constitue une bonne pratique à apprendre.
- Le support d'OpenAPI 3.1 par `openapi-generator` est encore partiel (types `null`, `oneOf`).
  Si nécessaire, on ajuste la génération (mappings de types) plutôt que le contrat.
