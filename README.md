# Gestión de Recursos en Emergencias — Tier de Negocio (Jakarta EE)

Taller No. 2 — Arquitectura de Software, Pontificia Universidad Javeriana (2026-30).

Tier de negocio de un sistema que registra y atiende solicitudes de recursos durante emergencias. Las reglas de negocio están en un EJB de sesión sin estado (`@Stateless`), que depende de un puerto de almacenamiento (`ResourceRequestRepository`) y no de la implementación concreta en memoria.

## Stack

Java 17 · Jakarta EE 10 · Payara Server 6 · Maven (WAR) · Docker · JAX-RS · JUnit 5 + Mockito

## Estructura

```text
co.edu.javeriana.recursos
├── api            REST: recursos, DTOs y manejadores de errores
├── business
│   ├── contract   ResourceRequestService, comandos y vistas
│   ├── ejb        ResourceRequestServiceBean (@Stateless)
│   ├── exception  Excepciones de negocio (@ApplicationException)
│   └── port       ResourceRequestRepository (puerto de almacenamiento)
├── domain.model   ResourceRequest, Priority, RequestStatus
└── data.memory    InMemoryResourceRequestRepository
```

## Compilar y probar

```bash
mvn clean package
```

## Ejecutar con Docker

```bash
docker build -t recursos-negocio .
docker run --rm -p 8080:8080 -p 4848:4848 --name recursos recursos-negocio
```

API base: `http://localhost:8080/recursos/api`

> Si el puerto 8080 está ocupado, usar otro puerto del host, p. ej. `-p 8081:8080`.

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/requests` | Registrar solicitud |
| GET | `/requests` | Listar todas |
| GET | `/requests/{id}` | Consultar por id |
| GET | `/requests/pending` | Listar pendientes |
| PUT | `/requests/{id}/attend` | Atender solicitud |

## Arquitectura

Ver [`docs/arquitectura.md`](docs/arquitectura.md).
