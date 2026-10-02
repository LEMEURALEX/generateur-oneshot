# ADR-0009 : stratégie qualité et seuils bloquants

- Statut : accepté
- Date : 2026-09-29

## Contexte
Le projet est un projet d'apprentissage avec un niveau d'exigence de qualité maximal. Les
seuils doivent être automatisés et bloquants dès le premier commit.

## Décision

| Domaine | Outil | Seuil bloquant en CI |
|---|---|---|
| Formatage Java | Spotless (google-java-format) | Aucun écart |
| Style et bugs Java | Checkstyle, SpotBugs et FindSecBugs | 0 violation |
| Architecture | ArchUnit | Toutes les règles de l'ADR-0001 |
| Tests backend | JUnit 5, AssertJ, Mockito, Testcontainers (PostgreSQL) | 100 % des tests au vert |
| Couverture backend | JaCoCo | Lignes ≥ 85 %, branches ≥ 80 % ; module `rules` : 100 % des lignes |
| Mutation | PIT | `rules` ≥ 90 %, autres modules ≥ 70 % |
| Contrat | Redocly lint, tests de compatibilité springdoc | 0 erreur |
| Frontend | ESLint (strict, jsx-a11y), Prettier, `tsc --noEmit` | 0 erreur, 0 avertissement |
| Tests frontend | Vitest, Testing Library, MSW | Couverture ≥ 80 % |
| Tests de bout en bout (e2e) | Playwright avec faux Ollama (stub) | Parcours : génération, commentaire, chat, verrouillage, PDF, mode anonyme puis inscription |
| Accessibilité | axe-core (Playwright) | 0 violation sérieuse ou critique |
| Analyse globale | SonarQube Cloud | Quality gate « Sonar way » sur le nouveau code |
| Sécurité | gitleaks, CodeQL, Trivy, OWASP ZAP (baseline) | 0 secret, 0 vulnérabilité critique ou haute non justifiée |
| Commits | Conventional Commits (commitlint) | Format respecté |

- **Tests de l'IA** : la CI n'appelle jamais de vrai modèle. Elle utilise un faux (fake) du
  port `NarrativeGenerator`, des réponses Ollama enregistrées (WireMock) et des cas de sortie
  invalide pour tester les relances.
- **Évaluation hors CI** : un workflow manuel exécute un jeu de 20 demandes de référence sur
  un vrai modèle et produit un rapport : taux de JSON valide, durée, respect des verrous.

## Conséquences
- Le démarrage est plus lent, mais c'est l'objectif pédagogique.
- Les seuils peuvent être relevés, jamais abaissés sans nouvel ADR.
