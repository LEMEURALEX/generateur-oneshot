# ADR-0010 : architecture en couches techniques globales

- Statut : accepté
- Date : 2026-01-03
- Remplace : ADR-0001 / 0001-monolithe-modulaire-monorepo.md

## Contexte
Le projet est un projet d'apprentissage avec un choix de conception en couches techniques : controller, model, mapper, repository, service, implémentation et dto.
La qualité de conception passera par les choix fait sur les responsabilités, les contrats, les tests et la maîtrise des dépendances.

## Décision

Le backend reste un monolithe dans un monorepo, les couches deviennent les packages de premier niveau sous `com?oneshot`, les packages métier `rules`, `generation`, etc.
ne sont plus les racines de l'organisation du code.

```
com.onshote
├── controller/        entrées HTTP, délégation aux services
├── dto/               contrats de données d'entrée et de sortie
├── mapper/            conversions entre contrats et modèles
├── model/             objets-valeurs métier et modèles de persistance distincts
├── service/           interface des opérations applicatives et du moteur
├── implementation/    implémentations des services et politiques de calcul
└── repository/        accès aux données persistées
```

Les classes existantes seront déplacées progressivement. Aucun package vide, controller ou repository artificiel n'est nécessaire pour un calcul en mémoire.
Si le volume le justifie, une couche pourra être subdivisée par fonctionnalité, ce n'est pas requis pour la migration initiale.

### Responsabilités et dépendances

- Les controllers valident les entrées HTTP et délèguent aux interfaces de service ;
ils ne calculent pas les règles métier et n'accèdent pas directement aux repositories.
- Les interfaces de service définissent les opérations publiques. Leurs implémentations
orchestrent les collaborateurs. Les calculs peuvent rester dans des politiques
concrètes internes : une interface n'est pas créée systématiquement pour chaque classe.
- Les modèles métier conservent leurs invariants. `Repartition` et `DureeSession`
restent des objets-valeurs validés à la construction, pas des DTO sans comportement.
- Les mappers assurent les conversions sans accéder à la base ni effectuer de tirage.
Un mapper manuel suffit tant qu'une bibliothèque ne simplifie pas réellement le code.
- Les repositories sont réservés à la persistance ; ils ne sont pas utilisés par le moteur.
- Les entités JPA ne sont jamais exposées dans une réponse HTTP.

### Contrats et moteur de règles

OpenAPI et le schéma JSON restent les sources de vérité. Les DTO HTTP générés ne sont
pas redéfinis manuellement dans une version concurrente. Leur package de génération
sera configuré séparément ; le code généré n'est pas édité à la main.

Une interface publique du moteur et ses DTO Java d'entrée/sortie seront définis dans
`service` et `dto`. Les conversions préserveront une séparation avec les DTO HTTP
générés. Le moteur et ses contrats restent sans dépendance à Spring, JPA, aux repositories,
aux DTO OpenAPI ou aux implémentations d'autres fonctionnalités.

L'indépendance doit être vérifiée par ArchUnit sur les types du moteur et leurs
dépendances transitives internes. La présence d'autres classes Spring dans
`implementation` ou JPA dans `model` ne rend pas ces dépendances autorisées pour le moteur.
Les classes et contrats concernés seront explicitement identifiés pendant la migration.

MVC concerne la couche web, avec React pour l'affichage. SOLID guide les responsabilités
et les dépendances ; l'injection par constructeur ne garantit pas à elle seule le
principe d'inversion des dépendances.

## Migration prévue

La présente décision modifie uniquement la documentation. Elle ne prouve pas que
les packages ci-dessus, les contrats ou les contrôles ArchUnit existent déjà dans le code.

1. Faire un point de sauvegarde Git et exécuter les tests actuels du moteur.
2. Déplacer les modèles et les politiques sans changer leur comportement, puis adapter
   les packages, imports et chemins des tests. Vérifier immédiatement les tests déplacés.
3. Introduire l'interface publique, les DTO et le mapper du moteur, puis adapter
   le générateur et les tests. Garder la graine, les résultats et les listes non modifiables.
4. Adapter les contrôles d'architecture et la sélection Maven des tests aux nouveaux
   packages. Le changement de packages ne nécessite aucune modification du contrat HTTP.
5. Vérifier le placement de la classe Spring Boot et son périmètre de scan lors de
   l'intégration des futurs composants Spring ; ne pas annoter le moteur pour le découvrir.

| Classe actuelle sous `com.oneshot.rules.domain` | Destination prévue |
|---|---|
| `Repartition`, `DureeSession`, `ComptageScenes`, `TypeScene` | `com.oneshot.model` |
| `PolitiqueNombreScenes`, `PolitiqueRepartitionScenes`, `AssembleurScenes` | `com.oneshot.implementation` |
| `GenerateurScenes` | `com.oneshot.implementation.GenerateurScenesServiceImpl` |
| Nouveau contrat du générateur | `com.oneshot.service.GenerateurScenesService` |
| Nouveaux contrats et conversions | `com.oneshot.dto` et `com.oneshot.mapper` |

Les DTO restent à concevoir avant implémentation ; leur forme n'est pas figée par
cette décision. La logique de répartition, y compris le départage combat/exploration/
roleplay, est conservée. Les controllers et repositories seront ajoutés lorsque
les fonctionnalités HTTP et de persistance le nécessiteront.

## Conséquences

- Les couches techniques sont directement visibles et peuvent être expliquées en entretien.
- Les frontières métier ne sont plus garanties par un package racine par fonctionnalité ;
  les tests d'architecture doivent donc être plus explicites.
- Le coût de migration inclut les imports, les tests et les sélections Maven, sans
  ajout de fonctionnalités métier ni modification du schéma de base de données.
- La couverture et les objectifs de mutation du moteur restent ceux de l'ADR-0009.