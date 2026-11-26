# API REST — Archivo Digital

Documentación de los endpoints expuestos por la aplicación.

## Información general

- **URL base (perfil local):** `http://localhost:8080`
- **Formato:** todos los pedidos y respuestas usan JSON (`Content-Type: application/json`).
- **Fechas:** formato ISO `yyyy-MM-dd` (por ejemplo `1977-05-03`).
- **Errores:** los errores del dominio se devuelven con este formato, generado por
  `GlobalExceptionHandler`:

```json
{ "status": 404, "message": "Piece not found with id: 99" }
```

| Código | Significado |
|---|---|
| `200 OK` | Consulta o modificación exitosa |
| `201 Created` | Recurso creado |
| `204 No Content` | Recurso eliminado |
| `400 Bad Request` | Datos inválidos (nombre de tag vacío, rango de fechas invertido, fecha mal escrita) |
| `404 Not Found` | El recurso pedido no existe |
| `409 Conflict` | Conflicto con el estado actual (tag duplicado o tag en uso) |

## Resumen de endpoints

| Módulo | Método | URL | Descripción |
|---|---|---|---|
| Digitalización | `POST` | `/api/pieces` | Crear una pieza |
| Digitalización | `POST` | `/api/pieces/{id}/digitizations` | Agregar una digitalización a una pieza |
| Catalogación | `GET` | `/api/tags` | Listar todos los tags |
| Catalogación | `GET` | `/api/tags/{id}` | Obtener un tag por id |
| Catalogación | `POST` | `/api/tags` | Crear un tag |
| Catalogación | `PUT` | `/api/tags/{id}` | Renombrar un tag |
| Catalogación | `DELETE` | `/api/tags/{id}` | Eliminar un tag |
| Catalogación | `POST` | `/api/digitizations/{id}/tags/{tagId}` | Asignar un tag a una digitalización |
| Disponibilización | `GET` | `/api/pieces/search` | Buscar piezas con filtros y paginación |

---

## Digitalización

Controller: `PieceController` · Service: `PieceService`

### `POST /api/pieces` — Crear una pieza

Registra una nueva pieza física en el archivo.

**Body**

| Campo | Tipo | Obligatorio | Descripción |
|---|---|---|---|
| `name` | `String` | Sí | Nombre de la pieza |
| `description` | `String` | No | Descripción (hasta 1000 caracteres) |
| `date` | `LocalDate` | No | Fecha de la pieza |

```json
{ "name": "Letter from Cordoba", "description": "Handwritten letter", "date": "1977-05-03" }
```

**Respuesta `201 Created`**

```json
{ "id": 1, "name": "Letter from Cordoba", "description": "Handwritten letter", "date": "1977-05-03" }
```

> **Pendiente:** hoy no se valida que `name` venga informado. Si falta, la base de datos
> rechaza el registro y la respuesta es un `500` genérico en lugar de un `400`.

### `POST /api/pieces/{id}/digitizations` — Agregar una digitalización

Registra una digitalización (archivo digital) para una pieza existente. La digitalización se
crea sin tags.

**Parámetros de URL**

| Parámetro | Tipo | Descripción |
|---|---|---|
| `id` | `Long` | Id de la pieza |

**Body**

| Campo | Tipo | Obligatorio | Descripción |
|---|---|---|---|
| `url` | `String` | Sí | Ubicación del archivo digital |
| `description` | `String` | No | Descripción (hasta 1000 caracteres) |

```json
{ "url": "https://files.example/letter-p1.pdf", "description": "Scan of page 1" }
```

**Respuesta `201 Created`**

```json
{ "id": 10, "url": "https://files.example/letter-p1.pdf", "description": "Scan of page 1", "pieceId": 1, "tags": [] }
```

**Errores**

| Código | Cuándo | Ejemplo |
|---|---|---|
| `404` | La pieza no existe | `{ "status": 404, "message": "Piece not found with id: 99" }` |

> **Pendiente:** al igual que en piezas, si falta `url` la respuesta es un `500` genérico.

---

## Catalogación

Controller: `TagController` · Service: `TagService`

### `GET /api/tags` — Listar tags

Devuelve todos los tags existentes.

**Respuesta `200 OK`**

```json
[
  { "id": 1, "name": "Press" },
  { "id": 2, "name": "Letters" }
]
```

### `GET /api/tags/{id}` — Obtener un tag

**Parámetros de URL**

| Parámetro | Tipo | Descripción |
|---|---|---|
| `id` | `Long` | Id del tag |

**Respuesta `200 OK`**

```json
{ "id": 1, "name": "Press" }
```

**Errores**

| Código | Cuándo | Ejemplo |
|---|---|---|
| `404` | El tag no existe | `{ "status": 404, "message": "Tag not found: 99" }` |

### `POST /api/tags` — Crear un tag

El nombre se guarda sin espacios al principio ni al final. No puede haber dos tags con el mismo
nombre, sin distinguir mayúsculas de minúsculas (`Press` y `press` son el mismo tag).

**Body**

| Campo | Tipo | Obligatorio | Descripción |
|---|---|---|---|
| `name` | `String` | Sí | Nombre del tag |

```json
{ "name": "Press" }
```

**Respuesta `201 Created`**

```json
{ "id": 1, "name": "Press" }
```

**Errores**

| Código | Cuándo | Ejemplo |
|---|---|---|
| `400` | El nombre está vacío | `{ "status": 400, "message": "Tag name must not be empty" }` |
| `409` | Ya existe un tag con ese nombre | `{ "status": 409, "message": "Tag already exists: Press" }` |

### `PUT /api/tags/{id}` — Renombrar un tag

Aplica las mismas reglas de nombre que la creación.

**Parámetros de URL**

| Parámetro | Tipo | Descripción |
|---|---|---|
| `id` | `Long` | Id del tag |

**Body**

```json
{ "name": "Newspapers" }
```

**Respuesta `200 OK`**

```json
{ "id": 1, "name": "Newspapers" }
```

**Errores**

| Código | Cuándo | Ejemplo |
|---|---|---|
| `400` | El nombre está vacío | `{ "status": 400, "message": "Tag name must not be empty" }` |
| `404` | El tag no existe | `{ "status": 404, "message": "Tag not found: 99" }` |
| `409` | Otro tag ya tiene ese nombre | `{ "status": 409, "message": "Tag already exists: Newspapers" }` |

### `DELETE /api/tags/{id}` — Eliminar un tag

Quita un tag del conjunto vigente. Solo se puede eliminar un tag que no esté asignado a ninguna
digitalización, para no perder catalogación sin aviso.

**Parámetros de URL**

| Parámetro | Tipo | Descripción |
|---|---|---|
| `id` | `Long` | Id del tag |

**Respuesta `204 No Content`** (sin body)

**Errores**

| Código | Cuándo | Ejemplo |
|---|---|---|
| `404` | El tag no existe | `{ "status": 404, "message": "Tag not found: 99" }` |
| `409` | El tag está asignado a alguna digitalización | `{ "status": 409, "message": "Tag is assigned to at least one digitization and cannot be deleted: 1" }` |

### `POST /api/digitizations/{id}/tags/{tagId}` — Asignar un tag a una digitalización

Controller: `DigitizationController` · Service: `DigitizationService`

Cataloga una digitalización con un tag del conjunto vigente. Si la digitalización ya tiene ese
tag, no se duplica (la operación responde igual). No lleva body.

**Parámetros de URL**

| Parámetro | Tipo | Descripción |
|---|---|---|
| `id` | `Long` | Id de la digitalización |
| `tagId` | `Long` | Id del tag |

**Respuesta `200 OK`**

La digitalización con todos sus tags, ordenados por nombre.

```json
{
  "id": 10,
  "url": "https://files.example/letter-p1.pdf",
  "description": "Scan of page 1",
  "pieceId": 1,
  "tags": [ { "id": 2, "name": "Letters" }, { "id": 1, "name": "Press" } ]
}
```

**Errores**

| Código | Cuándo | Ejemplo |
|---|---|---|
| `404` | La digitalización no existe | `{ "status": 404, "message": "Digitization not found with id: 99" }` |
| `404` | El tag no existe | `{ "status": 404, "message": "Tag not found: 99" }` |

---

## Disponibilización

Controller: `PieceSearchController` · Service: `PieceSearchService`

### `GET /api/pieces/search` — Buscar piezas

Busca piezas combinando filtros opcionales. Todos los filtros se combinan con AND: una pieza
aparece solo si cumple todos los que se indiquen. Sin filtros, devuelve todas las piezas.

**Parámetros de búsqueda**

| Parámetro | Tipo | Descripción |
|---|---|---|
| `name` | `String` | Texto contenido en el nombre (sin distinguir mayúsculas) |
| `description` | `String` | Texto contenido en la descripción (sin distinguir mayúsculas) |
| `dateFrom` | `LocalDate` | Fecha mínima, inclusive |
| `dateTo` | `LocalDate` | Fecha máxima, inclusive |
| `tags` | `Set<String>` | Tags que la pieza debe tener, **todos** (AND). Una pieza tiene un tag si alguna de sus digitalizaciones lo tiene |
| `page` | `int` | Número de página, empieza en `0` (por defecto `0`) |
| `size` | `int` | Resultados por página (por defecto `20`, máximo `50`) |

- Los resultados se ordenan por fecha descendente y, ante empate, por id.
- Una fecha exacta se busca con `dateFrom` y `dateTo` iguales.
- Varios tags se pasan repitiendo el parámetro (`tags=press&tags=letters`) o separados por
  coma (`tags=press,letters`).

**Ejemplo**

```http
GET /api/pieces/search?name=letter&dateFrom=1970-01-01&dateTo=1980-12-31&page=0&size=10
```

**Respuesta `200 OK`**

```json
{
  "content": [
    { "id": 1, "name": "Letter from Cordoba", "description": "Handwritten letter", "date": "1977-05-03" }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 1,
  "totalPages": 1
}
```

**Errores**

| Código | Cuándo | Ejemplo |
|---|---|---|
| `400` | `dateFrom` es posterior a `dateTo` | `{ "status": 400, "message": "Invalid date range: dateFrom 1990-01-01 is after dateTo 1980-01-01" }` |
| `400` | Una fecha no tiene formato ISO (por ejemplo `03/05/1977`) | Respuesta de error estándar de Spring |

> **Pendiente:**
> - Los caracteres `%` y `_` en `name` o `description` no se escapan y funcionan como comodines
>   (por ejemplo, `name=%` devuelve todas las piezas).
