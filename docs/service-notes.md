# Atelier 4 : Couche Service, IoC et Injection de Dépendances

## Section A. Modes d'injection comparés

| Critère | Injection par Constructeur (`@RequiredArgsConstructor`) | Injection par Attribut (`@Autowired` sur champ) |
| :--- | :--- | :--- |
| **Champ `final` possible ?** | **Oui** (garantit l'immuabilité et l'intégrité de la dépendance) | **Non** (le champ ne peut pas être `final`) |
| **Dépendance visible dans la signature ?** | **Oui** (explicite dans le constructeur) | **Non** (dépendance cachée à l'extérieur) |
| **Utilisable avec `new` hors conteneur ?** | **Oui** (facilite grandement les tests unitaires avec des mocks) | **Non** (le champ reste `null`, causant des `NullPointerException`) |
| **Signalé par SonarQube for IDE ?** | **Non** (recommandé et conforme aux bonnes pratiques) | **Oui** (règle `java:S6813` : *Field injection is not recommended*) |

---

## Section B. Couche Service et Dépendances Injectées

| Service | Dépendances injectées | Mode | Justification |
| :--- | :--- | :--- | :--- |
| **AgenceServiceImpl** | `IAgenceRepository` | Constructeur | Assure l'accès aux données des agences ; champ final et non-null garanti. |
| **ClientServiceImpl** | `IClientRepository` | Constructeur | Dépendance obligatoire pour les opérations métier et de validation des clients. |
| **ContratServiceImpl** | `IContratRepository` | Constructeur | Permet la persistance et la gestion en cascade des paiements via le contexte JPA. |
| **EmployeServiceImpl** | `IEmployeRepository` | Constructeur | Fournit les opérations de gestion des employés sans état mutable dans le service. |
| **EquipementServiceImpl**| `IEquipementRepository` | Constructeur | Injection obligatoire pour la manipulation des équipements associés aux véhicules. |
| **MaintenanceServiceImpl**| `IMaintenanceRepository`| Constructeur | Gestion du cycle de vie des maintenances et validation des dates de début/fin. |
| **PaiementServiceImpl** | `IPaiementRepository` | Constructeur | Service de consultation en lecture seule (les écritures passent par `Contrat`). |
| **ReservationServiceImpl**| `IReservationRepository`| Constructeur | Assure l'accès aux réservations et la vérification de la cohérence des dates. |
| **VehiculeServiceImpl** | `IVehiculeRepository` | Constructeur | Gestion du parc automobile et vérification des valeurs monétaires (tarif positif). |

---

## Section C. Diagnostic des Messages d'Erreur du Conteneur

### Message 1
- **Exception** : `NoSuchBeanDefinitionException`
- **Bean concerné** : `IContratService` attendu par `ContratController`.
- **Causes fréquentes** :
  1. Oubli de l'annotation `@Service` sur la classe d'implémentation `ContratServiceImpl`.
  2. Classe `ContratServiceImpl` placée en dehors du package scanné par `@SpringBootApplication` (hors de `tn.esprit.autoloc`).
- **Correction** : Ajouter `@Service` sur `ContratServiceImpl` et s'assurer que la classe se trouve dans `tn.esprit.autoloc.service`.

### Message 2
- **Exception** : `NoUniqueBeanDefinitionException`
- **Bean concerné** : `NotificationController` recevant deux implémentations du même type (`emailNotificateur` et `smsNotificateur`).
- **Deux corrections possibles** :
  1. Utiliser `@Primary` sur la classe prioritaire (ex. `@Component @Primary public class EmailNotificateur ...`).
  2. Utiliser `@Qualifier("emailNotificateur")` au niveau du paramètre du constructeur du contrôleur.

### Message 3
- **Exception** : Dépendance circulaire (`BeanCurrentlyInCreationException`).
- **Pourquoi le constructeur rend le cycle impossible à contourner ?**  
  Pour créer le bean A, Spring doit d'abord instancier ses dépendances (le bean B). Mais pour instancier le bean B, il doit déjà avoir le bean A. Avec des constructeurs, aucun objet ne peut être créé à moitié.
- **Piste de conception** : Extraire la logique ou la responsabilité partagée dans un troisième service tiers sans cycle, ou utiliser des événements applicatifs Spring (`ApplicationEventPublisher`).

---

## Section D. Anomalies SonarQube for IDE relevées et corrigées

| Règle SonarQube | Intitulé / Explication | Correction apportée |
| :--- | :--- | :--- |
| **java:S6813** | *Field dependency injection should be avoided* : l'injection par champ (`@Autowired`) masque les dépendances et empêche l'instanciation sans conteneur. | Remplacement par l'injection par constructeur avec `@RequiredArgsConstructor` et champs `private final`. |
| **java:S112** | *Generic exceptions should never be thrown* : lever `RuntimeException` ou `Exception` ne donne aucune sémantique métier. | Création et utilisation de la classe d'exception métier `ResourceNotFoundException`. |
| **java:S2160** | *Subclasses that add fields should override equals* / Risque de récursion infinie avec `@Data`. | Remplacement de `@Data` par `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor` sur toutes les entités du domaine. |

---

## Section E. Questions de Compréhension

1. **Où est le `new` de `ContratServiceImpl` et qui l'exécute ?**  
   Il n'y a aucun `new` explicite dans le code applicatif. C'est le conteneur Spring IoC (`ApplicationContext`) qui instancie la classe par réflexion au démarrage lors de la phase de création des beans.
2. **Pourquoi un contrôleur dépend-il de `IContratService` et non de `ContratServiceImpl` ?**  
   Pour respecter le principe d'inversion des dépendances (SOLID) et découpler la couche web de l'implémentation métier. Cela permet de remplacer ou enrichir l'implémentation (ex. ajout de cache, proxy transactionnel, mock pour tests) sans modifier le contrôleur.
3. **Pourquoi un service singleton doit-il rester sans état ? Donner un exemple de bug.**  
   Un bean singleton est partagé par tous les threads et requêtes concurrentes. S'il stockait un état mutable (ex: `private Contrat contratCourant;`), une requête utilisateur écraserait les données d'un autre utilisateur en cours, provoquant des corruptions de données majeures (*race conditions*).
4. **Pourquoi `update` charge-t-il l'entité existante avant d'appeler `save` au lieu de lui passer directement l'objet reçu ?**  
   Pour éviter d'écraser des champs non modifiés (ou non transmis par le client) et protéger les associations JPA ainsi que les collections orphelines (`orphanRemoval = true`), évitant la suppression accidentelle des relations (comme les paiements d'un contrat).
5. **Différence entre `@Component` et `@Bean` ? Entre `@Primary` et `@Qualifier` ? Pourquoi `IPaiementService` n'expose-t-il ni création ni suppression ?**  
   - `@Component` annote une classe pour qu'elle soit détectée automatiquement par le scan ; `@Bean` annote une méthode d'une classe `@Configuration` pour enregistrer un bean manuellement.
   - `@Primary` définit le bean par défaut lorsqu'il y a ambiguïté ; `@Qualifier` désigne explicitement par son nom le bean à injecter.
   - `IPaiementService` n'expose ni création ni suppression car `Paiement` est lié à `Contrat` par une relation de composition (`cascade = CascadeType.ALL, orphanRemoval = true`). Le cycle de vie d'un paiement dépend exclusivement de son contrat.
