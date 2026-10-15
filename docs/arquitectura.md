# Arquitectura — Gestión de Recursos en Emergencias

Documentación con el modelo de vistas 4+1. Las vistas se reparten entre los dos integrantes; lo que comparten son las **interfaces** (`ResourceRequestService` y `ResourceRequestRepository`), que ambas vistas deben respetar.

| Vista | Responsable | Contenido |
|---|---|---|
| Desarrollo | Rol 1 | Paquetes y capas, dependencias entre módulos, cómo el puerto desacopla negocio y datos, empaquetado WAR |
| Lógica | Rol 2 | Clases del dominio, contratos, EJB, excepciones |
| Procesos | Rol 2 | Secuencias de registrar, consultar, atender (incluye errores 400/404/409) |
| Física / despliegue | Rol 1 | Contenedor Docker → Payara 6 → WAR |
| Escenarios | Rol 2 | Casos de uso que recorren las vistas anteriores |

## Interfaces compartidas

```java
@Local
public interface ResourceRequestService {
    ResourceRequestView register(CreateResourceRequestCommand command);
    List<ResourceRequestView> findAll();
    ResourceRequestView findById(Long id);
    ResourceRequestView attend(Long id);
    List<ResourceRequestView> findPending();
}

public interface ResourceRequestRepository {
    ResourceRequest save(ResourceRequest request);
    Optional<ResourceRequest> findById(Long id);
    List<ResourceRequest> findAll();
}
```

## Vista de desarrollo

Muestra cómo está organizado el código: módulos (paquetes), capas y la dirección de las dependencias entre ellos.

### Paquetes y dependencias

```mermaid
flowchart TB
    subgraph WAR["recursos.war"]
        direction TB
        API["<b>api</b><br/>RestApplication<br/>resource · request · response · exception"]

        subgraph BUS["business"]
            direction TB
            CONTRACT["<b>contract</b><br/>«interface» ResourceRequestService<br/>CreateResourceRequestCommand<br/>ResourceRequestView"]
            EJB["<b>ejb</b><br/>ResourceRequestServiceBean<br/>@Stateless"]
            EXC["<b>exception</b><br/>BusinessValidationException<br/>RequestNotFoundException<br/>AlreadyAttendedException"]
            PORT["<b>port</b><br/>«interface» ResourceRequestRepository"]
        end

        DOMAIN["<b>domain.model</b><br/>ResourceRequest<br/>Priority · RequestStatus"]
        DATA["<b>data.memory</b><br/>InMemoryResourceRequestRepository<br/>@ApplicationScoped"]
    end

    API -->|usa| CONTRACT
    API -->|traduce a HTTP| EXC
    EJB -.->|implementa| CONTRACT
    EJB -->|lanza| EXC
    EJB -->|"@Inject"| PORT
    DATA -.->|implementa| PORT
    EJB --> DOMAIN
    PORT --> DOMAIN
    DATA --> DOMAIN
    CONTRACT --> DOMAIN
```

| Paquete | Capa | Responsabilidad | Puede depender de |
|---|---|---|---|
| `api` | Presentación (REST) | Traducir HTTP ↔ llamadas al servicio y excepciones ↔ códigos HTTP | `business.contract`, `business.exception` |
| `business.contract` | Negocio | Interfaz pública del tier de negocio y sus DTOs | `domain.model` |
| `business.ejb` | Negocio | Reglas y validaciones (EJB `@Stateless`) | `contract`, `exception`, `port`, `domain.model` |
| `business.exception` | Negocio | Errores de negocio (`@ApplicationException`) | — |
| `business.port` | Negocio | Contrato de almacenamiento que el negocio necesita | `domain.model` |
| `domain.model` | Dominio | Entidad y enums, sin dependencias de frameworks | — |
| `data.memory` | Datos | Almacenamiento en memoria que implementa el puerto | `business.port`, `domain.model` |

### Cómo se desacopla el negocio de los datos

1. **El puerto pertenece al negocio.** `ResourceRequestRepository` está en `business.port`, no en `data`. El negocio define qué necesita (guardar, buscar por id, listar) sin saber cómo se hace.
2. **Las dependencias apuntan hacia el negocio (inversión de dependencias).** `data.memory` depende de `business.port`, y no al revés. Ninguna clase de `business` importa algo de `data`.
3. **CDI conecta las piezas en tiempo de ejecución.** El EJB declara `@Inject ResourceRequestRepository repository;` y el contenedor le entrega `InMemoryResourceRequestRepository`, la única implementación disponible. El EJB nunca la nombra.
4. **El repositorio no tiene reglas de negocio.** No valida cantidades ni estados y no filtra pendientes. Toda la lógica está en el EJB, así que cambiar el almacenamiento no cambia las reglas.
5. **Copias defensivas.** El repositorio guarda y devuelve copias, así que un cambio solo queda persistido cuando el EJB llama explícitamente a `save()`, igual que pasaría con una base de datos.

**Consecuencia:** reemplazar la memoria por JPA/PostgreSQL implica crear, por ejemplo, `data.jpa.JpaResourceRequestRepository implements ResourceRequestRepository` y retirar la implementación en memoria. `business`, `api` y `domain` no cambian. Las pruebas del EJB lo demuestran usando un mock de Mockito en lugar del repositorio real.

### Concurrencia del almacenamiento en memoria

- `@ApplicationScoped`: una sola instancia compartida por todas las instancias del pool de EJB `@Stateless`.
- `ConcurrentHashMap` para el almacenamiento y `AtomicLong` para generar ids sin repetir.
- Probado en `InMemoryResourceRequestRepositoryTest.concurrentSavesAreSafe` (8 hilos × 250 inserciones, sin ids repetidos ni pérdidas).
- Limitación conocida: dos llamadas simultáneas a `attend` sobre la misma solicitud podrían pasar ambas la verificación de `PENDIENTE`. Para este taller se acepta; en producción se resolvería con bloqueo optimista (versión) en la persistencia.

### Construcción y empaquetado

| Elemento | Detalle |
|---|---|
| Build | Maven, `maven.compiler.release=17` |
| API | `jakarta.jakartaee-api:10.0.0` (`provided`, la aporta Payara) |
| Pruebas | JUnit 5 + Mockito (`mvn test`) |
| Artefacto | `target/recursos.war` (contexto `/recursos`, REST en `/recursos/api`) |
| Imagen | `Dockerfile` multi-etapa: `maven:3.9-eclipse-temurin-17` compila → `payara/server-full:6.2024.6-jdk17` despliega |

```text
src/
├── main/java/co/edu/javeriana/recursos/   (paquetes de la tabla anterior)
├── main/webapp/WEB-INF/beans.xml          (CDI, bean-discovery-mode="annotated")
└── test/java/co/edu/javeriana/recursos/   (pruebas por paquete)
```

## Vista lógica

La vista lógica presenta las clases principales y sus relaciones sin considerar detalles de despliegue.

```mermaid
classDiagram
    direction TB

    class ResourceRequestService {
        <<interface>>
        +register(command) ResourceRequestView
        +findAll() List
        +findById(id) ResourceRequestView
        +attend(id) ResourceRequestView
        +findPending() List
    }

    class ResourceRequestServiceBean {
        <<Stateless>>
        +register(command) ResourceRequestView
        +findAll() List
        +findById(id) ResourceRequestView
        +attend(id) ResourceRequestView
        +findPending() List
    }

    class ResourceRequestRepository {
        <<interface>>
        +save(request) ResourceRequest
        +findById(id) Optional
        +findAll() List
    }

    class ResourceRequest {
        -Long id
        -String disasterDescription
        -String resourceType
        -int quantity
        -Priority priority
        -RequestStatus status
    }

    class CreateResourceRequestCommand {
        +String disasterDescription
        +String resourceType
        +Integer quantity
        +String priority
    }

    class ResourceRequestView {
        +Long id
        +String disasterDescription
        +String resourceType
        +int quantity
        +Priority priority
        +RequestStatus status
    }

    class Priority {
        <<enumeration>>
        BAJA
        MEDIA
        ALTA
    }

    class RequestStatus {
        <<enumeration>>
        PENDIENTE
        ATENDIDA
    }

    ResourceRequestService <|.. ResourceRequestServiceBean
    ResourceRequestServiceBean --> ResourceRequestRepository
    ResourceRequestServiceBean --> CreateResourceRequestCommand
    ResourceRequestServiceBean --> ResourceRequestView
    ResourceRequestRepository --> ResourceRequest
    ResourceRequest --> Priority
    ResourceRequest --> RequestStatus
```

El cliente del negocio depende de `ResourceRequestService`, mientras que el EJB depende de `ResourceRequestRepository`. Ninguna operación del EJB conoce la implementación concreta del almacenamiento.

Las excepciones expresan los resultados inválidos del negocio:

| Excepción | Situación | Respuesta HTTP |
|---|---|---:|
| `BusinessValidationException` | Datos obligatorios ausentes o inválidos | 400 |
| `RequestNotFoundException` | La solicitud consultada no existe | 404 |
| `AlreadyAttendedException` | La solicitud ya estaba atendida | 409 |

## Vista de procesos

### Registrar una solicitud

```mermaid
sequenceDiagram
    participant C as Cliente
    participant A as API REST
    participant E as EJB
    participant R as Repositorio

    C->>A: POST /requests
    A->>E: register(command)

    alt Datos inválidos
        E-->>A: BusinessValidationException
        A-->>C: HTTP 400
    else Datos válidos
        E->>E: Validar y asignar PENDIENTE
        E->>R: save(request)
        R-->>E: Solicitud guardada
        E-->>A: ResourceRequestView
        A-->>C: HTTP 201
    end
```

La identificación y el estado no se reciben desde REST. El repositorio genera el identificador y el EJB asigna siempre `PENDIENTE`.

### Consultar una solicitud

```mermaid
sequenceDiagram
    participant C as Cliente
    participant A as API REST
    participant E as EJB
    participant R as Repositorio

    C->>A: GET /requests/{id}
    A->>E: findById(id)
    E->>R: findById(id)

    alt Solicitud existente
        R-->>E: ResourceRequest
        E-->>A: ResourceRequestView
        A-->>C: HTTP 200
    else Solicitud inexistente
        R-->>E: Optional vacío
        E-->>A: RequestNotFoundException
        A-->>C: HTTP 404
    end
```

### Atender una solicitud

```mermaid
sequenceDiagram
    participant C as Cliente
    participant A as API REST
    participant E as EJB
    participant R as Repositorio

    C->>A: PUT /requests/{id}/attend
    A->>E: attend(id)
    E->>R: findById(id)

    alt Solicitud inexistente
        R-->>E: Optional vacío
        E-->>A: RequestNotFoundException
        A-->>C: HTTP 404
    else Ya está ATENDIDA
        R-->>E: Solicitud ATENDIDA
        E-->>A: AlreadyAttendedException
        A-->>C: HTTP 409
    else Está PENDIENTE
        R-->>E: Solicitud PENDIENTE
        E->>E: Cambiar estado a ATENDIDA
        E->>R: save(request)
        R-->>E: Solicitud actualizada
        E-->>A: ResourceRequestView
        A-->>C: HTTP 200
    end
```

El EJB llama nuevamente a `save()` porque el repositorio trabaja con copias de los objetos almacenados.

### Listar solicitudes pendientes

```mermaid
sequenceDiagram
    participant C as Cliente
    participant A as API REST
    participant E as EJB
    participant R as Repositorio

    C->>A: GET /requests/pending
    A->>E: findPending()
    E->>R: findAll()
    R-->>E: Todas las solicitudes
    E->>E: Filtrar estado PENDIENTE
    E-->>A: Lista de vistas
    A-->>C: HTTP 200
```

El filtrado se realiza en el tier de negocio. El repositorio no conoce reglas relacionadas con estados.

## Vista física / despliegue

Muestra en qué nodos y contenedores se ejecuta el software y cómo se comunican.

### Diagrama de despliegue

```mermaid
flowchart LR
    CLIENT["<b>Cliente HTTP</b><br/>curl / Postman"]

    subgraph HOST["Nodo: máquina anfitriona (Docker Engine)"]
        subgraph CT["Contenedor: recursos<br/>imagen payara/server-full:6.2024.6-jdk17"]
            subgraph JVM["JVM · Java 17"]
                subgraph PAYARA["Payara Server 6 · domain1"]
                    subgraph WAR["recursos.war · contexto /recursos"]
                        JAXRS["Contenedor web / JAX-RS<br/>/recursos/api/*"]
                        EJBC["Contenedor EJB<br/>pool de ResourceRequestServiceBean"]
                        CDI["CDI<br/>InMemoryResourceRequestRepository<br/>(1 instancia)"]
                        HEAP[("Heap de la JVM<br/>ConcurrentHashMap")]
                    end
                    ADMIN["Consola de administración"]
                end
            end
        end
    end

    CLIENT -->|"HTTP/JSON · host:8080 → contenedor:8080"| JAXRS
    CLIENT -.->|"HTTPS · 4848 (opcional)"| ADMIN
    JAXRS -->|llamada local| EJBC
    EJBC -->|llamada local| CDI
    CDI --> HEAP
```

| Nodo / artefacto | Detalle |
|---|---|
| Máquina anfitriona | Cualquier equipo con Docker; un solo nodo |
| Contenedor `recursos` | Imagen construida con el `Dockerfile` del proyecto |
| Servidor de aplicaciones | Payara Server 6 (Jakarta EE 10) sobre JDK 17 |
| Artefacto desplegado | `recursos.war`, copiado a `$DEPLOY_DIR` y desplegado al arrancar |
| Puertos | `8080` HTTP (API REST); `4848` consola de administración (opcional) |
| Almacenamiento | Ninguno externo: los datos viven en el heap de la JVM |

Todas las llamadas internas (REST → EJB → repositorio) son **locales**, dentro de la misma JVM (`@Local`, sin serialización ni red). La única comunicación por red es la del cliente con la API.

### Construcción de la imagen

```mermaid
flowchart LR
    SRC["pom.xml + src/"] --> B1["Etapa 1: build<br/>maven:3.9-eclipse-temurin-17<br/>mvn package"]
    B1 -->|target/recursos.war| B2["Etapa 2: runtime<br/>payara/server-full:6.2024.6-jdk17<br/>COPY → $DEPLOY_DIR"]
    B2 --> IMG["Imagen recursos-negocio"]
```

La imagen final solo tiene Payara y el WAR, sin Maven ni el código fuente. Para compilar no hace falta tener Java ni Maven instalados en la máquina anfitriona.

```bash
docker build -t recursos-negocio .
docker run --rm -p 8080:8080 --name recursos recursos-negocio
# si el 8080 del host está ocupado: -p 8081:8080
```

### Implicaciones del despliegue

- **Los datos son volátiles.** Al reiniciar o eliminar el contenedor se pierden las solicitudes, lo cual es aceptable porque el enunciado pide una colección en memoria.
- **Solo funciona con una instancia.** Si se levantaran varios contenedores detrás de un balanceador, cada uno tendría su propio `Map` y verían datos distintos.
- **Cómo escalaría:** para persistir y tener varias instancias basta con agregar un nodo de base de datos y una implementación del puerto (por ejemplo, JPA). Gracias al desacoplamiento descrito en la vista de desarrollo, el EJB y la API no cambian; solo cambian el tier de datos y este diagrama.

## Escenarios

| Escenario | Flujo principal | Resultado |
|---|---|---|
| Registrar una solicitud válida | El cliente envía descripción, tipo, cantidad y prioridad | Solicitud creada con identificador y estado `PENDIENTE` |
| Registrar datos inválidos | Falta un campo obligatorio, la cantidad no es positiva o la prioridad no existe | `BusinessValidationException` y HTTP 400 |
| Consultar una solicitud | El cliente consulta un identificador existente | Se devuelve la información de la solicitud |
| Consultar un identificador inexistente | El repositorio no encuentra la solicitud | `RequestNotFoundException` y HTTP 404 |
| Listar solicitudes pendientes | El EJB consulta todas y filtra por estado | Solo se devuelven solicitudes `PENDIENTE` |
| Atender una solicitud pendiente | El EJB cambia el estado y guarda nuevamente | Solicitud actualizada a `ATENDIDA` |
| Atender una solicitud ya atendida | El estado actual es `ATENDIDA` | `AlreadyAttendedException` y HTTP 409 |

Estos escenarios conectan las vistas lógica, de procesos, de desarrollo y de despliegue mediante las interfaces compartidas.