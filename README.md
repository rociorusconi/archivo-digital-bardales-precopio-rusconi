# Nombre Equipo

Integrantes

nombre user_git
1
2
3
4

# Forma de uso

```
src/main/java/ar/edu/unsam/ddso/tp/
└── TpApplication.java          # entry point
src/main/resources/
├── application.properties        # nombre de la app + perfil activo
├── application-local.properties  # H2 + JPA para desarrollo local
└── application-prod.properties   # H2 + JPA para "prod"
src/test/java/ar/edu/unsam/ddso/tp/
└── TpApplicationTests.java       # smoke test (contextLoads)
```

## Cómo correr los tests

```bash
./mvnw test
```

## Cómo ejecutar la app en local

```bash
./mvnw spring-boot:run
```

Perfil `local` por defecto (H2 in-memory, consola en `http://localhost:8080/h2-console`).

## Bot de Telegram

Para usar el bot, crear un bot propio con [@BotFather](https://t.me/BotFather), copiar `.env.example` como `.env` y pegar el token (`.env` no se commitea). Sin token la aplicación levanta igual y el bot queda apagado. Paso a paso, comandos y casos de prueba en [`docs/bot.md`](docs/bot.md).
