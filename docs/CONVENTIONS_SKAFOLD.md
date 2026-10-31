# Guide — Ajouter une nouvelle entité (spring-boot-starter-kit)

> **But** : donner ce fichier à une IA avec une *fiche entité* remplie (section 1) pour qu'elle génère
> Entity → Migration → Repository → DTO → Mapper → Specification → Service → Controller
> en respectant les conventions du projet. On coche seulement les **modules** utiles (section 5).

---

## 0. Comment l'utiliser

1. Copier la fiche (section 1), la remplir, cocher les modules.
2. Donner à l'IA : **ce fichier + la fiche** (+ le prompt de la section 8).
3. Vérifier avec la checklist (section 7).

---

## 1. Fiche entité (à remplir)

```text
Entité (PascalCase singulier) : Product
Table (snake_case pluriel)    : products
URL (kebab pluriel)           : /api/v1/products

Champs :
- name   : String, obligatoire, max 150
- price  : BigDecimal, obligatoire, >= 0
- status : enum ProductStatus (DRAFT, PUBLISHED), obligatoire

Relations :
- owner → User (ManyToOne, obligatoire)

Modules (cocher) :
[x] BASE   (toujours)
[x] A  Entité appartenant à un utilisateur (owner) + sécurité ownership
[ ] B  Relation vers une autre entité (parent)
[x] C  Accès : GET public / écriture authentifiée / admin-only (préciser ci-dessous)
[x] D  Exclure les entités des users soft-deleted dans la Specification
[x] E  Champ enum
[ ] F  Média attaché (image/vidéo)
[x] G  Recherche texte (`search`) sur : name
[x] H  Données de seed (DataInitializer)
[ ] I  Suppression liée à la suppression d'un User

Accès :
- GET list / GET by id : public
- POST                 : authentifié
- PATCH / DELETE       : ADMIN ou owner

Filtres : search, ownerId, status
```

---

## 2. Contexte projet (conventions fixes)

- **Package racine** : `riflineapi`
- **Dossiers** (attention : `services` au pluriel) :
  `entity/`, `dto/`, `mapper/`, `repository/`, `specification/`, `services/`, `controller/`
- **Stack** : Spring Boot, Spring Data JPA (+ `JpaSpecificationExecutor`), Lombok, MapStruct (`componentModel = "spring"`), springdoc (Swagger), Jakarta Validation, Flyway, PostgreSQL.
- **Profils** : auth `session` / `jwt` / `keycloak` ; profil `local` = `ddl-auto: update` + Flyway désactivé ; sinon `ddl-auto: validate` + Flyway actif → **une migration est obligatoire** sinon l'app ne démarre pas.
- `spring.jpa.open-in-view: false` → ne jamais accéder à une relation LAZY hors d'un `@Transactional` (le mapping se fait dans le service).
- **Un fichier DTO par entité** : classe `final` + constructeur privé, avec des `record` imbriqués : `CreateRequest`, `UpdateRequest`, `Response`, (`Summary`), `Filter`.
- **Nommage**
    - Service : `get{Entities}`, `get{Entity}ById`, `create{Entity}`, `update{Entity}`, `delete{Entity}`
    - Controller : `list`, `getById`, `create`, `update`, `delete`
    - Mapper : `toDto`, `toEntity`, `updateEntity`
- **Langue** : logs en anglais ; messages d'erreur envoyés au client en **français** : `"Aucun {entity} trouvé avec l'ID: " + id`.
- **Erreurs** : `ResponseStatusException` (404 introuvable, 400 requête invalide). Pas de `@ControllerAdvice` dans le projet.
- **Update = `PATCH` partiel** (champs `null` ignorés). **Pagination** : page commence à **1**, tri par défaut `createdAt,desc` → l'entité **doit** avoir `createdAt` (sinon l'appelant doit toujours passer `sort`).
- `@EnableJpaAuditing` est déjà actif (`createdAt` / `updatedAt` automatiques).

---

## 3. Ordre de génération

1. Migration Flyway
2. Entity (+ enum si besoin)
3. Repository
4. DTO
5. Mapper
6. Specification
7. Service (interface) + ServiceImpl
8. Controller
9. Modules cochés : SecurityUtils / SecurityConfig / DataInitializer
10. Checklist (section 7)

---

## 4. Module BASE (toujours)

Remplacer `Entity` / `entity` / `entities` / `table` / `path` par les valeurs de la fiche.

### 4.1 Migration — `src/main/resources/db/migration/V{n}__create_{table}_table.sql`

`{n}` = **numéro le plus haut existant + 1** (vérifier le dossier, dernier connu : V12 → prochaine = V13). Jamais modifier une migration existante.

```sql
CREATE TABLE {table} (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(150) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
```

Règles : colonnes en `snake_case`, `created_at`/`updated_at` `TIMESTAMP NOT NULL`, FK nommées `fk_{table}_{colonne}`, unicité nommée `uk_{table}_{colonne}`.

### 4.2 Entity — `entity/{Entity}.java`

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@EntityListeners(AuditingEntityListener.class)
@Entity
@Builder
@Table(name = "{table}")
public class {Entity} {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
```

Si le nom du champ Java ≠ nom de colonne, préciser `@Column(name = "...")` (ex. `defaultAddress` → `is_default`).

### 4.3 Repository — `repository/{Entity}Repository.java`

```java
@Repository
public interface {Entity}Repository extends JpaRepository<{Entity}, Long>, JpaSpecificationExecutor<{Entity}> {
}
```

### 4.4 DTO — `dto/{Entity}Dto.java`

```java
public final class {Entity}Dto {
    private {Entity}Dto() {}

    // === REQUESTS ===
    public interface {Entity}Fields {
        @Pattern(regexp = "(?s).*\\S.*", message = "must not be blank")
        @Size(max = 150) String name();
    }

    @Schema(name = "{Entity}CreateRequest")
    public record CreateRequest(
            @NotBlank String name
    ) implements {Entity}Fields {}

    @Schema(name = "{Entity}UpdateRequest")
    public record UpdateRequest(
            String name            // tous optionnels (PATCH)
    ) implements {Entity}Fields {}

    // === RESPONSES ===
    @Schema(name = "{Entity}Response")
    public record Response(
            Long id,
            String name,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}

    @Schema(name = "{Entity}Summary")   // seulement si l'entité est embarquée dans un autre DTO
    public record Summary(Long id, String name) {}

    // === FILTER ===
    @Schema(name = "{Entity}Filter")
    public record Filter(
            String search
    ) {}
}
```

Variante avec groupes de validation (style `CommentDto`) : `@NotBlank(groups = OnCreate.class)` dans l'interface `Fields`, et `@Validated(OnCreate.class)` / `@Validated(OnUpdate.class)` dans le controller. Par défaut on garde `@Valid`.

### 4.5 Mapper — `mapper/{Entity}Mapper.java`

```java
@Mapper(componentModel = "spring")
public interface {Entity}Mapper {

    {Entity}Dto.Response toDto({Entity} entity);

    @Mapping(target = "id", ignore = true)
    {Entity} toEntity({Entity}Dto.CreateRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    void updateEntity(
            {Entity}Dto.UpdateRequest request,
            @MappingTarget {Entity} entity
    );
}
```

Règles MapStruct :
- Mêmes noms → mappé automatiquement. Noms différents → `@Mapping(target = "x", source = "y.z")`.
- Toute relation (`User`, `Media`, parent…) est **ignorée** dans `toEntity` / `updateEntity` et **résolue dans le service**.
- Objet imbriqué dans la réponse (ex. `UserDto.Summary owner`) → `uses = UserMapper.class` (comme `PostMapper`). **Ne pas copier** `uses = {User.class}` de `CommentMapper` (c'est une entité, pas un mapper).
- Toujours `NullValuePropertyMappingStrategy.IGNORE` sur `updateEntity` (sinon un PATCH écrase avec `null`).

### 4.6 Specification — `specification/{Entity}Specification.java`

```java
public final class {Entity}Specification {

    private {Entity}Specification() {}

    public static Specification<{Entity}> from({Entity}Dto.Filter filter) {
        if (filter == null) {
            return Specification.unrestricted();
        }
        return Specification.allOf(
                search(filter.search())
        );
    }

    private static Specification<{Entity}> search(String search) {
        if (search == null || search.isBlank()) {
            return Specification.unrestricted();
        }
        String like = "%" + search.toLowerCase() + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("name")), like);
    }
}
```

Règles :
- Un critère = une méthode privée statique qui renvoie `Specification.unrestricted()` si la valeur est `null`/vide.
- `root.get("...")` prend le nom du **champ Java** (pas la colonne SQL). Un nom inexistant = exception à l'exécution.
- Égalité de texte : `cb.equal(cb.lower(...), value.toLowerCase())` ; recherche partielle : `cb.like(cb.lower(...), "%...%")`.

### 4.7 Service — `services/{Entity}Service.java`

```java
public interface {Entity}Service {

    PageResponse<{Entity}Dto.Response> get{Entities}(
            {Entity}Dto.Filter filter,
            PaginationDto.Pagination pagination
    );

    {Entity}Dto.Response get{Entity}ById(Long id);

    {Entity}Dto.Response create{Entity}({Entity}Dto.CreateRequest request);

    {Entity}Dto.Response update{Entity}(Long id, {Entity}Dto.UpdateRequest request);

    void delete{Entity}(Long id);
}
```

### 4.8 ServiceImpl — `services/{Entity}ServiceImpl.java`

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class {Entity}ServiceImpl implements {Entity}Service {

    private final {Entity}Repository {entity}Repository;
    private final {Entity}Mapper {entity}Mapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<{Entity}Dto.Response> get{Entities}(
            {Entity}Dto.Filter filter,
            PaginationDto.Pagination pagination
    ) {
        log.debug("Fetching {entities} - page: {}, size: {}", pagination.page(), pagination.size());

        Page<{Entity}Dto.Response> page = {entity}Repository
                .findAll({Entity}Specification.from(filter), pagination.toPageable())
                .map({entity}Mapper::toDto);

        return PageResponse.from(page);
    }

    @Override
    @Transactional(readOnly = true)
    public {Entity}Dto.Response get{Entity}ById(Long id) {
        return {entity}Mapper.toDto(find{Entity}OrThrow(id));
    }

    @Override
    @Transactional
    public {Entity}Dto.Response create{Entity}({Entity}Dto.CreateRequest request) {
        {Entity} entity = {entity}Mapper.toEntity(request);
        // ⚠️ résoudre ici les relations (owner, parent, média…) — cf. modules A / B / F
        {Entity} saved = {entity}Repository.save(entity);
        log.info("{Entity} created with id: {}", saved.getId());
        return {entity}Mapper.toDto(saved);
    }

    @Override
    @Transactional
    public {Entity}Dto.Response update{Entity}(Long id, {Entity}Dto.UpdateRequest request) {
        {Entity} entity = find{Entity}OrThrow(id);
        {entity}Mapper.updateEntity(request, entity);
        {Entity} saved = {entity}Repository.save(entity);
        log.info("{Entity} updated with id: {}", id);
        return {entity}Mapper.toDto(saved);
    }

    @Override
    @Transactional
    public void delete{Entity}(Long id) {
        if (!{entity}Repository.existsById(id)) {
            log.warn("Delete failed - {entity} not found with id: {}", id);
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Aucun {entity} trouvé avec l'ID: " + id);
        }
        {entity}Repository.deleteById(id);
        log.info("{Entity} deleted with id: {}", id);
    }

    private {Entity} find{Entity}OrThrow(Long id) {
        return {entity}Repository.findById(id).orElseThrow(() -> {
            log.warn("{Entity} not found with id: {}", id);
            return new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Aucun {entity} trouvé avec l'ID: " + id);
        });
    }
}
```

### 4.9 Controller — `controller/{Entity}Controller.java`

```java
@Slf4j
@RestController
@RequestMapping("/api/v1/{path}")
@RequiredArgsConstructor
@Tag(name = "{Entities}", description = "Manage {entities}")
public class {Entity}Controller {

    private final {Entity}Service {entity}Service;

    @GetMapping
    @Operation(summary = "List {entities}", description = "Returns a paginated list of {entities}, optionally filtered.")
    public ResponseEntity<PageResponse<{Entity}Dto.Response>> list(
            @ModelAttribute {Entity}Dto.Filter filter,
            @ModelAttribute PaginationDto.Pagination pagination
    ) {
        log.debug("GET /api/v1/{path} - page: {}, size: {}", pagination.page(), pagination.size());
        return ResponseEntity.ok({entity}Service.get{Entities}(filter, pagination));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a {entity} by id", description = "Returns a single {entity}. Returns 404 if it does not exist.")
    public ResponseEntity<{Entity}Dto.Response> getById(@PathVariable Long id) {
        return ResponseEntity.ok({entity}Service.get{Entity}ById(id));
    }

    @PostMapping
    @Operation(summary = "Create a {entity}", description = "Creates a new {entity}.")
    public ResponseEntity<{Entity}Dto.Response> create(
            @Valid @RequestBody {Entity}Dto.CreateRequest request
    ) {
        {Entity}Dto.Response response = {entity}Service.create{Entity}(request);
        log.info("POST /api/v1/{path} - created with id: {}", response.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update a {entity}", description = "Partially updates an existing {entity}.")
    public ResponseEntity<{Entity}Dto.Response> update(
            @PathVariable Long id,
            @Valid @RequestBody {Entity}Dto.UpdateRequest request
    ) {
        return ResponseEntity.ok({entity}Service.update{Entity}(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a {entity}", description = "Deletes a {entity} by id.")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        {entity}Service.delete{Entity}(id);
        return ResponseEntity.noContent().build();
    }
}
```

Le `Filter` est lié aux query params via `@ModelAttribute` (`?search=abc&ownerId=3&page=1&size=10&sort=createdAt,desc`).

---

## 5. Modules optionnels

### A — Entité appartenant à un utilisateur (owner)

**Migration** (dans le `CREATE TABLE`) :
```sql
owner_id BIGINT NOT NULL,
CONSTRAINT fk_{table}_owner FOREIGN KEY (owner_id) REFERENCES users(id)
```

**Entity** :
```java
@JsonIgnore
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "owner_id", nullable = false)
private User owner;
```

**DTO** : `CreateRequest` + `@NotNull Long ownerId` · `Response` + `UserDto.Summary owner` · `Filter` + `Long ownerId`.

**Mapper** : `@Mapper(componentModel = "spring", uses = UserMapper.class)` ; dans `toEntity` **et** `updateEntity` : `@Mapping(target = "owner", ignore = true)`.

**Service (create)** :
```java
User owner = userRepository.findById(request.ownerId())
        .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Aucun utilisateur trouvé avec l'ID: " + request.ownerId()));
entity.setOwner(owner);
```
(injecter `UserRepository`)

**Specification** :
```java
private static Specification<{Entity}> hasOwner(Long ownerId) {
    if (ownerId == null) return Specification.unrestricted();
    return (root, query, cb) -> cb.equal(root.get("owner").get("id"), ownerId);
}
```

**Sécurité ownership** — ajouter dans `config/SecurityUtils.java` (injecter `{Entity}Repository`) :
```java
public boolean is{Entity}Owner(Long {entity}Id) {
    Long currentUserId = getCurrentUserId();
    return {entity}Repository.findById({entity}Id)
            .map(e -> e.getOwner() != null && e.getOwner().getId().equals(currentUserId))
            .orElse(false);
}
```
Puis sur `PATCH` et `DELETE` du controller :
```java
@PreAuthorize("hasRole('ADMIN') or @securityUtils.is{Entity}Owner(#id)")
```

> ⚠️ **Variante recommandée (plus sûre)** : le code actuel (`Post`, `Comment`) reçoit `authorId` dans le body, donc un utilisateur peut créer au nom d'un autre. Pour éviter ça, retirer `ownerId` du `CreateRequest` et faire dans le service : `entity.setOwner(securityUtils.getCurrentUser());`. À préciser dans la fiche si tu veux cette variante.

### B — Relation vers une autre entité (parent)

Même schéma que A, avec `{Parent}` :
- **Migration** : `{parent}_id BIGINT NOT NULL` + FK `fk_{table}_{parent}`.
- **Entity** : `@JsonIgnore @ManyToOne(fetch = LAZY) @JoinColumn(name = "{parent}_id", nullable = false) private {Parent} {parent};`
- **DTO** : `CreateRequest` + `@NotNull Long {parent}Id` ; `Response` + `Long {parent}Id` (ou `{Parent}Dto.Summary {parent}`) ; `Filter` + `Long {parent}Id`.
- **Mapper** : `@Mapping(target = "{parent}Id", source = "{parent}.id")` sur `toDto` ; `@Mapping(target = "{parent}", ignore = true)` sur `toEntity` et `updateEntity`.
- **Service** : `{parent}Repository.findById(...)` → 404 `"Aucun {parent} trouvé avec l'ID: "` → `entity.set{Parent}(...)`.
- **Repository** (évite le N+1 sur la liste, comme `CommentRepository`) :
```java
@Override
@EntityGraph(attributePaths = {"{parent}"})
@NonNull
Page<{Entity}> findAll(Specification<{Entity}> spec, @NonNull Pageable pageable);
```
- **Suppression du parent** : par défaut la FK bloque la suppression du parent s'il a des enfants. Alternative `ON DELETE SET NULL` / `CASCADE` : voir le bloc commenté « CASE 2 » dans `Comment.java` (colonne nullable + `foreignKeyDefinition`). Le préciser dans la fiche.

### C — Règles d'accès (`config/SecurityConfig.java`)

Par défaut `anyRequest().authenticated()` s'applique : **rien à faire** si tout est réservé aux connectés.

Dans `COMMON_RULES`, **avant** `.anyRequest()` :
```java
// GET public
.requestMatchers(HttpMethod.GET, "/api/v1/{path}/**").permitAll()
// OU tout réservé aux admins
.requestMatchers("/api/v1/{path}/**").hasRole("ADMIN")
```
Alternative par méthode : `@PreAuthorize("hasRole('ADMIN')")` sur le controller / la méthode.
Note : `@EnableMethodSecurity` est actif ; `GET` public = pas de `@PreAuthorize` sur les GET.

### D — Exclure les entités des utilisateurs supprimés (soft delete)

À mettre dans `from(...)` (et dans le cas `filter == null`, renvoyer `excludeSoftDeleteOwner()` au lieu de `unrestricted()`) :
```java
private static Specification<{Entity}> excludeSoftDeleteOwner() {
    return (root, query, cb) -> cb.and(
            cb.isNull(root.get("owner").get("deletedAt")),
            cb.notEqual(root.get("owner").get("status"), UserStatus.DELETED)
    );
}
```
(nécessite le module A.)

### E — Champ enum

- `entity/{Entity}Status.java` : `public enum {Entity}Status { DRAFT, PUBLISHED }`
- Entity : `@Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private {Entity}Status status;`
- Migration : `status VARCHAR(30) NOT NULL` (+ optionnel `CONSTRAINT {table}_status_check CHECK (status IN ('DRAFT','PUBLISHED'))` comme `users_status_check` en V12).
- DTO : utiliser le **type enum** (pas `String`) dans `CreateRequest` / `UpdateRequest` / `Response` / `Filter` (Spring le convertit depuis le query param).
- Specification : `cb.equal(root.get("status"), status)`.
- **Modifier une liste de valeurs plus tard = nouvelle migration** (cf. V12).

### F — Média attaché (image / vidéo)

Pattern de `Post` :
- **Migration** : `image_id BIGINT` + `CONSTRAINT fk_{table}_image FOREIGN KEY (image_id) REFERENCES media(id)` (+ index).
- **Entity** : `@ManyToOne(fetch = LAZY) @JoinColumn(name = "image_id") private Media image;`
- **DTO** : `CreateRequest` / `UpdateRequest` + `Long imageId` ; `UpdateRequest` + `Boolean removeImage` ; `Response` + `String imageUrl`.
- **Mapper** : `@Mapping(target = "imageUrl", source = "image.url")` ; `@Mapping(target = "image", ignore = true)` sur `toEntity` et `updateEntity`.
- **Service** (injecter `MediaService`) :
```java
// create
entity.setImage(request.imageId() == null ? null
        : mediaService.requiredOwnerOrAdmin(request.imageId(), ownerId, MediaType.IMAGE));

// update
if (Boolean.TRUE.equals(request.removeImage())) {
    entity.setImage(null);
} else if (request.imageId() != null) {
    entity.setImage(mediaService.requiredOwnerOrAdmin(request.imageId(), ownerId, MediaType.IMAGE));
}
```
Si besoin d'un nouveau contexte de média, regarder `MediaContext` / `MediaService` avant d'inventer quoi que ce soit.

### G — Recherche texte

Dans la Specification, `search` sur **plusieurs champs** (noms Java réels uniquement) :
```java
return (root, query, cb) -> cb.or(
        cb.like(cb.lower(root.get("name")), like),
        cb.like(cb.lower(root.get("description")), like)
);
```

### H — Données de seed (`data/DataInitializer.java`)

Suivre le schéma existant : une méthode `create{Entities}(...)` qui construit une liste avec une méthode helper basée sur le `builder()`, puis `{entity}Repository.saveAll(...)`, appelée avec les autres `createXxx(...)` (ex. `createAddresses(users)`). Injecter le repository. Respecter l'ordre : d'abord les entités dont dépendent les FK (users, parents).
```java
private {Entity} {entity}(User owner, String name) {
    return {Entity}.builder().owner(owner).name(name).build();
}
```

### I — Suppression liée à un User

Si l'entité référence `User`, regarder comment `UserServiceImpl` supprime déjà `Address` / `Comment` (`deleteByUserId`, `deleteByAuthorId` dans les repositories) et suivre **exactement** le même mécanisme :
```java
@Modifying(clearAutomatically = true, flushAutomatically = true)
@Query(value = "DELETE FROM {table} WHERE owner_id = :userId", nativeQuery = true)
void deleteByOwnerId(@Param("userId") Long userId);
```
puis l'appeler au bon endroit dans `UserServiceImpl`.

---

## 6. Règles pour l'IA

- Ne **jamais** modifier du code existant sauf ce que demande un module coché (`SecurityUtils`, `SecurityConfig`, `DataInitializer`, `UserServiceImpl`). Dans ce cas, donner uniquement le diff.
- Ne pas ajouter de dépendance (`pom.xml`/`build.gradle`) ni de librairie non utilisée dans le projet.
- Pas de relation bidirectionnelle (`@OneToMany`) sauf demande explicite dans la fiche.
- Toujours `FetchType.LAZY` sur les `@ManyToOne`, avec `@JsonIgnore` (les réponses passent par les DTO, jamais par l'entité).
- Ne jamais renvoyer l'entité JPA depuis un controller : toujours un DTO.
- Pas de logique métier dans le controller ni dans le mapper.
- Si la fiche est ambiguë (type, nullabilité, règle d'accès, comportement à la suppression), **poser la question** avant de coder.
- Livrer **un fichier par bloc de code**, avec son chemin complet.

---

## 7. Checklist de fin

- [ ] Migration `V{n}` créée, numéro = dernier + 1, SQL cohérent avec l'entité (noms de colonnes, `NOT NULL`, longueurs, types)
- [ ] L'app démarre en profil non-local (`ddl-auto: validate` ne se plaint pas)
- [ ] Tous les `root.get("...")` de la Specification correspondent à des champs Java existants
- [ ] Relations ignorées dans `toEntity`/`updateEntity` et résolues dans le service
- [ ] `updateEntity` avec `NullValuePropertyMappingStrategy.IGNORE`
- [ ] `@PreAuthorize` / règles `SecurityConfig` conformes à la fiche
- [ ] Swagger : l'entité apparaît, les schémas `{Entity}CreateRequest` etc. sont uniques
- [ ] `GET /api/v1/{path}?page=1&size=5` → 200 ; `POST` invalide → 400 ; `GET` id inconnu → 404
- [ ] Seed OK (si module H)

---

## 8. Prompt prêt à copier

```text
Tu es un développeur Spring Boot senior. Je te donne le guide ENTITY_SCAFFOLD_GUIDE.md
de mon projet spring-boot-starter-kit et une fiche entité.

Génère TOUT le code de l'entité décrite dans la fiche en suivant exactement :
- les conventions de la section 2,
- le module BASE (section 4),
- uniquement les modules cochés dans la fiche (section 5),
- les règles de la section 6.

Format de réponse : un bloc de code par fichier, avec le chemin complet en titre.
Pour les fichiers existants à modifier (SecurityUtils, SecurityConfig, DataInitializer…),
donne uniquement les ajouts. Termine par la checklist de la section 7.
Si une information manque dans la fiche, pose-moi la question avant de coder.

--- FICHE ---
(coller la fiche ici)
```