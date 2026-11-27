# Prueba del sistema — Archivo Digital

Secuencia para probar el camino feliz de punta a punta: crear un tag, crear una pieza, agregarle
una digitalización, catalogarla con tags y encontrarla con la búsqueda. Al final hay casos de
error para comprobar las validaciones y, por último, la misma búsqueda desde el bot de Telegram.

Los endpoints están especificados en [`api.md`](api.md).

## Preparación

1. Levantar la aplicación en el perfil `local` (el que viene por defecto):

   ```bash
   ./mvnw spring-boot:run
   ```

2. Al arrancar se cargan los datos semilla de `src/main/resources/data.sql`: 6 tags, 6 piezas y
   8 digitalizaciones. Por eso los ids de esta guía empiezan en 7 (tags y piezas) y en 9
   (digitalizaciones).

La base es H2 en memoria: cada vez que se reinicia la aplicación se vuelve a los datos semilla, así
que la secuencia se puede repetir desde cero reiniciando la aplicación. Los ids de esta guía
suponen que se ejecuta una sola vez, en orden, después de arrancar.

Los pedidos se pueden hacer de dos formas:

- **Swagger UI** (`http://localhost:8080/swagger-ui.html`): abrir el endpoint, tocar
  **Try it out**, completar los parámetros o el body y tocar **Execute**.
- **curl** desde Git Bash, con los comandos de cada paso.

## Camino feliz

| Paso | Módulo | Pedido | Respuesta esperada |
|---|---|---|---|
| 1 | Catalogación | `GET /api/tags` | `200`, los 6 tags semilla |
| 2 | Catalogación | `POST /api/tags` | `201`, tag `Prensa local` con id 7 |
| 3 | Digitalización | `POST /api/pieces` | `201`, pieza con id 7 |
| 4 | Digitalización | `POST /api/pieces/7/digitizations` | `201`, digitalización con id 9 y sin tags |
| 5 | Catalogación | `POST /api/digitizations/9/tags/7` y `/tags/1` | `200`, la digitalización con los tags `Cartas` y `Prensa local` |
| 6 | Digitalización | `GET /api/pieces/7` | `200`, la pieza con su digitalización y sus tags |
| 7 | Disponibilización | `GET /api/pieces/search?...` | `200`, la pieza nueva aparece en las búsquedas |

### 1. Ver los tags existentes

```bash
curl http://localhost:8080/api/tags
```

Respuesta `200 OK` (el orden puede variar):

```json
[
  { "id": 5, "name": "Afiches" },
  { "id": 1, "name": "Cartas" },
  { "id": 2, "name": "Fotografia" },
  { "id": 6, "name": "Manuscrito" },
  { "id": 3, "name": "Prensa" },
  { "id": 4, "name": "Testimonio" }
]
```

### 2. Crear un tag

El administrador agrega un tag al conjunto vigente.

```bash
curl -X POST http://localhost:8080/api/tags \
  -H "Content-Type: application/json" \
  -d '{"name": "Prensa local"}'
```

Respuesta `201 Created`:

```json
{ "id": 7, "name": "Prensa local" }
```

### 3. Crear una pieza

```bash
curl -X POST http://localhost:8080/api/pieces \
  -H "Content-Type: application/json" \
  -d '{"name": "Carta desde Rosario", "description": "Carta manuscrita enviada desde Rosario", "date": "1979-02-11"}'
```

Respuesta `201 Created`:

```json
{ "id": 7, "name": "Carta desde Rosario", "description": "Carta manuscrita enviada desde Rosario", "date": "1979-02-11" }
```

### 4. Agregar una digitalización a la pieza

```bash
curl -X POST http://localhost:8080/api/pieces/7/digitizations \
  -H "Content-Type: application/json" \
  -d '{"url": "https://archivo.example/carta-rosario-p1.pdf", "description": "Escaneo de la pagina 1"}'
```

Respuesta `201 Created` (la digitalización se crea sin tags):

```json
{ "id": 9, "url": "https://archivo.example/carta-rosario-p1.pdf", "description": "Escaneo de la pagina 1", "pieceId": 7, "tags": [] }
```

### 5. Asignar tags a la digitalización

Se asignan el tag nuevo (`Prensa local`, id 7) y uno de los semilla (`Cartas`, id 1). No llevan body.

```bash
curl -X POST http://localhost:8080/api/digitizations/9/tags/7
curl -X POST http://localhost:8080/api/digitizations/9/tags/1
```

Respuesta `200 OK` del segundo pedido (los tags se ordenan por nombre):

```json
{
  "id": 9,
  "url": "https://archivo.example/carta-rosario-p1.pdf",
  "description": "Escaneo de la pagina 1",
  "pieceId": 7,
  "tags": [ { "id": 1, "name": "Cartas" }, { "id": 7, "name": "Prensa local" } ]
}
```

### 6. Ver el detalle de la pieza

```bash
curl http://localhost:8080/api/pieces/7
```

Respuesta `200 OK`:

```json
{
  "id": 7,
  "name": "Carta desde Rosario",
  "description": "Carta manuscrita enviada desde Rosario",
  "date": "1979-02-11",
  "digitizations": [
    {
      "id": 9,
      "url": "https://archivo.example/carta-rosario-p1.pdf",
      "description": "Escaneo de la pagina 1",
      "pieceId": 7,
      "tags": [ { "id": 1, "name": "Cartas" }, { "id": 7, "name": "Prensa local" } ]
    }
  ]
}
```

### 7. Buscar la pieza

**Por tag.** La búsqueda devuelve piezas: la pieza 7 aparece porque una de sus digitalizaciones
tiene el tag.

```bash
curl "http://localhost:8080/api/pieces/search?tags=Prensa%20local"
```

```json
{
  "content": [
    { "id": 7, "name": "Carta desde Rosario", "description": "Carta manuscrita enviada desde Rosario", "date": "1979-02-11" }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

**Por nombre y tag combinados (AND).** Aparecen la pieza nueva y la carta semilla, ordenadas por
fecha descendente.

```bash
curl "http://localhost:8080/api/pieces/search?name=carta&tags=Cartas"
```

```json
{
  "content": [
    { "id": 7, "name": "Carta desde Rosario", "description": "Carta manuscrita enviada desde Rosario", "date": "1979-02-11" },
    { "id": 1, "name": "Carta desde Cordoba", "description": "Carta manuscrita enviada a la familia", "date": "1977-05-03" }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 2,
  "totalPages": 1
}
```

**Por rango de fechas (inclusivo).**

```bash
curl "http://localhost:8080/api/pieces/search?dateFrom=1979-01-01&dateTo=1979-12-31"
```

Responde solo con la pieza 7 (`"totalElements": 1`).

## Casos de error

Continúan después del camino feliz (usan el tag 7 y la digitalización 9). Todos responden con
el formato de error descripto en [`api.md`](api.md#información-general).

| Caso | Pedido | Respuesta esperada |
|---|---|---|
| Tag duplicado (sin distinguir mayúsculas) | `POST /api/tags` con `{"name": "prensa local"}` | `409` · `Tag already exists: prensa local` |
| Eliminar un tag en uso | `DELETE /api/tags/7` | `409` · `Tag is assigned to at least one digitization and cannot be deleted: 7` |
| Pieza sin nombre | `POST /api/pieces` con `{"name": ""}` | `400` · `name: must not be blank` |
| Digitalización de una pieza inexistente | `POST /api/pieces/99/digitizations` | `404` · `Piece not found with id: 99` |
| Asignar un tag inexistente | `POST /api/digitizations/9/tags/99` | `404` · `Tag not found: 99` |
| Rango de fechas invertido | `GET /api/pieces/search?dateFrom=1990-01-01&dateTo=1980-01-01` | `400` · `Invalid date range: dateFrom 1990-01-01 is after dateTo 1980-01-01` |

Para cerrar, se quita el tag de la digitalización y después se puede eliminar:

| Pedido | Respuesta esperada |
|---|---|
| `DELETE /api/digitizations/9/tags/7` | `200`, la digitalización queda solo con `Cartas` |
| `DELETE /api/tags/7` | `204 No Content` |

## Visualización: la búsqueda desde el bot

Con el bot configurado (ver [`bot.md`](bot.md#cómo-levantarlo)) y la aplicación recién levantada:

| Mensaje al bot | Respuesta esperada |
|---|---|
| `/tags` | `Tags (6): Afiches, Cartas, Fotografia, Manuscrito, Prensa, Testimonio` |
| `/buscar carta` | #1 Carta desde Cordoba |
| `/pieza 1` | La carta con sus 2 digitalizaciones, links y tags |

Si antes se hicieron los pasos 1 a 5 del camino feliz, `/buscar tag: Prensa local` encuentra la
pieza #7 creada por la API. Así se comprueba que el bot consulta los mismos datos que la API. El
resto de los casos del bot están en [`bot.md`](bot.md#cómo-probarlo).

## Tests automáticos

```bash
./mvnw test
```

Corre los tests unitarios de los services y del bot (JUnit 5 + Mockito, sin levantar Spring) y el
smoke test `TpApplicationTests`, que comprueba que la aplicación arranca.
