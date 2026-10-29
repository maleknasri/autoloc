# Atelier 3 : Couche Repository Spring Data JPA

## 1. Choix des interfaces Spring Data JPA

Pour l'ensemble des 9 entités du projet AutoLoc, le choix retenu est l'interface **`JpaRepository<Entite, Long>`**.

| Interface | Étend | Justification |
| :--- | :--- | :--- |
| **IAgenceRepository** | `JpaRepository<Agence, Long>` | Fournit les méthodes CRUD complètes, `findAll()` retourne une `List<Agence>`, support de la pagination, tri et méthodes spécifiques JPA. |
| **IClientRepository** | `JpaRepository<Client, Long>` | Permet la gestion complète des clients avec retour de listes prêtes à l'emploi et méthodes de persistance immédiate (`saveAndFlush`). |
| **IContratRepository** | `JpaRepository<Contrat, Long>` | CRUD complet, gestion en cascade des paiements associés (composition), suppression sécurisée via le contexte de persistance. |
| **IEmployeRepository** | `JpaRepository<Employe, Long>` | CRUD complet, pagination et tri par nom/rôle sans code DAO manuel. |
| **IEquipementRepository** | `JpaRepository<Equipement, Long>` | Gestion des équipements, navigation dans les relations ManyToMany avec Vehicule. |
| **IMaintenanceRepository** | `JpaRepository<Maintenance, Long>` | Suivi des opérations de maintenance avec tri chronologique par date et filtrage par véhicule. |
| **IPaiementRepository** | `JpaRepository<Paiement, Long>` | Lecture et consultation des paiements. Bien que lié en composition au contrat, le repository dédié permet des requêtes directes. |
| **IReservationRepository** | `JpaRepository<Reservation, Long>` | Gestion des réservations, vérification d'existence (`existsById`), requêtes paginées. |
| **IVehiculeRepository** | `JpaRepository<Vehicule, Long>` | Opérations CRUD complètes sur les véhicules, pagination par catégorie ou statut, suppression sécurisée. |

---

## 2. Anomalies SonarQube for IDE (SonarLint) relevées et corrigées

| Anomalie SonarQube for IDE | Règle / Explication | Correction apportée |
| :--- | :--- | :--- |
| **Usage de `@Data` sur les entités JPA** | `java:S2160` / Règle anti-pattern : `@Data` génère automatiquement `equals`, `hashCode` et `toString` sur tous les champs, risquant des boucles infinies (`StackOverflowError`) sur les relations bidirectionnelles. | Remplacé par les annotations Lombok ciblées : `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`. |
| **Injection par champ (`@Autowired`)** | `java:S6813` : L'injection par champ rend les tests unitaires plus difficiles et cache les dépendances obligatoires. | Remplacé par l'injection de dépendances par constructeur en utilisant `@RequiredArgsConstructor` de Lombok et des champs `private final`. |
| **Retour `Iterable` vs `List`** | `CrudRepository` retourne des `Iterable<T>`, obligeant à des conversions manuelles verbeuses vers des collections. | Utilisation de `JpaRepository<T, ID>` (qui hérite de `ListCrudRepository`) pour obtenir directement des `List<T>`. |
| **Gestion des `Optional` et risque de `NullPointerException`** | `java:S2789` : Éviter l'accès direct sans vérification à la valeur renvoyée par `findById`. | Utilisation de `findById(id).orElse(null)` ou gestion d'exceptions explicites dans la couche Service. |
