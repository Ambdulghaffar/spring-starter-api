## Lancer l'environnement Docker local

> Se placer dans le dossier `ops/` avant d'exécuter les commandes.

```powershell
cd ops
```

### 1. Réinitialiser l'environnement

```powershell
docker compose --env-file ../.env down -v
```

### 2. Démarrer les services

```powershell
docker compose --env-file ../.env up -d
```

### 3. Vérifier les bases PostgreSQL

```powershell
docker exec -it rifline-db psql -U admin -d postgres -c "\l"
```

Résultat attendu :

```text
                 List of databases
     Name      | Owner | Encoding | Locale Provider |  Collate   |   Ctype    | ICU Locale | ICU Rules | Access privileges
---------------+-------+----------+-----------------+------------+------------+------------+------------+-------------------
 keycloak_db   | admin | UTF8     | libc            | en_US.utf8 | en_US.utf8 |            |           |
 kickstart_db  | admin | UTF8     | libc            | en_US.utf8 | en_US.utf8 |            |           |
 postgres      | admin | UTF8     | libc            | en_US.utf8 | en_US.utf8 |            |           |
 template0     | admin | UTF8     | libc            | en_US.utf8 | en_US.utf8 |            |           | =c/admin
 template1     | admin | UTF8     | libc            | en_US.utf8 | en_US.utf8 |            |           | =c/admin
(5 rows)
```

### 4. Vérifier les logs Keycloak

```powershell
docker compose --env-file ../.env logs keycloak --tail=100
```
