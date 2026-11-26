# Sequence Diagram — Assign Tag to a Digitization

Flujo de `POST /api/digitizations/{id}/tags/{tagId}`: cataloga una digitalización con un tag
del conjunto vigente. Incluye los caminos de error cuando la digitalización o el tag no existen.

```mermaid
sequenceDiagram
    autonumber
    actor Client
    box Controller layer
        participant DC as DigitizationController
        participant GEH as GlobalExceptionHandler
    end
    box Service layer
        participant DS as DigitizationService
    end
    box Model layer
        participant D as Digitization
    end
    box Repository layer
        participant DR as DigitizationRepository
        participant TR as TagRepository
    end
    participant DB as H2 Database

    Client->>DC: POST /api/digitizations/{id}/tags/{tagId}
    DC->>DS: addTag(id, tagId)
    DS->>DR: findById(id)
    DR->>DB: SELECT FROM digitizations WHERE id = ?
    DB-->>DR: row / no row
    DR-->>DS: Optional (Digitization or empty)

    alt Digitization not found
        DS-->>DC: throws DigitizationNotFoundException(id)
        DC-->>GEH: exception propagates
        GEH-->>Client: 404 Not Found<br/>{ status: 404, message: "Digitization not found with id: {id}" }
    else Digitization found
        DS->>TR: findById(tagId)
        TR->>DB: SELECT FROM tags WHERE id = ?
        DB-->>TR: row / no row
        TR-->>DS: Optional (Tag or empty)

        alt Tag not found
            DS-->>DC: throws TagNotFoundException(tagId)
            DC-->>GEH: exception propagates
            GEH-->>Client: 404 Not Found<br/>{ status: 404, message: "Tag not found: {tagId}" }
        else Tag found
            DS->>D: addTag(tag)
            D->>D: add only if it does not have it yet
            DS->>DR: save(digitization)
            DR->>DB: INSERT INTO digitization_tags
            DS->>DS: toResponse(saved)
            DS-->>DC: DigitizationResponseDTO
            DC-->>Client: 200 OK<br/>{ id, url, description, pieceId, tags }
        end
    end
```

## Notas

- Toda la operación corre dentro de una transacción (`@Transactional` en `addTag`).
- La regla "un tag no se repite" vive en la entidad (`Digitization.addTag`), no en el service:
  el service busca, delega la regla y guarda.
- Si alguno de los dos no existe, no se guarda nada. El controller no atrapa las excepciones:
  `GlobalExceptionHandler` arma la respuesta 404.
- Si el tag ya estaba asignado, no se inserta otra fila en `digitization_tags` y la respuesta es
  la misma.

## Ejemplo

Pedido:

```http
POST /api/digitizations/10/tags/1
```

Respuesta `200 OK`:

```json
{
  "id": 10,
  "url": "https://files.example/letter-p1.pdf",
  "description": "Scan of page 1",
  "pieceId": 1,
  "tags": [ { "id": 1, "name": "Press" } ]
}
```

Respuesta cuando el tag `99` no existe, `404 Not Found`:

```json
{ "status": 404, "message": "Tag not found: 99" }
```
