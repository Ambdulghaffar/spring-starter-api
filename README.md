# Spring Boot Starter Kit

### Clean package and skip tests
```bash
 mvn clean package -DskipTests
```
A starter kit for building Spring Boot applications with essential dependencies pre-configured.

## Requirements

- **Java Version:** 21

## Installed Dependencies

The following dependencies are installed in this project:

| Dependency                                                                              | Category | Description |
|-----------------------------------------------------------------------------------------|----------|-------------|
| Spring Boot DevTools                                                                    | Developer Tools | Provides fast application restarts, LiveReload, and configurations for enhanced development experience. |
| Lombok                                                                                  | Developer Tools | Java annotation library which helps to reduce boilerplate code. |
| Spring Web                                                                              | Web | Build web, including RESTful, applications using Spring MVC. Uses Apache Tomcat as the default embedded container. |
| Spring Security                                                                         | Security | Highly customizable authentication and access-control framework for Spring applications. |
| OAuth2 Resource Server                                                                  | Security | Spring Boot integration for Spring Security's OAuth2 resource server features. |
| PostgreSQL Driver                                                                       | SQL | A JDBC and R2DBC driver that allows Java programs to connect to a PostgreSQL database using standard, database independent Java code. |
| Spring Data JPA                                                                         | SQL | Persist data in SQL stores with Java Persistence API using Spring Data and Hibernate. |
| Spring Validation                                                                       | Web | Bean Validation with Hibernate validator. |
| SpringDoc OpenAPI                                                                       | Documentation | OpenAPI 3 documentation for Spring Boot applications. |
| ⚠️ [MapStruct](https://mapstruct.org/documentation/stable/reference/html/#introduction) | Developer Tools | Code generator that simplifies mappings between Java bean types. |

![spring_initializr.png](spring_initializr.png)
