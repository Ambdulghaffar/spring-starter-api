# Docker

## Démarrer les services (Docker Compose)
Ouvrez PowerShell dans la racine du projet (ou dans `ops`) puis lancez :

```powershell
# Affiche la configuration interpolée (utile pour vérifier les valeurs)
docker compose --env-file .env -f ops/docker-compose.yml config

# Démarrer uniquement la base Postgres
docker compose --env-file .env -f ops/docker-compose.yml up -d db

# Lancer et reconstruire les services (détaché)
docker compose -f ops/docker-compose.yml up --build -d

# Suivre les logs
docker logs --tail 100 kickstart_springboot_db
docker compose -f ops/docker-compose.yml logs -f

# Arrêter et supprimer conteneurs et volumes
docker compose -f ops/docker-compose.yml down -v

# Démarrer plusieurs services
docker compose -f ops/docker-compose.yml up -d db keycloak
```

## Postgres command utilise
Vérifier que la base de données a été créée :

````bash
docker exec -it kickstart_springboot_db psql -U root -l

                                                        List of databases
         Name         | Owner | Encoding | Locale Provider |  Collate   |   Ctype    | ICU Locale | ICU Rules | Access privileges 
----------------------+-------+----------+-----------------+------------+------------+------------+-----------+-------------------
 hotel | root  | UTF8     | libc            | en_US.utf8 | en_US.utf8 |            |           | 
 postgres             | root  | UTF8     | libc            | en_US.utf8 | en_US.utf8 |            |           | 
 template0            | root  | UTF8     | libc            | en_US.utf8 | en_US.utf8 |            |           | =c/root          +
                      |       |          |                 |            |            |            |           | root=CTc/root
 template1            | root  | UTF8     | libc            | en_US.utf8 | en_US.utf8 |            |           | =c/root          +

````

## Create and Recreate Docker contianers
Pour (re)créer les conteneurs Docker (Postgres + Spring Boot app)
```powershell
# Arrêter et supprimer les conteneurs et le volume (ATTENTION: supprime les données)
docker compose -f ops/docker-compose.yml down -v

docker compose --env-file .env -f ops/docker-compose.yml up --build -d
```

## Lancer Postgres avec docker run
Équivalent de la commande fournie :

```bash
docker run -d --name kickstart_springboot_db -p 5432:5432 -e POSTGRES_PASSWORD=yourstrongpassword postgres:16
```
## Construire l'application Spring Boot
Sur Windows (PowerShell) :

```powershell
# Construire le jar (wrapper Maven inclus dans le repo)
.\mvnw.cmd -DskipTests package
```

Puis construire l'image Docker depuis la racine (si `Dockerfile` présent) :

```bash
docker build -t spring-boot-app .
```

### Forcer le chargement d'un `.env` avec Docker Compose
Parfois Docker Compose ne charge pas le `.env` que vous pensez utiliser.
Pour être sûr que Compose utilise bien votre fichier `.env`, utilisez l'option `--env-file` :

```powershell
# Affiche la configuration interpolée (utile pour vérifier les valeurs)
docker compose --env-file .env -f ops/docker-compose.yml config

# Démarrer uniquement la base Postgres en forçant l'usage de .env
docker compose --env-file .env -f ops/docker-compose.yml up -d db

# Démarrer tout le stack en s'assurant que .env est pris en compte
docker compose --env-file .env -f ops/docker-compose.yml up --build -d
```

Si vous acceptez de supprimer les données et repartir à zéro (méthode destructive) :

```powershell
# Arrêter et supprimer les conteneurs et le volume (ATTENTION: supprime les données)
docker compose -f ops/docker-compose.yml down -v

# Recréer le stack (Postgres va initialiser le volume et créer POSTGRES_DB)
docker compose --env-file .env -f ops/docker-compose.yml up --build -d
```
---

## Conseils pratiques
- Gardez un fichier `.env.example` dans le repo (sans secrets) et mettez `.env` dans `.gitignore`.
- Pour les environnements reproductibles, placez des scripts SQL d'initialisation dans `./ops/db/init` et montez-les sur `/docker-entrypoint-initdb.d` pour que Postgres les exécute au premier démarrage.
- Si votre application utilise `devtools`, notez que Spring ne reconnecte pas toujours automatiquement à la DB si la base est créée après le démarrage — un redémarrage de l'app peut être nécessaire.
