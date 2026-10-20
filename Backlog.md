# Backlog

Backlog del TPI **Archivo Digital**. Cada ítem corresponde a un Issue de GitHub, organizado en el
tablero de GitHub Projects y trazado a los requerimientos funcionales (RF) mediante labels.
Contexto del dominio y arquitectura: [`ARCHITECTURE.md`](ARCHITECTURE.md). Convenciones de
código: [`AGENTS.md`](AGENTS.md).

## 1. Tablero Kanban

El tablero tiene una columna por módulo del dominio, más una de documentación:

| Columna | Contenido |
|---|---|
| Digitalización | Alta de piezas físicas y registro de sus digitalizaciones |
| Catalogación | Gestión de tags y asignación a digitalizaciones |
| Disponibilización | Búsqueda de piezas por múltiples criterios |
| Visualización | Bot de Telegram |
| Documentación | Diagramas, `api.md` y `test.md` |

## 2. Labels

| Label | Significado |
|---|---|
| `RF-DIG-01` | Registro y seguimiento de cada pieza física e inventario digital |
| `RF-DIG-02` | Registro de metadatos básicos de digitalización |
| `RF-CAT-01` | Enriquecimiento del registro con metadatos descriptivos y tags |
| `RF-DISP-03` | Motor de búsqueda y consulta por múltiples criterios |
| `RF-VIS-01` | Consulta pública e interacción vía bot de Telegram |
| `Documentación` | Artefactos, diagramas (`.puml` / `.mermaid`), `api.md` y `test.md` |
| `Transversal` | *(propuesto)* Tareas que no pertenecen a un único módulo |

## 3. Issues

### Digitalización

**#1 — Crear entidad Pieza** · `RF-DIG-01`
Entidad JPA `Piece` con `id`, `name`, `description` y `date`.
- Mapeada a la tabla `pieces` siguiendo las convenciones de `AGENTS.md`.
- Nombre obligatorio.

**#2 — Crear entidad Digitalizacion** · `RF-DIG-01`, `RF-DIG-02`
Entidad JPA `Digitization` (URL + descripción) asociada a una `Piece`.
- Toda digitalización pertenece a una pieza (relación muchos a uno).
- URL obligatoria.

**#3 — Implementar Service para alta de Pieza** · `RF-DIG-01`, `RF-DIG-02`
Lógica para persistir y actualizar piezas y sus digitalizaciones.
- Alta de pieza y alta de digitalización sobre una pieza existente.
- Pieza inexistente → excepción de "no encontrado".
- Incluye test unitario del service (JUnit 5 + Mockito).

**#4 — Crear Controller para API de Digitalización** · `RF-DIG-01`
Endpoints REST para recibir las solicitudes de alta.
- Usa DTOs (`record`), nunca expone entidades.
- Códigos HTTP correctos: `201` en alta, `404` si la pieza no existe.

### Catalogación

**#5 — Crear entidad Tag** · `RF-CAT-01`
Entidad JPA `Tag` y su relación muchos a muchos con `Digitization`.
- Nombre único.

**#6 — Implementar TagService y Test Unitario** · `RF-CAT-01`
Servicio de gestión de tags y asignación a digitalizaciones.
- El administrador puede crear, modificar y eliminar tags en tiempo de ejecución.
- Solo se pueden asignar tags existentes en el conjunto vigente.
- Test unitario con JUnit/Mockito.

**#7 — Crear TagController para API de Catalogación** · `RF-CAT-01`
Endpoints REST para administrar tags y asignarlos o consultarlos sobre digitalizaciones.
- Alta, modificación, baja y listado de tags.
- Asignar y consultar tags de una digitalización.

### Disponibilización

**#8 — Implementar Servicio de Búsqueda de Piezas** · `RF-DISP-03`
Filtrado de **piezas** por fecha, nombre, descripción y tags.
- Devuelve piezas, no digitalizaciones.
- Resultados paginados (se asume un volumen grande de piezas).
- Criterios combinables y opcionales.
- Pendiente de definir: tags con AND u OR, y fecha exacta o rango (ver `ARCHITECTURE.md`, sección 8).

**#9 — Crear Controller para API de Búsqueda** · `RF-DISP-03`
Endpoint REST de consulta.
- Parámetros de búsqueda por query string.
- Respuesta paginada con DTOs.

### Visualización

**#10 — Configurar integración con Bot de Telegram** · `RF-VIS-01`
Configuración inicial del cliente y token.
- El token se lee de una variable de entorno o propiedad externa, nunca se commitea.
- El bot arranca junto con la aplicación.

**#11 — Implementar comandos de consulta en el Bot** · `RF-VIS-01`
Respuesta a comandos de búsqueda.
- Usa el service de búsqueda (#8), sin acceder a repositorios.
- Muestra los resultados en un formato legible y maneja el caso sin resultados.

### Documentación

**#12 — Elaborar Diagramas de Arquitectura y Clases** · `Documentación`
Diagramas de componentes, paquetes y modelo de clases.
- Se agrega: diagrama de base de datos y diagramas de secuencia de los flujos principales, marcando la capa de cada clase (requeridos por la consigna).

**#13 — Redactar api.md y test.md** · `Documentación`
Especificación de la API REST y secuencia de prueba del sistema.
- `test.md` recorre: crear tag, crear pieza, crear digitalización, asignar tag y buscar.

**#16 — Completar README** · `Documentación`
Nombre del equipo, integrantes con su usuario de GitHub y cómo levantar la aplicación.

## 4. Issues transversales

Cubren requisitos de la consigna o de `AGENTS.md` que los 13 issues actuales no contemplan.

**#14 — Manejo global de errores** · `Transversal`
Excepciones de dominio (`model/exceptions`), excepciones de "no encontrado" (`exception`) y
`GlobalExceptionHandler` que devuelve `{"error": mensaje}` con el status correspondiente.

**#15 — Datos semilla para perfil local** · `Transversal`
`data.sql` con algunas piezas, digitalizaciones y tags para poder probar el recorrido completo.


## 5. Requisitos de entrega a tener presentes

- Todos los integrantes necesitan al menos un commit que aporte al trabajo (no solo documentación).
- Todos los integrantes necesitan un test unitario de un service y poder explicarlo. Con los
  issues actuales hay cuatro candidatos: `PieceService` (#3), `TagService` (#6), el servicio de
  búsqueda (#8) y la lógica de comandos del bot (#11). Conviene asignarlos de forma que cada
  persona quede con uno.

## 6. Orden de trabajo sugerido

| Etapa | Issues | Motivo |
|---|---|---|
| 1. Modelo | #1, #2, #5, #14 | Todo lo demás depende de las entidades y del manejo de errores |
| 2. Lógica | #3, #6 | Services con sus tests unitarios |
| 3. APIs | #4, #7, #15 | Permite el primer recorrido de punta a punta por HTTP |
| 4. Búsqueda | #8, #9 | Requiere piezas con digitalizaciones y tags cargados |
| 5. Bot | #10, #11 | Consume el service de búsqueda |
| 6. Documentación | #12, #13, #16 | Los diagramas y `test.md` se validan contra la app funcionando |

Los diagramas de clases y de base de datos de #12 pueden empezar antes (después de la etapa 1);
`test.md` y los diagramas de secuencia conviene cerrarlos al final.
