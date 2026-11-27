# Sequence Diagram — CSV Import (Batch Process)

Flujo de la carga masiva: `POST /api/imports` revisa el archivo, registra la carga y responde `202` enseguida; el proceso sigue en segundo plano, de a bloques, y el cliente consulta el avance con `GET /api/imports/{id}`.

```mermaid
sequenceDiagram
    autonumber
    actor Client
    box Controller layer
        participant IC as ImportController
    end
    box Service layer
        participant IS as ImportService
        participant CSV as CsvImportReader
        participant BP as ImportBatchProcessor
        participant CS as ImportChunkService
        participant PS as PieceService
        participant DS as DigitizationService
    end
    box Model layer
        participant IJ as ImportJob
    end
    box Repository layer
        participant IJR as ImportJobRepository
        participant TR as TagRepository
        participant PR as PieceRepository
    end

    Client->>IC: POST /api/imports (file)
    IC->>IS: startImport(file)
    IS->>CSV: read(content)
    Note over CSV: Revisa columnas y arma las filas.<br/>Archivo inválido → 400
    CSV-->>IS: List<ImportRow>
    IS->>IJ: ImportJob.start(fileName, totalRows)
    IS->>IJR: save(job)
    IS->>BP: process(jobId, rows) (@Async)
    IS-->>IC: ImportJobResponseDTO (IN_PROGRESS)
    IC-->>Client: 202 Accepted + Location

    loop Cada bloque de 100 filas (una transacción por bloque)
        BP->>CS: importChunk(jobId, rows)
        loop Cada fila
            CS->>CS: validar con las anotaciones de los DTOs
            CS->>TR: findByNameIgnoreCase(tag)
            alt Fila válida
                CS->>PR: findFirstByNameIgnoreCaseAndDate(name, date)
                opt La pieza no existe
                    CS->>PS: createPiece(...)
                end
                CS->>PS: addDigitization(pieceId, ...)
                CS->>DS: addTag(digitizationId, tagId)
                CS->>IJ: recordImported(createdPiece)
            else Fila inválida (tag inexistente, URL inválida, fecha mal escrita, url repetida...)
                CS->>IJ: recordError(line, message)
            end
        end
        CS->>IJR: save(job)
    end
    BP->>CS: complete(jobId)
    CS->>IJ: complete()

    Client->>IC: GET /api/imports/{id}
    IC->>IS: getImport(id)
    IS->>IJR: findById(id)
    IS-->>IC: ImportJobResponseDTO (COMPLETED, contadores y errores)
    IC-->>Client: 200 OK
```

## Notas

- **En segundo plano:** `ImportBatchProcessor.process` es `@Async`, así que `POST` no espera a que se procesen todas las filas. `startImport` no es transaccional a propósito: la carga se guarda antes de arrancar el proceso, para que el proceso la encuentre.
- **Un bloque, una transacción:** con muchas filas no queda todo en una sola transacción, y el avance (`processedRows`) se ve mientras corre.
- **Mismas reglas que la API:** cada fila se valida con las anotaciones de `PieceCreateRequestDTO` y `DigitizationCreateRequestDTO`, y se carga con los mismos services (`PieceService`, `DigitizationService`). Los tags no son libres: un tag que no está en el catálogo rechaza la fila.
- **Primero validar, después crear:** una fila se valida completa antes de crear nada. Si un service lanzara una excepción a mitad de la fila, marcaría todo el bloque para deshacerse.
- **Errores inesperados** (por ejemplo, la base no responde): la carga queda `FAILED` con el motivo en `failureMessage`; los bloques ya guardados se conservan.
