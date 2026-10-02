# ADR-0004 : génération hybride (moteur de règles déterministe et IA locale)

- Statut : accepté
- Date : 2026-09-29

## Contexte
Un LLM local de petite taille est créatif, mais il ne sait pas équilibrer les rencontres, ne
respecte pas toujours une structure et reste lent sur CPU. À l'inverse, des tables aléatoires
sont fiables et testables, mais pauvres sur le plan narratif.

## Décision
Le travail est divisé en deux couches.

**1. Moteur de règles (`rules`, Java pur)** : il produit un **squelette** déterministe.
- Nombre de scènes selon la durée (120 min → 3-4, 180 min → 4-6, 240 min → 5-7), et types de
  scènes selon la répartition combat / exploration / roleplay.
- Rencontres :
  - `TYPE_5E` : budget d'XP par personnage selon le niveau, avec des tables de difficulté
    externalisées dans `rules/data/*.json`, de source SRD CC-BY-4.0 (attribution dans
    l'application et dans le PDF) ;
  - `ALLEGE` : difficulté qualitative ;
  - `NARRATIF` : aucune statistique.
- Ajustement selon l'expérience des joueurs : les débutants abaissent la difficulté d'un cran
  (RM9).
- Tirages dans les tables personnelles de l'utilisateur et sélection de ses monstres et PNJ.
- Générateur pseudo-aléatoire : `RandomGeneratorFactory.of("L64X128MixRandom")` initialisé avec
  la **graine**. L'algorithme est nommé explicitement pour garantir la reproductibilité entre
  versions du JDK. La constante `ENGINE_VERSION` est enregistrée avec chaque version du plan.

**2. IA (`ai`, Spring AI avec Ollama)** : elle **habille** le squelette.
- Génération **découpée en plusieurs appels** : d'abord le cadre (titre, pitch, synopsis,
  accroches, PNJ, conclusion), puis un appel par scène (scène, rencontres, butin et aides de jeu
  associés). Cela réduit la taille des schémas, fiabilise la sortie JSON d'un petit modèle et
  permet d'afficher une progression réelle.
- **Sortie structurée** : le schéma JSON est transmis à Ollama (`format`), la réponse est
  validée contre `plan.schema.json` et relancée au maximum 2 fois, avec les erreurs de
  validation renvoyées au modèle.
- **Contrôle de cohérence** en Java : identifiants uniques, références valides, respect du
  squelette (nombre de scènes, difficultés, `xpBudget` imposé), langue.
- **Modifications partielles** (chat, commentaires) : l'IA renvoie un `PlanPatch` qui ne
  contient que les éléments modifiés ou ajoutés et les suppressions, jamais le plan complet.
  Le `LockGuard` rejette toute modification d'un élément verrouillé (RM3). Le champ `reply`
  est placé en premier et diffusé au fil de l'eau grâce au parser JSON non bloquant de Jackson.
- **Prompts** : modèles versionnés dans `resources/prompts/{fr,en}/`, instructions système
  interdisant les contenus et noms sous droits. Le texte de l'utilisateur est placé dans un bloc
  délimité, présenté comme une donnée et non comme une instruction. Le modèle n'a accès à aucun
  outil (pas de function calling).
- Le **port** `NarrativeGenerator` isole Spring AI. Les tests utilisent un faux (fake)
  déterministe.

## Alternatives écartées
- **Tout confier à l'IA** : résultats non testables, équilibrage aléatoire, JSON géant souvent invalide.
- **Tout par tables** : narration pauvre, pas de chat possible.
- **Régénérer le plan complet à chaque modification** : trop lent sur CPU (plusieurs milliers de tokens).

## Conséquences
- Le moteur de règles est testable à 100 %, y compris par des tests de mutation.
- La graine ne garantit que la reproductibilité du squelette. Côté IA, `seed` et
  `temperature` sont transmis à Ollama, mais la reproductibilité n'est qu'au mieux.
- La durée de génération dépend du matériel. Elle est mesurée dès l'étape 7 : sur CPU, l'objectif
  est de 10 min maximum pour une génération complète et de 3 min pour une modification.
