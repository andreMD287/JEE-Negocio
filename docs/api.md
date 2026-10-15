# API REST — Gestión de Recursos

Base: `http://localhost:8080/recursos/api` (si el 8080 del host está ocupado, se publica en otro puerto, p. ej. `-p 8081:8080`).

Todas las peticiones y respuestas usan `application/json` en UTF-8. La API solo traduce HTTP a llamadas a `ResourceRequestService`; **todas las reglas se validan en el EJB**.

## Endpoints

| Método | Ruta | Descripción | Éxito |
|---|---|---|---|
| POST | `/requests` | Registrar una solicitud | 201 + header `Location` |
| GET | `/requests` | Consultar todas | 200 |
| GET | `/requests/{id}` | Consultar por id | 200 |
| GET | `/requests/pending` | Consultar solo las pendientes | 200 |
| PUT | `/requests/{id}/attend` | Atender (PENDIENTE → ATENDIDA) | 200 |

### Registrar

```http
POST /recursos/api/requests
Content-Type: application/json

{
  "disasterDescription": "Inundación en el sector norte",
  "resourceType": "Agua potable",
  "quantity": 200,
  "priority": "ALTA"
}
```

El cliente no envía `id` ni `status`: el sistema asigna el id y el estado `PENDIENTE`. `priority` acepta `BAJA`, `MEDIA` o `ALTA`.

```http
HTTP/1.1 201 Created
Location: http://localhost:8080/recursos/api/requests/1

{
  "id": 1,
  "disasterDescription": "Inundación en el sector norte",
  "resourceType": "Agua potable",
  "quantity": 200,
  "priority": "ALTA",
  "status": "PENDIENTE"
}
```

## Errores

Todos los errores tienen el mismo formato:

```json
{
  "code": "REQUEST_NOT_FOUND",
  "message": "No existe la solicitud con id 10",
  "timestamp": "2026-10-14T20:52:51.456Z"
}
```

| HTTP | `code` | Cuándo | Origen |
|---|---|---|---|
| 400 | `VALIDATION_ERROR` | Cantidad ≤ 0 o ausente, descripción o tipo de recurso vacíos, prioridad ausente o inválida | `BusinessValidationException` (EJB) |
| 404 | `REQUEST_NOT_FOUND` | Consultar o atender un id que no existe | `RequestNotFoundException` (EJB) |
| 409 | `ALREADY_ATTENDED` | Atender una solicitud que ya está ATENDIDA | `AlreadyAttendedException` (EJB) |
| 400 | `EMPTY_BODY` | POST sin cuerpo | JAX-RS |
| 400 | `MALFORMED_JSON` | JSON mal formado o con tipos incorrectos (p. ej. `"quantity": "muchos"`) | JSON-B |
| 404 / 405 / 415 | `NOT_FOUND`, `METHOD_NOT_ALLOWED`, `UNSUPPORTED_MEDIA_TYPE` | Ruta, método o tipo de contenido no soportados | JAX-RS |
| 500 | `INTERNAL_ERROR` | Cualquier error no controlado (no expone detalles internos) | — |

Cada excepción tiene su `ExceptionMapper` en `api.exception`. Las excepciones de negocio llevan `@ApplicationException`, así que el contenedor EJB no las envuelve en `EJBException` y llegan intactas al mapper correspondiente.

## Evidencias con Postman

La colección [`evidencias/recursos.postman_collection.json`](evidencias/recursos.postman_collection.json) tiene 21 peticiones en tres carpetas: operaciones, reglas de negocio y errores de entrada. Cada una verifica su código HTTP y el contenido de la respuesta (61 verificaciones en total).

1. Levantar la aplicación (`docker run ...`, ver README).
2. En Postman: **Import** → seleccionar el archivo de la colección.
3. Si el puerto no es 8080, cambiar la variable de colección `baseUrl`.
4. Abrir la colección → **Run** → ejecutar todas las peticiones **en orden**, porque las de la carpeta 2 usan los ids creados en la carpeta 1.
5. Capturar el resumen del Runner y, si se quiere, el detalle de cada petición.

Por línea de comandos (útil para adjuntar la salida):

```bash
npx newman run docs/evidencias/recursos.postman_collection.json \
  --env-var "baseUrl=http://localhost:8080/recursos/api"
```

> Con `curl` en la terminal de Windows las tildes escritas en el comando llegan mal codificadas; el servidor sí maneja bien el UTF-8. Para las capturas es mejor usar Postman, o `curl --data-binary @archivo.json`.
