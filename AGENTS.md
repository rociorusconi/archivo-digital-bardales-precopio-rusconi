# AGENTS.md

Convenciones de este proyecto (Spring Boot + H2 + JPA).

Para la descripción del dominio y la arquitectura general (actores, alcance, diagramas C4), ver
[`ARCHITECTURE.md`](ARCHITECTURE.md).

## Stack

- Java 21, Spring Boot (`spring-boot-starter-parent`).
- Persistencia: Spring Data JPA + H2 (in-memory, `spring.jpa.hibernate.ddl-auto=create-drop` en `local`).
- Lombok para reducir boilerplate en entidades.
- Testing: JUnit 5 + Mockito (`@ExtendWith(MockitoExtension.class)`), sin Spring context en los
  tests unitarios (mocks manuales, sin `@SpringBootTest` salvo el smoke test de la aplicación).

## Paquetes

Todo bajo `ar.edu.unsam.ddso.tp`, organizado por capa (no por feature):

- `controller` — `@RestController`, mapean HTTP a llamadas al `service`. Sin lógica de negocio.
- `dto` — `record`s inmutables para requests/responses. Nunca se expone una entidad JPA directo
  en un controller.
- `model` — entidades `@Entity`. Contienen las reglas de negocio/invariantes del dominio (los
  métodos de negocio viven en la entidad, no en el service, cuando son invariantes propias del
  objeto).
  - `model/enums` — enums del dominio.
  - `model/exceptions` — excepciones de **reglas de negocio del dominio** (ej. violar un
    invariante). Extienden `RuntimeException`.
- `exception` — excepciones **de infraestructura/HTTP** (ej. "no encontrado") más el
  `@RestControllerAdvice` (`GlobalExceptionHandler`) que las traduce a `ResponseEntity` con el
  status HTTP correspondiente.
- `repository` — interfaces de acceso a datos. Ver patrón abajo.
- `service` — orquestación: busca entidades, invoca las reglas de negocio del `model`, persiste,
  mapea a DTOs. Constructor injection siempre (no `@Autowired` en campos).

## Entidades (`model`)

- Anotadas con `@Entity`, `@Table(name = "...")` en snake_case plural.
- Lombok: `@Getter @Setter @NoArgsConstructor @AllArgsConstructor` en la clase — no escribir
  getters/setters a mano.
- `@Id @GeneratedValue(strategy = GenerationType.IDENTITY)` para la PK.
- `BigDecimal` para montos, con `@Column(precision = 12, scale = 2)`.
- Las invariantes de negocio (ej. transferencias, validaciones de estado) son métodos de la
  propia entidad, que lanzan excepciones de `model/exceptions` cuando se violan.

## DTOs

- `record`, no clases. Un DTO por response/request, nombrados `<Algo>RequestDTO` / `<Algo>ResponseDTO`
  (sufijo `DTO` al final del nombre, ej. `ItemCreateRequestDTO`, `ItemResponseDTO`).
- Sin lógica; solo transporte de datos.

## Repositorios

Patrón para poder tener datos de prueba sin depender de la base real:

- Una interfaz de dominio en `repository` (ej. `ItemRepository`) con los métodos que necesita
  el `service`.
- Una implementación JPA (`XxxRepositoryJpa`) que extiende `JpaRepository` y la interfaz de
  dominio, activa con `@Profile({"local", "prod"})`.
- Una implementación en memoria (`XxxRepositoryEnMemoria`) para el perfil `test`, útil para
  levantar la app sin base de datos real y con datos hardcodeados de ejemplo.
- Los repositorios de Spring Data puro (sin necesidad de swap in-memory) son simplemente
  interfaces `extends JpaRepository<Entidad, Long>` con métodos derivados por nombre
  (`findByXxxOrderByYyyDesc`, etc.), sin `@Query` a menos que sea imprescindible.

## Services

- Un `@Service` por agregado principal, constructor injection de sus repositorios.
- Métodos que mutan estado van con `@Transactional`.
- Métodos públicos separados con comentarios de sección (`// ---------- Consultas ----------`,
  `// ---------- Operaciones ----------`, `// ---------- Privados ----------`).
- El service busca la entidad (lanzando la excepción de "no encontrado" si falta), delega la
  regla de negocio a la entidad, persiste el resultado y mapea a DTO antes de devolver.

## Excepciones y manejo de errores

- Dos familias, no las mezclar:
  - `model/exceptions`: violación de una regla de negocio (ej. `InvalidStateException`).
  - `exception`: recurso no encontrado / errores de infraestructura (ej.
    `ItemNotFoundException`), capturadas en `GlobalExceptionHandler`
    (`@RestControllerAdvice`) que devuelve `Map.of("error", mensaje)` con el `HttpStatus` que
    corresponda.
- Cada excepción de negocio lleva su propio constructor con el mensaje.

## Configuración

- `application.properties`: solo `spring.application.name` y `spring.profiles.active`.
- Un `application-<profile>.properties` por entorno (`local`, `prod`), con la config de
  datasource/H2/JPA de ese entorno. `local` usa H2 con `create-drop` y consola habilitada;
  `prod` usa `update` y consola deshabilitada.
- Datos de prueba vía `data.sql` (`spring.sql.init.mode=always` +
  `spring.jpa.defer-datasource-initialization=true` en `local`) cuando haga falta semilla.

## Tests

- Unitarios con JUnit 5 + Mockito, sin levantar contexto de Spring:
  - `@ExtendWith(MockitoExtension.class)`, `@Mock` para los repositorios, se instancia el
    `service`/entidad a mano.
  - Comentarios `// Given`, `// When`, `// Then` para marcar las secciones del test (en inglés,
    como el resto del código).
  - `assertEquals`, `assertThrows` de `org.junit.jupiter.api.Assertions` (import estático).
- `XxxApplicationTests` con `@SpringBootTest` y un `contextLoads()` vacío como smoke test.
- Tests de entidades (reglas de negocio) separados de tests de service (orquestación).

## Estilo y calidad

- Formateo automático con `pre-commit` (`.pre-commit-config.yaml`):
  - `pretty-format-java --aosp` (google-java-format, estilo AOSP) — no discutir el formato,
    dejar que lo aplique el hook.
  - `trailing-whitespace`, `end-of-file-fixer`, `check-yaml/json/xml`, `pretty-format-json`,
    `pretty-format-yaml`.
  - `mvn checkstyle:check`, `mvn pmd:check`, `mvn spotbugs:check` como hooks locales.
- `checkstyle.xml` es un ruleset liviano a propósito (no exige Javadoc, permite líneas hasta 120
  columnas, no obliga `final` en parámetros): solo chequea imports, naming, buenas prácticas
  básicas (`EqualsHashCode`, `NeedBraces`, `HiddenField` con excepciones para constructor/setter).
  No endurecerlo sin que lo pida el usuario.
- **Todo el código va en inglés**: clases, campos,
  métodos, comentarios, mensajes de excepción, nombres de test. La documentación (este archivo,
  `README.md`, `ARCHITECTURE.md`, `spec.md`/`plan.md`) se mantiene en español.
- Imports: sin wildcard imports en código de producción (sí se usa `import static ... *` en
  tests para `Mockito`/`Assertions`).

## Antes de dar por terminada una tarea

- `./mvnw test` debe pasar.
- Si se corre `pre-commit run --all-files`, dejar que el formatter reescriba los archivos y
  volver a revisar el diff.
