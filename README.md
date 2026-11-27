# Archivo Digital — Los Copiapega

TP Integrador de Diseño de Software (UNSAM): monolito Spring Boot que digitaliza, cataloga y pone a
disposición un archivo de piezas físicas, con APIs REST y un bot de Telegram para la búsqueda. El
dominio y la arquitectura están en [`ARCHITECTURE.md`](ARCHITECTURE.md) y las convenciones de
código en [`AGENTS.md`](AGENTS.md).

## Integrantes

| Nombre | Usuario de GitHub |
|---|---|
| Wilfredo Bardales | [@willybc](https://github.com/willybc) |
| Kiara Precopio | [@KiaraPrecopio](https://github.com/KiaraPrecopio) |
| Rocio Rusconi | [@rociorusconi](https://github.com/rociorusconi) |

## Requisitos

- Java 21 (el proyecto trae Maven Wrapper, no hace falta instalar Maven).
- Para el bot, un token propio de Telegram (opcional: sin token la aplicación levanta igual).

## Cómo ejecutar la app en local

```bash
./mvnw spring-boot:run
```

Perfil `local` por defecto: H2 en memoria, con los datos de prueba de
`src/main/resources/data.sql` cargados al arrancar. Con la app levantada:

| Qué | Dónde |
|---|---|
| API REST | `http://localhost:8080/api/...` |
| Swagger UI (ver y probar los endpoints desde el navegador) | `http://localhost:8080/swagger-ui.html` |
| Consola de H2 (JDBC URL `jdbc:h2:mem:tp`, usuario `SA`, sin contraseña) | `http://localhost:8080/h2-console` |

La base vive en memoria: al reiniciar la aplicación se vuelve a los datos de `data.sql`.

## Cómo correr los tests

```bash
./mvnw test
```

## Bot de Telegram

Para usar el bot, crear un bot propio con [@BotFather](https://t.me/BotFather), copiar `.env.example` como `.env` y pegar el token (`.env` no se commitea). Sin token la aplicación levanta igual y el bot queda apagado. Paso a paso, comandos y casos de prueba en [`docs/bot.md`](docs/bot.md).

## Documentación

| Documento | Contenido |
|---|---|
| [`docs/api.md`](docs/api.md) | Especificación de la API REST: endpoints, parámetros, respuestas y errores |
| [`docs/test.md`](docs/test.md) | Secuencia de prueba del sistema (camino feliz y casos de error) |
| [`docs/bot.md`](docs/bot.md) | Bot de Telegram: cómo levantarlo, comandos, despliegue y diseño |
| [`docs/diagrams/architecture-diagram.md`](docs/diagrams/architecture-diagram.md) | Componentes por capa y paquetes |
| [`docs/diagrams/class-diagram.md`](docs/diagrams/class-diagram.md) | Diagramas de clases por módulo |
| [`docs/diagrams/database-diagram.md`](docs/diagrams/database-diagram.md) | Tablas de la base de datos |
| `docs/diagrams/sequence-*.md` | Diagramas de secuencia: [crear pieza](docs/diagrams/sequence-create-piece.md), [agregar digitalización](docs/diagrams/sequence-add-digitization.md), [asignar tag](docs/diagrams/sequence-assign-tag.md) y [búsqueda desde el bot](docs/diagrams/sequence-bot-search.md) |
| [`Backlog.md`](Backlog.md) | Issues, labels y tablero del proyecto |

## Estructura del proyecto

```
src/main/java/ar/edu/unsam/ddso/tp/
├── TpApplication.java   # entry point
├── controller/          # @RestController: mapean HTTP a los services
├── dto/                 # records de request/response
├── service/             # orquestación: buscar, aplicar reglas, persistir, mapear a DTO
├── repository/          # acceso a datos (Spring Data JPA)
├── model/               # entidades @Entity y reglas de negocio
│   └── exceptions/      # excepciones de reglas de negocio
├── exception/           # "no encontrado" + GlobalExceptionHandler
├── config/              # OpenApiConfig (Swagger UI)
└── bot/                 # bot de Telegram (Visualización)
src/main/resources/
├── application.properties        # nombre de la app + perfil activo
├── application-local.properties  # H2 en memoria + datos semilla + bot en long polling
├── application-prod.properties   # config de "prod" (variables de entorno, bot por webhook opcional)
└── data.sql                      # datos semilla del perfil local
src/test/java/ar/edu/unsam/ddso/tp/
├── TpApplicationTests.java       # smoke test (contextLoads)
├── service/                      # tests unitarios de los services
└── bot/                          # tests unitarios del bot
```
