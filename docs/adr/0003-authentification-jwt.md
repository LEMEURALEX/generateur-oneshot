# ADR-0003 : authentification JWT maison, refresh token rotatif et mode anonyme

- Statut : accepté
- Date : 2026-09-29

## Contexte
Le choix s'est porté sur un JWT maison plutôt que Keycloak. Il faut gérer à la fois des comptes
(email et mot de passe) et des sessions anonymes temporaires, convertibles en compte.

## Décision
- **Jeton d'accès** : JWT signé en **RS256**, émis par `NimbusJwtEncoder` et validé par le
  serveur de ressources OAuth2 de Spring Security.
  - Durée de validité : 15 min.
  - Claims : `sub` (id utilisateur), `role`, `iat`, `exp`, `jti`.
  - Côté frontend, le jeton est conservé **en mémoire uniquement**.
- **Refresh token** : valeur opaque aléatoire de 256 bits, stockée **hachée (SHA-256)** dans
  `refresh_token`.
  - Transport : cookie `__Secure-refresh` (HttpOnly, Secure, SameSite=Strict,
    Path=/api/v1/auth).
  - Durée de validité : 7 jours pour un compte, 24 h pour une session anonyme.
  - **Rotation** à chaque utilisation. Si un jeton déjà consommé est présenté, toute sa famille
    est révoquée (détection de vol).
  - L'en-tête `X-Requested-With: fetch` est obligatoire sur `/auth/refresh` et
    `/auth/logout`, en protection CSRF complémentaire.
- **Mots de passe** : Argon2id (`Argon2PasswordEncoder`), 12 caractères minimum, vérification
  contre une liste de mots de passe courants.
- **Mode anonyme** : `POST /auth/anonymous` après vérification Cloudflare Turnstile. Cela crée
  un `app_user` avec le rôle `ANONYMOUS`, sans email.
  - `register` avec un jeton anonyme : la **même ligne** est convertie en `USER`, et le champ
    `expires_at` des one-shots est remis à NULL.
  - `login` avec un jeton anonyme : le one-shot anonyme est transféré au compte, puis
    l'utilisateur anonyme est supprimé.
- **Anti-bruteforce** : Bucket4j limite les tentatives par IP et par email sur
  `login`, `register` et `anonymous`. Le message d'erreur reste générique : « Identifiants
  incorrects ».
- **Clés** : paire RSA fournie par variables d'environnement (PEM). Le champ `kid` dans l'en-tête
  permet d'effectuer une rotation des clés sans coupure.

## Alternatives écartées
- **Keycloak** : plus formateur sur OIDC, mais c'est un service lourd de plus sur la VM gratuite.
- **HS256** : plus simple, mais un secret partagé ne permet pas d'exposer une clé publique ni de faire une rotation propre.
- **Jeton stocké dans `localStorage`** : exposé en cas de faille XSS.

## Conséquences
- L'application n'est pas vulnérable au CSRF sur l'API, car le jeton d'accès passe dans l'en-tête `Authorization`.
- Au rechargement de la page, le frontend appelle `/auth/refresh` pour obtenir un nouveau jeton d'accès.
- Une tâche planifiée purge les refresh tokens expirés et les utilisateurs anonymes inactifs.
