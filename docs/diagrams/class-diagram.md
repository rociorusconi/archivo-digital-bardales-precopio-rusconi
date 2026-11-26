# Class Diagram — Digitization Module

Clases del módulo de Digitalización agrupadas por capa (Controller, Service, Repository, Model),
sumadas a los DTOs y las excepciones que usan. Se incluye `Tag` porque `Digitization` lo referencia.

```mermaid
classDiagram
    direction TB

    namespace controller {
        class PieceController {
            -PieceService pieceService
            +getPiece(Long id) ResponseEntity~PieceDetailResponseDTO~
            +createPiece(PieceCreateRequestDTO request) ResponseEntity~PieceResponseDTO~
            +addDigitization(Long id, DigitizationCreateRequestDTO request) ResponseEntity~DigitizationResponseDTO~
        }
    }

    namespace service {
        class PieceService {
            -PieceRepository pieceRepository
            -DigitizationRepository digitizationRepository
            +getPiece(Long id) PieceDetailResponseDTO
            +createPiece(PieceCreateRequestDTO request) PieceResponseDTO
            +addDigitization(Long pieceId, DigitizationCreateRequestDTO request) DigitizationResponseDTO
            -toResponse(Piece piece) PieceResponseDTO
        }
        class DigitizationMapper {
            <<utility>>
            +toResponse(Digitization digitization)$ DigitizationResponseDTO
        }
    }

    namespace repository {
        class PieceRepository {
            <<interface>>
        }
        class DigitizationRepository {
            <<interface>>
            +findByPieceIdOrderByIdAsc(Long pieceId) List~Digitization~
        }
        class JpaRepository {
            <<interface>>
            +save(entity)
            +findById(id) Optional
        }
    }

    namespace model {
        class Piece {
            <<Entity>>
            -Long id
            -String name
            -String description
            -LocalDate date
        }
        class Digitization {
            <<Entity>>
            -Long id
            -String url
            -String description
            -Piece piece
            -Set~Tag~ tags
            +addTag(Tag tag) void
        }
        class Tag {
            <<Entity>>
            -Long id
            -String name
        }
    }

    namespace dto {
        class PieceCreateRequestDTO {
            <<record>>
            String name
            String description
            LocalDate date
        }
        class PieceResponseDTO {
            <<record>>
            Long id
            String name
            String description
            LocalDate date
        }
        class PieceDetailResponseDTO {
            <<record>>
            Long id
            String name
            String description
            LocalDate date
            List~DigitizationResponseDTO~ digitizations
        }
        class DigitizationCreateRequestDTO {
            <<record>>
            String url
            String description
        }
        class DigitizationResponseDTO {
            <<record>>
            Long id
            String url
            String description
            Long pieceId
            List~TagResponseDTO~ tags
        }
        class ErrorResponseDTO {
            <<record>>
            int status
            String message
        }
    }

    namespace exception {
        class PieceNotFoundException {
            +PieceNotFoundException(Long id)
        }
        class GlobalExceptionHandler {
            <<RestControllerAdvice>>
            +handleNotFound(RuntimeException ex) ResponseEntity~ErrorResponseDTO~
        }
    }

    %% Layer dependencies
    PieceController --> PieceService : uses
    PieceService --> PieceRepository : uses
    PieceService --> DigitizationRepository : uses
    PieceService --> DigitizationMapper : maps with
    PieceRepository --|> JpaRepository : extends
    DigitizationRepository --|> JpaRepository : extends

    %% Domain relationships
    Piece "1" <-- "0..*" Digitization : belongs to
    Digitization "0..*" --> "0..*" Tag : classified with

    %% DTOs and exceptions
    PieceController ..> PieceCreateRequestDTO : receives
    PieceController ..> DigitizationCreateRequestDTO : receives
    PieceService ..> PieceResponseDTO : returns
    PieceService ..> PieceDetailResponseDTO : returns
    PieceService ..> DigitizationResponseDTO : returns
    PieceService ..> PieceNotFoundException : throws
    GlobalExceptionHandler ..> PieceNotFoundException : handles
    GlobalExceptionHandler ..> ErrorResponseDTO : returns
```

## Notas

- **Controller → Service → Repository:** cada capa solo se comunica con la de abajo. El
  controller nunca accede directamente a los repositories.
- **Las entidades no se exponen:** el service convierte `Piece` y `Digitization` en DTOs de
  respuesta (`toResponse`), así la API nunca devuelve entidades JPA. El mapeo de `Digitization`
  vive en un solo lugar, `DigitizationMapper` (capa service), que usan `PieceService` y
  `DigitizationService`.
- **Piece ↔ Digitization:** una digitalización siempre pertenece a una pieza (`@ManyToOne`,
  `piece_id` es obligatorio). Una pieza puede tener cero o muchas digitalizaciones.
- **Digitization ↔ Tag:** relación muchos a muchos (`@ManyToMany`), guardada en la tabla
  intermedia `digitization_tags`. Una digitalización nueva se crea sin tags; los tags se asignan con `addTag`, que no agrega un tag repetido (ver `sequence-assign-tag.md`). La respuesta (`DigitizationResponseDTO`) incluye los tags de la digitalización.
- **Detalle de una pieza:** `getPiece` trae la pieza y después sus digitalizaciones con
  `findByPieceIdOrderByIdAsc`, que usa `@EntityGraph` para cargar los tags en la misma consulta
  (dos consultas en total, sin importar cuántas digitalizaciones tenga la pieza).
- **Errores:** el service lanza `PieceNotFoundException` y `GlobalExceptionHandler` la
  traduce a un HTTP 404 con un `ErrorResponseDTO` en el cuerpo.
