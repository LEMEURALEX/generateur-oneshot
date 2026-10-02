# ADR-0007 : export PDF côté serveur (HTML vers PDF)

- Statut : accepté
- Date : 2026-09-29

## Contexte
Le PDF doit être imprimable (A4, noir et blanc lisible), bilingue, avec un sommaire et une page
par scène. Il est régénéré à la demande (RM7).

## Décision
- Un template **Thymeleaf** par langue produit du XHTML, converti par **openhtmltopdf**
  (moteur de rendu PDFBox).
- Les polices sont embarquées dans l'application, sous licence OFL : Cinzel pour les titres,
  Inter pour le texte.
- Mise en page via CSS paged media : `@page`, `page-break-before` par scène, sommaire avec
  `target-counter`.
- **Sécurité** :
  - tout contenu issu de l'IA est inséré avec `th:text` (échappement), jamais `th:utext` ;
  - un `FSUriResolver` restrictif n'autorise que les ressources du classpath (pas de SSRF) ;
  - pas de JavaScript.
- Une limite de débit spécifique s'applique, car le rendu est coûteux en CPU. La réponse est en
  streaming (`StreamingResponseBody`).
- Le PDF contient la mention d'attribution CC-BY-4.0 lorsque des tables issues du SRD ont été
  utilisées.

## Alternatives écartées
- **Rendu côté navigateur (`window.print`, jsPDF)** : rendu variable d'un navigateur à l'autre
  et moins d'apprentissage côté backend.
- **Chromium headless (Gotenberg)** : très fidèle, mais c'est un conteneur lourd de plus sur la VM.

## Conséquences
- Le support CSS d'openhtmltopdf est limité (CSS 2.1 et paged media, pas de flexbox ni de grid) :
  le template reste simple.
- Des tests de non-régression comparent le texte extrait du PDF (PDFBox), et non le rendu en pixels.
