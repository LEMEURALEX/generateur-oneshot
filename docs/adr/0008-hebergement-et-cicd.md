# ADR-0008 : hébergement Oracle Cloud Always Free, CI/CD GitHub Actions

- Statut : accepté
- Date : 2026-09-29

## Contexte
Les contraintes sont les suivantes : un coût nul, un LLM local (environ 6 Go de RAM pour un
modèle de 7-8 milliards de paramètres quantifié), un dépôt public et un seul environnement de
production.

## Décision
**Hébergement**
- Une VM **Oracle Cloud Always Free Ampere A1** (ARM64, 4 OCPU, 24 Go de RAM) sous Ubuntu 24.04 LTS.
- **Docker Compose** en production avec les services `caddy`, `backend`, `postgres`, `ollama`,
  `prometheus` et `grafana`.
  - Seul Caddy est exposé (ports 80 et 443).
  - Les autres services communiquent sur un réseau interne.
- **Caddy** :
  - HTTPS automatique (Let's Encrypt) sur un sous-domaine gratuit (DuckDNS, par exemple) ;
  - sert les fichiers statiques du frontend et redirige `/api/*` vers le backend (même
    origine, donc pas de CORS) ;
  - en-têtes de sécurité (CSP stricte, HSTS) ;
  - pas de mise en mémoire tampon pour le SSE.
- **Durcissement** : SSH par clé uniquement, `ufw` et liste de sécurité OCI (ports 22, 80 et
  443), `fail2ban`, mises à jour automatiques (`unattended-upgrades`).
- **Sauvegardes** : `pg_dump` chaque nuit, chiffré, envoyé vers OCI Object Storage (offre
  gratuite) avec `rclone`, conservé 7 jours. Un test de restauration est lancé chaque mois par
  un workflow.

**CI/CD (GitHub Actions, gratuit pour un dépôt public)**
- `ci.yml` (pull requests et `main`) :
  - backend : build Maven, Spotless, Checkstyle, SpotBugs, tests (Testcontainers), JaCoCo et PIT ;
  - frontend : lint, typecheck, Vitest avec couverture, build ;
  - linting de l'OpenAPI ;
  - SonarQube Cloud (quality gate bloquante) ;
  - gitleaks, CodeQL et Trivy (dépendances et image).
- `e2e.yml` : Docker Compose avec un faux Ollama (stub), Playwright, puis un scan de base
  (baseline) OWASP ZAP.
- `release.yml` (tag `v*`) :
  - build des images **arm64** sur les runners `ubuntu-24.04-arm`, publication sur GHCR et
    signature `cosign` ;
  - déploiement par SSH (`docker compose pull && up -d`), avec approbation manuelle via un
    environnement GitHub `production` ;
  - smoke test sur `/actuator/health` et rollback automatique vers le tag précédent en cas d'échec.
- Dependabot pour Maven, npm, Docker et Actions.
- **Secrets** stockés dans les environnements GitHub, puis écrits dans un fichier `.env` sur la
  VM (`chmod 600`). Aucun secret dans le dépôt.

## Alternatives écartées
- **Render ou Fly.io en offre gratuite** : 256 à 512 Mo de RAM, impossible d'y faire tourner un LLM.
- **Kubernetes (k3s)** : surdimensionné pour une seule VM. On pourra l'envisager plus tard comme exercice.
- **GitLab CI ou Azure DevOps** : gratuits aussi, mais GitHub est plus intégré avec Dependabot,
  CodeQL, GHCR et SonarQube Cloud.

## Conséquences et risques
- **Récupération des VM inactives** : Oracle peut récupérer une VM Always Free peu utilisée.
  Pour l'éviter, passer le compte en paiement à l'usage (Pay As You Go), qui reste gratuit dans
  les limites de l'offre Always Free.
- **Capacité ARM** : la création d'une VM ARM peut échouer faute de capacité dans la région.
  Il faut alors réessayer ou changer de domaine de disponibilité.
- **Point unique de défaillance** : c'est accepté, le service est fourni au mieux (best effort).
