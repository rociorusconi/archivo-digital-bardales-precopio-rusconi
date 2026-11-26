# Sequence Diagram — Search from the Telegram Bot

Flujo de `/buscar` desde Telegram: el usuario busca piezas y el bot responde con la lista. El bot es un cliente más de la API REST: llama por HTTP al mismo endpoint que cualquier cliente (`GET /api/pieces/search`), que a su vez sigue las capas controller → service → repository.

```mermaid
sequenceDiagram
    autonumber
    actor User as Usuario final
    participant TG as Telegram (Bot API)
    box Bot (cliente de la API)
        participant TB as TelegramBot (adapter)
        participant BCH as BotCommandHandler
        participant AAC as ArchiveApiClient
    end
    box Controller layer
        participant PSC as PieceSearchController
    end
    box Service layer
        participant PSS as PieceSearchService
    end
    box Repository layer
        participant PR as PieceRepository
    end
    participant DB as H2 Database

    User->>TG: /buscar carta tag: prensa
    TB->>TG: getUpdates (long polling)
    TG-->>TB: Update con el mensaje
    TB->>BCH: handle("/buscar carta tag: prensa")
    BCH->>BCH: parseFilters → name "carta", tags {prensa}, página 1
    BCH->>AAC: searchPieces(PieceSearchRequestDTO, page 0, size 10)
    AAC->>PSC: HTTP GET /api/pieces/search?name=carta&tags=prensa&page=0&size=10
    PSC->>PSS: search(PieceSearchRequestDTO, 0, 10)
    PSS->>PSS: new PieceSearchCriteria(...) (valida el rango de fechas)
    PSS->>PR: search(..., Pageable)
    PR->>DB: SELECT ... LIMIT 10 + SELECT COUNT(*)
    DB-->>PR: piezas de la página + total
    PR-->>PSS: Page<Piece>
    PSS-->>PSC: PageResponseDTO<PieceResponseDTO>
    PSC-->>AAC: 200 OK (JSON)
    AAC-->>BCH: PageResponseDTO<PieceResponseDTO>
    BCH->>BCH: formatResults → texto
    BCH-->>TB: texto de la respuesta
    TB->>TG: sendMessage(chatId, texto)
    TG-->>User: "Encontré 1 pieza(s): #1 · Carta desde Cordoba ..."
```

## Notas

- **Long polling:** `TelegramBot` le pide los mensajes nuevos a Telegram (`getUpdates`). Telegram nunca llama a la aplicación, así que funciona en local sin exponerla a internet. En un servidor desplegado se puede usar **webhook**: Telegram envía el `Update` a `TelegramWebhookController` (`POST /telegram/webhook`), que se lo pasa a `TelegramBot`; desde ahí el flujo es el mismo.
- **Adapter:** solo `TelegramBot` conoce Telegram. `BotCommandHandler` recibe y devuelve texto, así que otra plataforma (por ejemplo WhatsApp) solo necesitaría otro adapter.
- **Por HTTP, a los controllers:** el bot no llama a los services. Pasa por el controller como cualquier cliente, así recibe las mismas validaciones y el mismo formato de error.
- **Errores:** si la fecha está mal escrita, `BotCommandHandler` responde cómo escribirla sin llamar a la API. Si la API responde `400` (por ejemplo, rango de fechas invertido), `ArchiveApiClient` lanza `ArchiveApiException` con el mensaje y el bot lo muestra. Si la API no responde, el bot avisa que no pudo consultar el archivo.
- `/pieza <id>` sigue el mismo camino con `GET /api/pieces/{id}` (`PieceController` → `PieceService.getPiece`); un `404` se responde como "No existe la pieza".
