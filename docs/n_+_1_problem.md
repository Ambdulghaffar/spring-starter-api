# Le Problème N+1 en JPA/Hibernate

## C'est quoi le N+1 ?

**N+1** = 1 requête pour les parents + N requêtes pour chaque enfant

C'est le **killer de performance #1** en JPA/Hibernate.

---

## Exemple concret

Tu as **100 posts** dans ta base de données.

### Le problème N+1

```java
List<Post> posts = postRepository.findAll();  // 1 requête

for (Post post : posts) {
    String authorName = post.getAuthor().getFirstName();  // N requêtes !
}
```

**Ce qui se passe en SQL :**

```sql
-- Requête 1 (le "1" de N+1)
SELECT * FROM posts;

-- Requête 2 (commence le "N")
SELECT * FROM users WHERE id = 1;

-- Requête 3
SELECT * FROM users WHERE id = 2;

-- Requête 4
SELECT * FROM users WHERE id = 3;

-- ... 97 autres requêtes ...

-- Requête 101
SELECT * FROM users WHERE id = 100;
```

**Total : 101 requêtes** pour afficher 100 posts.

---

## Pourquoi ça s'appelle "N+1" ?

| Terme | Signification |
|-------|---------------|
| **1** | La première requête (les posts) |
| **N** | Une requête par item (les auteurs) |
| **N+1** | Total = 1 + N requêtes |

Si tu as 1000 posts → **1001 requêtes** au lieu de **1**.

---

## Visualisation

```
N+1 Problem:
┌─────────────┐     ┌─────────────┐
│  1 requête  │     │ 100 requêtes│
│  GET posts  │ ──► │ GET user 1  │  = 101 requêtes
└─────────────┘     │ GET user 2  │
                    │ GET user 3  │
                    │    ...      │
                    └─────────────┘

JOIN FETCH (solution):
┌─────────────────────────────┐
│  1 requête                  │
│  GET posts JOIN users       │  = 1 requête
└─────────────────────────────┘
```

---

## La solution : JOIN FETCH

### Repository avec JOIN FETCH

```java
public interface PostRepository extends JpaRepository<Post, Long> {

    // Pour liste - 1 seule requête SQL
    @Query("SELECT p FROM Post p JOIN FETCH p.author")
    List<Post> findAllWithAuthor();

    // Pour détail avec commentaires - 1 seule requête SQL
    @Query("""
        SELECT p FROM Post p
        JOIN FETCH p.author
        LEFT JOIN FETCH p.comments c
        LEFT JOIN FETCH c.author
        WHERE p.id = :id
        """)
    Optional<Post> findByIdWithDetails(@Param("id") Long id);
}
```

### Utilisation dans le Service

```java
@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;

    public List<Post> getAllPosts() {
        return postRepository.findAllWithAuthor();  // 1 seule requête !
    }
}
```

### Résultat SQL

```sql
-- UNE SEULE requête avec JOIN
SELECT p.*, u.*
FROM posts p
INNER JOIN users u ON p.author_id = u.id;
```

**Hibernate reçoit tout en 1 requête** et hydrate les objets `Post` avec leur `Author` déjà chargé.

---

## Comment détecter le N+1 ?

### Activer les logs SQL dans `application.yml`

```yaml
spring:
  jpa:
    show-sql: true
    properties:
      hibernate:
        format_sql: true
logging:
  level:
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
```

### Sans JOIN FETCH → Tu verras :

```
Hibernate: SELECT * FROM posts
Hibernate: SELECT * FROM users WHERE id=?
Hibernate: SELECT * FROM users WHERE id=?
Hibernate: SELECT * FROM users WHERE id=?
...
```

### Avec JOIN FETCH → Tu verras :

```
Hibernate: SELECT p.*, u.* FROM posts p INNER JOIN users u ON p.author_id = u.id
```

**Une seule ligne SQL.**

---

## Comparaison des performances

| Méthode | Requêtes SQL | Performance |
|---------|--------------|-------------|
| `findAll()` + accès LAZY | N+1 | Catastrophique |
| `findAllWithAuthor()` JOIN FETCH | **1** | Optimal |

### Impact réel

| Nombre de posts | Sans JOIN FETCH | Avec JOIN FETCH |
|-----------------|-----------------|-----------------|
| 10 posts | 11 requêtes | 1 requête |
| 100 posts | 101 requêtes | 1 requête |
| 1000 posts | 1001 requêtes | 1 requête |

---

## Bonnes pratiques

### 1. Toujours LAZY par défaut

```java
@ManyToOne(fetch = FetchType.LAZY)
private User author;

@OneToMany(mappedBy = "post", fetch = FetchType.LAZY)
private List<Comment> comments;
```

### 2. JOIN FETCH selon le besoin

```java
// Liste légère - juste l'auteur
@Query("SELECT p FROM Post p JOIN FETCH p.author")
List<Post> findAllWithAuthor();

// Détail complet - auteur + commentaires
@Query("SELECT p FROM Post p JOIN FETCH p.author LEFT JOIN FETCH p.comments WHERE p.id = :id")
Optional<Post> findByIdWithDetails(@Param("id") Long id);
```

### 3. Ne jamais exposer les entités directement

```java
// Mauvais
@GetMapping
public List<Post> list() {
    return postRepository.findAll();  // Risque N+1 dans le sérialiseur JSON
}

// Bon
@GetMapping
public List<PostDto.Response> list() {
    return postRepository.findAllWithAuthor()
        .stream()
        .map(postMapper::toResponse)
        .toList();
}
```

---

## Résumé

| Problème | Solution |
|----------|----------|
| EAGER par défaut | Toujours mettre LAZY |
| N+1 avec LAZY | Utiliser JOIN FETCH |
| Entités dans Controller | Utiliser des DTOs |
| Pas de visibilité sur les requêtes | Activer les logs SQL |
