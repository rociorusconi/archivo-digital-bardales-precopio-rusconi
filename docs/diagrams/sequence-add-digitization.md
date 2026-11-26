# Sequence Diagram — Add Digitization to a Piece

Flujo de `POST /api/pieces/{id}/digitizations`: registra una digitalización (archivo digital)
para una pieza existente. Incluye el camino de error cuando la pieza no existe.

```mermaid
sequenceDiagram
    autonumber
    actor Client
    box Controller layer
        participant PC as PieceController
        participant GEH as GlobalExceptionHandler
    end
    box Service layer
        participant PS as PieceService
    end
    box Repository layer
        participant PR as PieceRepository
        participant DR as DigitizationRepository
    end
    participant DB as H2 Database

    Client->>PC: POST /api/pieces/{id}/digitizations<br/>{ url, description }
    PC->>PS: addDigitization(id, DigitizationCreateRequestDTO)
    PS->>PR: findById(id)
    PR->>DB: SELECT FROM pieces WHERE id = ?
    DB-->>PR: row / no row
    PR-->>PS: Optional (Piece or empty)

    alt Piece not found
        PS-->>PC: throws PieceNotFoundException(id)
        PC-->>GEH: exception propagates
        GEH->>GEH: handleNotFound(ex)
        GEH-->>Client: 404 Not Found<br/>{ status: 404, message: "Piece not found with id: {id}" }
    else Piece found
        PS->>PS: new Digitization(null, url, description, piece, empty tags)
        PS->>DR: save(digitization)
        DR->>DB: INSERT INTO digitizations
        DB-->>DR: generated id
        DR-->>PS: saved Digitization (with id)
        PS->>PS: toResponse(saved)
        PS-->>PC: DigitizationResponseDTO
        PC-->>Client: 201 Created<br/>{ id, url, description, pieceId, tags: [] }
    end
```

## Notas

- Toda la operación corre dentro de una transacción (`@Transactional` en `addDigitization`).
- El service verifica que la pieza exista antes de crear la digitalización, así nunca queda
  una digitalización sin pieza.
- La digitalización nueva arranca sin tags. La asignación de tags es un flujo aparte.
- El controller no atrapa `PieceNotFoundException`: Spring se la pasa a
  `GlobalExceptionHandler`, que arma la respuesta 404.

## Ejemplo

Pedido:

```http
POST /api/pieces/1/digitizations
Content-Type: application/json

{ "url": "https://files.example/letter-p1.pdf", "description": "Scan of page 1" }
```

Respuesta `201 Created`:

```json
{ "id": 10, "url": "https://files.example/letter-p1.pdf", "description": "Scan of page 1", "pieceId": 1, "tags": [] }
```

Respuesta cuando la pieza `99` no existe, `404 Not Found`:

```json
{ "status": 404, "message": "Piece not found with id: 99" }
```
