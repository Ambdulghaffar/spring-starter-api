# Sauvegarde et migration des bases Aiven — PostgreSQL 18

## Objectif

Migrer les bases de données de production et de staging vers de nouveaux services Aiven PostgreSQL 18, sans perdre les données.

Le nom d’un service Aiven ne peut pas être modifié directement. Pour le changer, il faut créer un nouveau service, puis y restaurer une sauvegarde de l’ancien.

**Prérequis :**
- Docker installé et démarré.
- PowerShell sous Windows.
- Accès aux anciens et nouveaux services Aiven.
- Image Docker `postgres:18`.
- Maven Wrapper du projet (`mvnw.cmd`).
- Accès aux identifiants de connexion de chaque environnement.

## 1. Sauvegardes réalisées

Les sauvegardes du 12 octobre 2026 sont stockées dans `ops/backups/` :

- `backup-prod-2026-10-12.dump`
- `backup-staging-2026-10-12.dump`

Les dumps sont au format PostgreSQL personnalisé (`-Fc`). Ils ont été créés avec `pg_dump --no-owner --no-acl`.

```powershell
docker run --rm `
  -v "${PWD}\ops\backups:/out" `
  postgres:18 `
  pg_dump `
  "postgres://USER:MOT_DE_PASSE@HOST:PORT/NOM_BASE?sslmode=require" `
  --no-owner `
  --no-acl `
  -Fc `
  -f /out/backup-AAAA-MM-JJ.dump
```

Remplacer les paramètres de connexion et le nom du fichier selon l’environnement.

## 2. Vérification des sauvegardes

Lister le contenu d’un dump :

```powershell
docker run --rm `
  -v "${PWD}\ops\backups:/out" `
  postgres:18 `
  pg_restore -l /out/backup-prod-2026-10-12.dump
```

Répéter avec le fichier staging.

Vérifier notamment la présence des entrées `TABLE DATA` correspondant aux tables applicatives et aux tables de session.

## 3. Création des nouveaux services Aiven

Pour chaque environnement :

1. Créer le nouveau service PostgreSQL 18.
2. Créer ou vérifier la base de données cible dans **Databases**.
3. Créer ou vérifier l’utilisateur applicatif dans **Users**.
4. Récupérer le nom exact de la base, l’hôte, le port et les identifiants de connexion.
5. Vérifier que la base cible est vide avant la restauration.

Ne pas supposer que la base s’appelle `rifline_db` ou `rifline_staging_db` : utiliser le nom réellement affiché dans Aiven.

## 4. Restauration de la production

Exécuter toute la commande en une seule fois dans PowerShell :

```powershell
docker run --rm `
  -v "${PWD}\ops\backups:/out" `
  postgres:18 `
  pg_restore `
  --verbose `
  --no-owner `
  --no-acl `
  --exit-on-error `
  -d "postgres://avnadmin:NOUVEAU_MDP@NOUVEL_HOST:NOUVEAU_PORT/NOUVELLE_BASE?sslmode=require" `
  /out/backup-prod-2026-10-12.dump
```

## 5. Restauration du staging

Utiliser la même procédure avec les informations du service staging et son dump :

```powershell
docker run --rm `
  -v "${PWD}\ops\backups:/out" `
  postgres:18 `
  pg_restore `
  --verbose `
  --no-owner `
  --no-acl `
  --exit-on-error `
  -d "postgres://avnadmin:NOUVEAU_MDP:NOUVEL_HOST:NOUVEAU_PORT/NOUVELLE_BASE?sslmode=require" `
  /out/backup-staging-2026-10-12.dump
```

**Important :** dans la commande de restauration staging, l’URI doit respecter le format suivant :

`postgres://UTILISATEUR:MOT_DE_PASSE@HOST:PORT/NOM_BASE?sslmode=require`

Ne pas ajouter `>>` devant les lignes. Dans PowerShell, les accents graves à la fin des lignes permettent de continuer la commande. Aucun espace ne doit les suivre.

L’option `--exit-on-error` arrête la restauration dès qu’une erreur survient.

## 6. Vérification des bases restaurées

Lister les tables :

```powershell
docker run --rm postgres:18 psql `
  "postgres://avnadmin:NOUVEAU_MDP@NOUVEL_HOST:NOUVEAU_PORT/NOUVELLE_BASE?sslmode=require" `
  -c "SELECT tablename FROM pg_tables WHERE schemaname = 'public' ORDER BY tablename;"
```

Vérifier les statistiques de lignes :

```powershell
docker run --rm postgres:18 psql `
  "postgres://avnadmin:NOUVEAU_MDP@NOUVEL_HOST:NOUVEAU_PORT/NOUVELLE_BASE?sslmode=require" `
  -c "SELECT relname, n_live_tup FROM pg_stat_user_tables ORDER BY relname;"
```

Les statistiques `n_live_tup` sont des estimations. Pour une vérification précise, compter les lignes des tables importantes et comparer les résultats à l’ancienne base.

Vérifier également les données applicatives, les contraintes, les clés étrangères et les séquences.

## 7. Droits de l’utilisateur applicatif

La restauration avec `--no-owner --no-acl` évite de rétablir les anciens propriétaires et permissions. Accorder les droits à l’utilisateur applicatif réel :

```sql
GRANT USAGE, CREATE ON SCHEMA public TO NOM_UTILISATEUR_APP;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO NOM_UTILISATEUR_APP;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO NOM_UTILISATEUR_APP;
```

Exécuter ces instructions avec un compte autorisé, dans la base restaurée.

Si nécessaire, configurer également les privilèges par défaut pour les futurs objets créés par le propriétaire concerné.

## 8. Historique Flyway et migration initiale

### 8.1 Objectif

Le projet utilise une migration SQL initiale consolidée :

`src/main/resources/db/migration/V1__init_schema.sql`

L’objectif est de disposer d’un schéma initial reproductible pour les nouvelles bases, tout en conservant les données des bases existantes.

La table `flyway_schema_history` enregistre les migrations exécutées. **Vider son historique ne supprime pas les tables applicatives.** Si l’historique est vide alors que les tables existent encore, Flyway peut tenter de rejouer V1 et provoquer une erreur telle que :

`ERROR: relation "users" already exists`

### 8.2 Baseline des bases existantes

Pour une base déjà restaurée et dont le schéma correspond à V1, il faut enregistrer une baseline afin que Flyway reconnaisse ce schéma existant sans exécuter de nouveau V1.

La commande suivante a réussi pour le service staging utilisé lors de l’opération :

```powershell
$env:FLYWAY_PASSWORD = Read-Host "Mot de passe Aiven STAGING"

.\mvnw.cmd flyway:baseline `
  "-Dflyway.url=jdbc:postgresql://HOST_STAGING:PORT_STAGING/NOM_BASE_STAGING?sslmode=require" `
  "-Dflyway.user=avnadmin" `
  "-Dflyway.password=$env:FLYWAY_PASSWORD" `
  "-Dflyway.baselineVersion=1"
```

Adapter l’hôte, le port et le nom de base à ceux du service staging réellement ciblé.

La commande doit notamment afficher :

`Successfully baselined schema with version: 1`

### 8.3 Baseline de la production

Appliquer le même principe à la base de production, avec les coordonnées et le mot de passe propres à la production :

```powershell
$env:FLYWAY_PASSWORD = Read-Host "Mot de passe Aiven PROD"

.\mvnw.cmd flyway:baseline `
  "-Dflyway.url=jdbc:postgresql://HOST_PROD:PORT_PROD/NOM_BASE_PROD?sslmode=require" `
  "-Dflyway.user=avnadmin" `
  "-Dflyway.password=$env:FLYWAY_PASSWORD" `
  "-Dflyway.baselineVersion=1"
```

**Si Flyway indique que `flyway_schema_history` existe déjà et est vide**, la commande `baseline` échoue. Après avoir vérifié que l’on est connecté à la bonne base et que le schéma existant est correct, supprimer uniquement cette table d’historique vide :

```sql
DROP TABLE public.flyway_schema_history;
```

Puis relancer la commande `flyway:baseline`.

Cette opération ne doit pas être réalisée à l’aveugle : vérifier l’environnement, la base ciblée et l’état de l’historique avant toute suppression. Ne jamais supprimer les tables applicatives pour résoudre cette erreur.

### 8.4 Différence entre une base existante et une base neuve

- **Base existante restaurée :** utiliser `baseline` en version 1 pour enregistrer le schéma déjà présent, après vérification.
- **Base neuve et vide :** laisser Flyway exécuter `V1__init_schema.sql` normalement.
- **Base dont le schéma ne correspond pas à V1 :** ne pas appliquer de baseline avant d’avoir identifié et corrigé l’écart.

Une baseline ne crée pas les tables, ne copie aucune donnée et ne valide pas automatiquement que le schéma correspond à V1. Elle enregistre un point de départ pour l’historique Flyway.

Après la baseline, les futures migrations doivent avoir une version supérieure à 1.

## 9. Vérification de l’historique Flyway

Dans chaque base, exécuter :

```sql
SELECT installed_rank, version, description, type, success
FROM public.flyway_schema_history
ORDER BY installed_rank;
```

Vérifier que la version 1 est enregistrée et que l’application ne tente plus de recréer les tables existantes.

Après le déploiement, vérifier les journaux Render. Une erreur `relation already exists` indique généralement que Flyway tente d’exécuter une migration de création sur un schéma déjà présent.

L’erreur `No open ports detected` peut être une conséquence de l’arrêt de l’application pendant son initialisation Flyway, et non un problème de port indépendant.

## 10. Configuration de Render

Pour chaque environnement Render :

1. Mettre à jour les variables de connexion avec le nouvel hôte, le port, le nom de base et les identifiants correspondants.
2. Vérifier que staging pointe vers la base staging et que production pointe vers la base de production.
3. Vérifier la configuration Flyway et Hibernate (`ddl-auto=validate` ou `none`, selon la stratégie retenue).
4. Déployer l’application.
5. Contrôler les journaux de démarrage.
6. Tester la connexion, l’authentification et les opérations CRUD.

Ne jamais enregistrer les mots de passe réels dans le README, le dépôt Git ou les commandes partagées. Utiliser les variables d’environnement ou une saisie interactive.

## 11. Bascule finale

Après validation des deux restaurations :

1. Mettre l’application en pause si des écritures sont encore possibles.
2. Si des changements ont eu lieu depuis la sauvegarde, réaliser un dump final et le restaurer sur la cible.
3. Mettre à jour les variables d’environnement locales et celles de Render.
4. Vérifier les URL JDBC et les identifiants.
5. Déployer et tester l’application.
6. Conserver les anciens services quelques jours après validation avant toute suppression définitive.

**Règle essentielle :** ne supprimer un ancien service Aiven qu’après avoir vérifié les données, le démarrage de l’application et le fonctionnement des opérations principales sur les nouveaux services.