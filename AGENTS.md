# Guide d'ajout de features (backend)

## Projet
Backend Spring Boot 4.1.1 en Java 21, sous `com.ambdulghaffar.springstarterapi`. PostgreSQL est la base principale (H2 pour les tests) et Flyway gère le schéma. Outils structurants : Spring Data JPA, Lombok, MapStruct 1.6.3, Jakarta Validation, springdoc OpenAPI. L'authentification se fait par session, JWT ou Keycloak selon le profil actif. Feature de référence : `users`.

## Quand je demande une nouvelle feature
1. Si la demande n'indique pas les champs et types, contraintes (dont les colonnes uniques), relations et rôles autorisés (lecture et écriture), les demander.
2. Lister les fichiers à créer ou modifier avec leurs chemins exacts, et attendre mon accord.
3. Après accord, créer dans l'ordre ci-dessous, puis compiler et lancer les tests.
4. Terminer par un résumé du contrat API : routes, paramètres, statuts, payloads, permissions. Ce résumé sert d'entrée à la génération du front.
5. Ne jamais lire ni recopier de secret depuis `.env` ou `application*.yml`.

## Ordre et fichiers de la feature
Pour `car`, remplacer `User` par `Car` et `users` par le nom SQL choisi. Optionnel = seulement si la feature en a besoin.

1. `src/main/resources/db/migration/V<n>__<description>.sql` — nouvelle migration Flyway, avec le prochain numéro libre. Ne jamais modifier `V1__init_schema.sql` (users y est défini, avec contraintes, index et relations).
2. `.../entity/Car.java` — nommer l'entité **sans suffixe**, comme `User`. `@Entity`, `@Table`, id auto-incrémenté, enums en `EnumType.STRING`, contraintes, audit, soft delete (`deletedAt`, `@SQLDelete`).
3. `.../repository/CarRepository.java` — `JpaRepository` + `JpaSpecificationExecutor`, existence, soft delete, `hardDelete` natif pour la purge.
4. `.../specification/CarSpecification.java` — **obligatoire** dès que la liste a `search` ou des filtres (presque toujours) : filtres, recherche, exclusion des supprimés.
5. `.../dto/CarDto.java` — records `Request`, `Response`, `Summary`, `Filter`. `PaginationDto` et `PageResponse` sont partagés, ne pas les recréer.
6. `.../mapper/CarMapper.java` — MapStruct `componentModel = "spring"`, mise à jour partielle (valeurs non nulles conservées).
7. `.../services/CarService.java` puis `CarServiceImpl.java` — contrat, transactions, mapping, exceptions métier.
8. `.../controller/CarController.java` — routes `/api/v1/cars`, validation, pagination, statuts HTTP, et **protection explicite des rôles** (voir « Sécurité »).
9. Tests sous `src/test/java/com/ambdulghaffar/springstarterapi/` — ajouter au minimum un test Mockito du service, comme `src/test/java/com/ambdulghaffar/springstarterapi/services/UserServiceImplTest.java`.
10. `data` (`DataInitializer`, `AdminInitializer`) — optionnel : seed ou bootstrap de la feature uniquement.
11. `cronJob` (`EmailTokenCleanup`) — optionnel : seulement si la feature a des données à expirer ou purger.

## Conventions observées
- **Entité** : Lombok (`@Data`, `@Builder`, constructeurs), JPA, relations en `LAZY`. `User.avatar` est un `@ManyToOne` vers `Media` en `@JsonIgnore`.
- **Audit** : `@CreatedDate` non modifiable et `@LastModifiedDate`, en `LocalDateTime` ; l'entité porte `deletedAt`.
- **Validation** : Jakarta Validation ; création avec le groupe `OnCreate`, mise à jour partielle avec `OnUpdate` ; `@NotBlank`, `@Email`, `@Size`, `@Pattern`, `@URL`.
- **Mapping** : ne jamais exposer l'entité. Le mapper convertit vers `Response`/`Summary`, ignore `id` (et les relations) à la création, et conserve les valeurs non nulles à la mise à jour.
- **Service** : `@Service`, `@RequiredArgsConstructor`, logs SLF4J ; lecture en `@Transactional(readOnly = true)`, écriture en `@Transactional`. Erreurs via `ResponseStatusException` : 404 absent, 410 déjà supprimé, 409 purge interdite.
- **Pagination** : paramètres `page`, `size`, `sort` ; entrée publique **indexée à 1**, défauts `page=1`, `size=5`, `sort=createdAt,desc` ; Spring convertit en index 0. `sort` s'écrit `propriété,direction`.
- **Réponse paginée** : `PageResponse` contient exactement `content`, `page`, `size`, `totalElements`, `totalPages` ; `page` retournée est indexée à 1.
- **Filtres** : `search` en minuscules, sur les champs texte pertinents de la feature (users : `firstName`, `lastName`, `email`). Par défaut les supprimés sont exclus ; `status=DELETED` les sélectionne.
- **Soft delete** : `DELETE /{id}` appelle `repository.delete` ; `@SQLDelete` met à jour `deleted_at` et `status = DELETED`. La purge est un `DELETE` SQL natif, après vérification et nettoyage des données liées.
- **Colonnes uniques** : le `@SQLDelete` de users préfixe l'email et le téléphone parce que ces colonnes sont uniques. C'est **spécifique à users** : ne pas le copier si la feature n'a pas de colonne unique. Sinon, utiliser un index unique partiel `WHERE deleted_at IS NULL` ou le même préfixage.
- **Controller** : `@RestController`, `@RequestMapping`, `@Validated`, `@ModelAttribute` pour les filtres ; GET et PATCH renvoient 200, POST 201, DELETE 204.
- **OpenAPI** : `OpenApiConfig` déclare un bearer JWT ; annoter les opérations avec `@Tag` et `@Operation`.

## Sécurité
- `SecurityConfig` active `@EnableMethodSecurity`. Les chaînes de sécurité sont sous profils `session`, `jwt`, `keycloak`. `UserDetailsServiceImpl` produit `ROLE_` + enum. Users est réservé à `ROLE_ADMIN` par une règle de `SecurityConfig`.
- Toute route non listée dans `COMMON_RULES` tombe sur `.anyRequest().authenticated()` : `/api/v1/<xxx>` exige donc un utilisateur authentifié, et n'est ni publique ni refusée à tous. Une route users est plus restrictive via `.requestMatchers("/api/v1/users/**").hasRole("ADMIN")`.
- **Une route ne doit jamais dépendre d'un comportement par défaut non vérifié.** Pour chaque nouvelle feature, annoter explicitement le controller ou chaque méthode avec `@PreAuthorize` (par exemple `hasRole('ADMIN')` pour l'écriture, `isAuthenticated()` pour la lecture), conformément aux rôles que j'ai donnés.
- Ne modifier `SecurityConfig` que si le comportement par défaut des routes sans règle l'impose, et seulement après mon accord.

## Règles
- Ne jamais retourner `User` (ou une autre entité) directement depuis un controller, ni mettre de logique métier ou d'accès repository dans le controller.
- Ne jamais contourner DTO, mapper, service, validation ou transactions.
- Ne pas utiliser `cascade = REMOVE/ALL` ni `orphanRemoval = true` sur les relations d'une entité soft-deletée.
- Pas de suppression physique pour le `DELETE` standard ; la purge doit être explicite et contrôlée.
- Ne pas modifier JWT, Keycloak, CORS ou les profils pour une feature ordinaire.
- Ne pas exposer `password`, tokens, secrets ou valeurs de configuration sensibles.
- Une feature ordinaire ne dépend d'aucun profil Spring.

## Exemple minimal (users)
```java
@Entity @Table(name = "users")
@SQLDelete(sql = "UPDATE users SET deleted_at = NOW(), status = 'DELETED' WHERE id = ?")
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
}

@Transactional(readOnly = true)
public UserDto.Response getById(Long id) {
    return userMapper.toDto(userRepository.findById(id).orElseThrow(
        () -> new ResponseStatusException(HttpStatus.NOT_FOUND)));
}

@GetMapping("/{id}")
public ResponseEntity<UserDto.Response> getUser(@PathVariable Long id) {
    return ResponseEntity.ok(userService.getById(id));
}
```

## Contrat pour le front
Le front consomme les features sans adaptation si le backend suit ces conventions.
- Routes `/api/v1/<feature>` : `GET` liste, `GET /{id}`, `POST`, `PATCH /{id}`, `DELETE /{id}` (soft delete), `DELETE /{id}/purge` (purge irréversible, optionnelle).
- La liste accepte `page` (1-based, défaut 1), `size` (défaut 5), `sort` (`propriété,direction`), `search` et les filtres propres à la feature.
- Réponse : `content`, `page`, `size`, `totalElements`, `totalPages`.
- Statuts : 200, 201 et 204 comme pour users. Sans `@ControllerAdvice` ni configuration `server.error.*` trouvée, une `ResponseStatusException` passe par le gestionnaire d'erreur Spring Boot par défaut. Le message n'est pas inclus par défaut (`server.error.include-message=never`) : `{"timestamp":"2026-10-23T13:00:00.000+00:00","status":404,"error":"Not Found","path":"/api/v1/users/999"}`. Le timestamp réel varie ; les champs sont `timestamp`, `status`, `error`, `path`.
- Les rôles autorisés sont toujours explicites (voir « Sécurité »).

## Vérifications de référence
- **Tests** : `src/test/java/com/ambdulghaffar/springstarterapi/services/UserServiceImplTest.java` est le test de référence Mockito pour le service users ; il vérifie le mapping d'un utilisateur trouvé et le 404 (`ResponseStatusException`) quand l'id est absent. Le test de contexte existant est `SpringStarterApiApplicationTests.contextLoads`.