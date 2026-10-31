# Swagger / OpenAPI

Ce fichier décrit comment accéder et utiliser la documentation Swagger (OpenAPI) fournie par ce projet.

## URLs importantes
- Swagger UI (interface interactive) :
    - http://localhost:8081/swagger-ui.html
    - ou : http://localhost:8081/swagger-ui/index.html

- OpenAPI JSON :
    - http://localhost:8081/v3/api-docs

- OpenAPI YAML :
    - http://localhost:8081/v3/api-docs.yaml

- Redirect URI pour OAuth2 / Swagger UI (à enregistrer dans la configuration du client Keycloak) :
    - http://localhost:8081/swagger-ui/oauth2-redirect.html

(Remplacez le port et le host si votre application utilise d'autres valeurs.)

## Utilisation de Swagger UI
1. Ouvrez la Swagger UI dans votre navigateur (URL ci-dessus).
2. Vous verrez la liste des endpoints REST exposés par l'application. Cliquez sur une route pour afficher les détails et tester l'appel (bouton "Try it out").

### Authentification / Keycloak
Si votre application est protégée par Keycloak (configuration `spring-boot-starter-security` + resource server), vous pouvez utiliser la Swagger UI pour vous authentifier et enrichir les requêtes avec un token :

- La propriété `springdoc.swagger-ui.oauth.client-id` (et `client-secret`) dans `application.yml` permet de préconfigurer le bouton d'autorisation de Swagger UI.
- Dans Keycloak, créez/éditez un client correspondant (par exemple `inventory-client`) et ajoutez la Redirect URI :
    - `http://localhost:8081/swagger-ui/oauth2-redirect.html`
- Méthodes pour obtenir un token et l'utiliser dans Swagger UI :
    - Utiliser le bouton "Authorize" si la configuration OAuth2 est exposée dans Swagger UI — cela déclenchera le flux d'authentification et injectera le token.
    - Ou obtenir un token manuellement (ex. flux Resource Owner / Password ou Client Credentials) et l'ajouter via le champ "Bearer token".

Exemple (remplacez <KEYCLOAK_HOST>, <REALM>, identifiants) :

```
# Exemple: récupérer un access_token via grant_type=password
curl -X POST "http://<KEYCLOAK_HOST>/realms/<REALM>/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=inventory-client&client_secret=your-client-secret&grant_type=password&username=admin&password=admin"

# La réponse contient access_token -> copiez-le et utilisez-le dans Swagger UI (Authorize -> Bearer <token>)
```

Ou pour client credentials :

```
curl -X POST "http://<KEYCLOAK_HOST>/realms/<REALM>/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=inventory-client&client_secret=your-client-secret&grant_type=client_credentials"
```

Remarque : selon la configuration Keycloak et les politiques de sécurité, vous pourriez préférer PKCE ou d'autres flux plus sûrs.

## Dépannage
- Si Swagger UI renvoie 404 : vérifiez `server.port` et `server.servlet.context-path` dans `application.yml`.
- Si les endpoints ne sont pas visibles : assurez-vous que vos controllers sont correctement annotés (`@RestController`, `@RequestMapping`/`@GetMapping` etc.) et que le démarrage de l'application n'a pas d'erreurs.
- Si l'authentification bloque les appels depuis Swagger : vérifiez que le client Keycloak autorise la redirection sur l'URL de Swagger UI et que `client-id`/`client-secret` correspondent à la config.

## Remarques finales
- Cette documentation est volontairement générique : adaptez les URLs, le client-id et le secret à votre environnement Keycloak.
- Pour personnaliser l'apparence ou le comportement de Swagger UI, consultez la documentation de springdoc-openapi : https://springdoc.org/

