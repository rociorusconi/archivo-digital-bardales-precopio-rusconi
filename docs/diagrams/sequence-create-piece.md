# Sequence Diagram — Create Piece

Flujo de `POST /api/pieces`: registra una nueva pieza física en el archivo.

```mermaid
sequenceDiagram
    autonumber
    actor Client
    box Controller layer
        participant PC as PieceController
    end
    box Service layer
        participant PS as PieceService
    end
    box Repository layer
        participant PR as PieceRepository
    end
    participant DB as H2 Database

    Client->>PC: POST /api/pieces<br/>{ name, description, date }
    PC->>PS: createPiece(PieceCreateRequestDTO)
    PS->>PS: new Piece(null, name, description, date)
    PS->>PR: save(piece)
    PR->>DB: INSERT INTO pieces
    DB-->>PR: generated id
    PR-->>PS: saved Piece (with id)
    PS->>PS: toResponse(saved)
    PS-->>PC: PieceResponseDTO
    PC-->>Client: 201 Created<br/>{ id, name, description, date }
```

## Notas

- Toda la operación corre dentro de una transacción (`@Transactional` en `createPiece`).
- El id es `null` al construir la `Piece`; la base de datos lo genera al hacer `save`
  (`GenerationType.IDENTITY`).
- La respuesta es un `PieceResponseDTO`, nunca la entidad `Piece`.

## Ejemplo

Pedido:

```http
POST /api/pieces
Content-Type: application/json

{ "name": "Letter from Cordoba", "description": "Handwritten letter", "date": "1977-05-03" }
```

Respuesta `201 Created`:

```json
{ "id": 1, "name": "Letter from Cordoba", "description": "Handwritten letter", "date": "1977-05-03" }
```
