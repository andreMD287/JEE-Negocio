# Arquitectura — Gestión de Recursos en Emergencias

Este documento describe la arquitectura del tier de negocio mediante el modelo de vistas **4+1**. Las vistas comparten un mismo eje: dos interfaces que separan los tiers del sistema.

| Vista | Pregunta que responde |
|---|---|
| [Desarrollo](#vista-de-desarrollo) | ¿Cómo está organizado el código y en qué dirección van las dependencias? |
| [Lógica](#vista-lógica) | ¿Qué clases componen el sistema y cómo se relacionan? |
| [Procesos](#vista-de-procesos) | ¿Cómo colaboran los componentes en tiempo de ejecución? |
| [Física / despliegue](#vista-física--despliegue) | ¿Dónde se ejecuta el software? |
| [Escenarios](#escenarios) | ¿Qué casos de uso recorren las vistas anteriores? |

Convención de colores en todos los diagramas: **azul** presentación (REST), **violeta** negocio (EJB), **ámbar** dominio, **verde** datos, **gris** infraestructura.

## Interfaces entre tiers

```java
// Contrato del tier de negocio: lo usa la API y lo implementa el EJB
@Local
public interface ResourceRequestService {
    ResourceRequestView register(CreateResourceRequestCommand command);
    List<ResourceRequestView> findAll();
    ResourceRequestView findById(Long id);
    ResourceRequestView attend(Long id);
    List<ResourceRequestView> findPending();
}

// Puerto de almacenamiento: lo usa el EJB y lo implementa el tier de datos
public interface ResourceRequestRepository {
    ResourceRequest save(ResourceRequest request);
    Optional<ResourceRequest> findById(Long id);
    List<ResourceRequest> findAll();
}
```

## Vista de desarrollo

Muestra la organización del código en paquetes, las capas a las que pertenecen y la dirección de sus dependencias.

### Paquetes y dependencias

```mermaid
flowchart TB
    classDef api fill:#DBEAFE,stroke:#2563EB,stroke-width:2px,color:#0F172A
    classDef biz fill:#EDE9FE,stroke:#7C3AED,stroke-width:2px,color:#0F172A
    classDef port fill:#FFFFFF,stroke:#7C3AED,stroke-width:2px,stroke-dasharray:5 3,color:#0F172A
    classDef dom fill:#FEF3C7,stroke:#D97706,stroke-width:2px,color:#0F172A
    classDef data fill:#D1FAE5,stroke:#059669,stroke-width:2px,color:#0F172A

    subgraph L1["Tier de presentación"]
        API["<b>api</b><br/>ResourceRequestResource<br/>ExceptionMappers · DTOs"]:::api
    end

    subgraph L2["Tier de negocio"]
        CONTRACT["<b>business.contract</b><br/>«interface» ResourceRequestService<br/>Command · View"]:::port
        EJB["<b>business.ejb</b><br/>ResourceRequestServiceBean<br/>@Stateless"]:::biz
        EXC["<b>business.exception</b><br/>Validation · NotFound · AlreadyAttended"]:::biz
        PORT["<b>business.port</b><br/>«interface» ResourceRequestRepository"]:::port
    end

    subgraph L3["Tier de datos"]
        DATA["<b>data.memory</b><br/>InMemoryResourceRequestRepository<br/>@ApplicationScoped"]:::data
    end

    DOMAIN["<b>domain.model</b><br/>ResourceRequest · Priority · RequestStatus"]:::dom

    API -->|usa| CONTRACT
    API -->|traduce a HTTP| EXC
    CONTRACT -.->|implementada por| EJB
    EJB -->|lanza| EXC
    EJB -->|"usa (@Inject)"| PORT
    PORT -.->|implementada por| DATA
    L2 --> DOMAIN
    L3 --> DOMAIN

    style L1 fill:#EFF6FF,stroke:#93C5FD
    style L2 fill:#F5F3FF,stroke:#C4B5FD
    style L3 fill:#ECFDF5,stroke:#6EE7B7
```

Las flechas continuas son dependencias de uso y las discontinuas unen cada interfaz (borde punteado) con su implementación. Ambas interfaces pertenecen al tier de negocio: `data.memory` implementa `business.port` y, por tanto, **el tier de datos depende del tier de negocio, y no al revés** (inversión de dependencias).

| Paquete | Tier | Responsabilidad | Depende de |
|---|---|---|---|
| `api` | Presentación | Traducir HTTP a llamadas al servicio y excepciones a códigos HTTP | `business.contract`, `business.exception` |
| `business.contract` | Negocio | Interfaz pública del tier de negocio y sus DTOs | `domain.model` |
| `business.ejb` | Negocio | Reglas y validaciones (EJB `@Stateless`) | `contract`, `exception`, `port`, `domain.model` |
| `business.exception` | Negocio | Errores de negocio (`@ApplicationException`) | — |
| `business.port` | Negocio | Contrato de almacenamiento que requiere el negocio | `domain.model` |
| `domain.model` | Dominio | Entidad y enumeraciones, sin dependencias de frameworks | — |
| `data.memory` | Datos | Almacenamiento en memoria que implementa el puerto | `business.port`, `domain.model` |

### Desacoplamiento entre el tier de negocio y el tier de datos

1. **El puerto pertenece al negocio.** `ResourceRequestRepository` se ubica en `business.port`, no en `data`. El negocio declara las operaciones que necesita (guardar, buscar por identificador, listar) sin conocer cómo se implementan.
2. **Inversión de dependencias.** `data.memory` depende de `business.port`, y no al revés. Ninguna clase de `business` importa elementos de `data`.
3. **Inyección por el contenedor.** El EJB declara `@Inject ResourceRequestRepository repository;` y CDI le entrega en tiempo de ejecución la única implementación disponible, `InMemoryResourceRequestRepository`. El EJB nunca nombra la clase concreta.
4. **Repositorio sin reglas de negocio.** El repositorio no valida cantidades ni estados y no filtra solicitudes pendientes; toda la lógica reside en el EJB. Cambiar el almacenamiento no altera las reglas.
5. **Copias defensivas.** El repositorio almacena y devuelve copias de las entidades. Un cambio solo se persiste cuando el EJB invoca `save()` explícitamente, igual que con una base de datos.

**Consecuencia:** reemplazar el almacenamiento en memoria por JPA/PostgreSQL requiere únicamente una nueva implementación del puerto, por ejemplo `data.jpa.JpaResourceRequestRepository`. Los paquetes `business`, `api` y `domain` no cambian. Las pruebas unitarias del EJB lo demuestran al ejecutarse con un repositorio simulado (Mockito) en lugar del real.

### Concurrencia del almacenamiento en memoria

- `@ApplicationScoped`: existe una sola instancia del repositorio, compartida por todas las instancias del pool de EJB `@Stateless`.
- `ConcurrentHashMap` para el almacenamiento y `AtomicLong` para generar identificadores únicos.
- Verificado en `InMemoryResourceRequestRepositoryTest.concurrentSavesAreSafe`: 8 hilos con 250 inserciones cada uno, sin identificadores repetidos ni pérdidas.
- Limitación conocida: dos invocaciones simultáneas de `attend` sobre la misma solicitud podrían superar ambas la verificación de estado `PENDIENTE`. Con una base de datos se resolvería mediante bloqueo optimista (columna de versión).

### Construcción y empaquetado

| Elemento | Detalle |
|---|---|
| Build | Maven, `maven.compiler.release=17` |
| API | `jakarta.jakartaee-api:10.0.0` (alcance `provided`, la aporta Payara) |
| Pruebas | JUnit 5 + Mockito (`mvn test`) |
| Artefacto | `target/recursos.war` (contexto `/recursos`, API REST en `/recursos/api`) |
| Imagen | `Dockerfile` multi-etapa (ver [vista física](#construcción-de-la-imagen)) |

```text
src/
├── main/java/co/edu/javeriana/recursos/   paquetes de la tabla anterior
├── main/webapp/WEB-INF/beans.xml          CDI, bean-discovery-mode="annotated"
└── test/java/co/edu/javeriana/recursos/   pruebas unitarias por paquete
```

## Vista lógica

Presenta las clases principales y sus relaciones, sin considerar detalles de despliegue.

```mermaid
classDiagram
    direction TB

    class ResourceRequestService {
        <<interface>>
        +register(CreateResourceRequestCommand) ResourceRequestView
        +findAll() List~ResourceRequestView~
        +findById(Long) ResourceRequestView
        +attend(Long) ResourceRequestView
        +findPending() List~ResourceRequestView~
    }
    class ResourceRequestServiceBean {
        <<Stateless>>
        -ResourceRequestRepository repository
        -validateCommand(CreateResourceRequestCommand)
        -parsePriority(String) Priority
    }
    class CreateResourceRequestCommand {
        <<record>>
        +String disasterDescription
        +String resourceType
        +Integer quantity
        +String priority
    }
    class ResourceRequestView {
        <<record>>
        +Long id
        +String disasterDescription
        +String resourceType
        +int quantity
        +Priority priority
        +RequestStatus status
    }
    class ResourceRequestRepository {
        <<interface>>
        +save(ResourceRequest) ResourceRequest
        +findById(Long) Optional~ResourceRequest~
        +findAll() List~ResourceRequest~
    }
    class InMemoryResourceRequestRepository {
        <<ApplicationScoped>>
        -ConcurrentMap storage
        -AtomicLong sequence
    }
    class ResourceRequest {
        -Long id
        -String disasterDescription
        -String resourceType
        -int quantity
        -Priority priority
        -RequestStatus status
        +copy() ResourceRequest
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

    ResourceRequestService <|.. ResourceRequestServiceBean : implementa
    ResourceRequestServiceBean ..> CreateResourceRequestCommand : recibe
    ResourceRequestServiceBean ..> ResourceRequestView : devuelve
    ResourceRequestServiceBean --> ResourceRequestRepository : usa
    ResourceRequestRepository <|.. InMemoryResourceRequestRepository : implementa
    ResourceRequestRepository ..> ResourceRequest : gestiona
    ResourceRequest --> Priority
    ResourceRequest --> RequestStatus

    style ResourceRequestService fill:#FFFFFF,stroke:#7C3AED,stroke-width:2px,stroke-dasharray:5 3
    style ResourceRequestServiceBean fill:#EDE9FE,stroke:#7C3AED,stroke-width:2px
    style CreateResourceRequestCommand fill:#F5F3FF,stroke:#7C3AED
    style ResourceRequestView fill:#F5F3FF,stroke:#7C3AED
    style ResourceRequestRepository fill:#FFFFFF,stroke:#7C3AED,stroke-width:2px,stroke-dasharray:5 3
    style InMemoryResourceRequestRepository fill:#D1FAE5,stroke:#059669,stroke-width:2px
    style ResourceRequest fill:#FEF3C7,stroke:#D97706,stroke-width:2px
    style Priority fill:#FEF3C7,stroke:#D97706
    style RequestStatus fill:#FEF3C7,stroke:#D97706
```

El cliente del negocio depende de `ResourceRequestService` y el EJB depende de `ResourceRequestRepository`. `InMemoryResourceRequestRepository` implementa el puerto, pero el EJB solo se relaciona con la interfaz, nunca con la implementación concreta.

Las excepciones de negocio representan los resultados inválidos de cada operación:

| Excepción | Situación | Respuesta HTTP |
|---|---|---:|
| `BusinessValidationException` | Datos obligatorios ausentes o inválidos | 400 |
| `RequestNotFoundException` | La solicitud consultada no existe | 404 |
| `AlreadyAttendedException` | La solicitud ya estaba atendida | 409 |

Las tres están anotadas con `@ApplicationException(rollback = true)`, de modo que el contenedor EJB no las envuelve en `EJBException` y llegan intactas al `ExceptionMapper` correspondiente.

## Vista de procesos

Describe la colaboración entre componentes en tiempo de ejecución para cada operación. Todas las llamadas entre tiers son locales, dentro de la misma JVM.

### Registrar una solicitud

```mermaid
sequenceDiagram
    autonumber
    actor C as Cliente
    box rgba(37,99,235,0.10) Tier de presentación
        participant API as ResourceRequestResource
        participant MAP as ExceptionMapper
    end
    box rgba(124,58,237,0.10) Tier de negocio
        participant EJB as ResourceRequestServiceBean
    end
    box rgba(5,150,105,0.10) Tier de datos
        participant REP as InMemoryResourceRequestRepository
    end

    C->>API: POST /requests (JSON)
    API->>EJB: register(command)
    activate EJB
    Note over EJB: Valida descripción, tipo de recurso,<br/>cantidad > 0 y prioridad

    alt Datos inválidos
        EJB--)MAP: BusinessValidationException
        MAP-->>C: 400 VALIDATION_ERROR
    else Datos válidos
        EJB->>EJB: Crear ResourceRequest con estado PENDIENTE
        EJB->>REP: save(request)
        REP->>REP: Asignar id (AtomicLong)
        REP-->>EJB: copia de la solicitud guardada
        EJB-->>API: ResourceRequestView
        API-->>C: 201 Created + Location
    end
    deactivate EJB
```

El cliente no envía el identificador ni el estado: el repositorio genera el identificador y el EJB asigna siempre `PENDIENTE`.

### Consultar una solicitud por identificador

```mermaid
sequenceDiagram
    autonumber
    actor C as Cliente
    box rgba(37,99,235,0.10) Tier de presentación
        participant API as ResourceRequestResource
        participant MAP as ExceptionMapper
    end
    box rgba(124,58,237,0.10) Tier de negocio
        participant EJB as ResourceRequestServiceBean
    end
    box rgba(5,150,105,0.10) Tier de datos
        participant REP as InMemoryResourceRequestRepository
    end

    C->>API: GET /requests/{id}
    API->>EJB: findById(id)
    activate EJB
    EJB->>REP: findById(id)

    alt La solicitud existe
        REP-->>EJB: Optional con la solicitud
        EJB-->>API: ResourceRequestView
        API-->>C: 200 OK
    else La solicitud no existe
        REP-->>EJB: Optional vacío
        EJB--)MAP: RequestNotFoundException
        MAP-->>C: 404 REQUEST_NOT_FOUND
    end
    deactivate EJB
```

### Atender una solicitud

```mermaid
sequenceDiagram
    autonumber
    actor C as Cliente
    box rgba(37,99,235,0.10) Tier de presentación
        participant API as ResourceRequestResource
        participant MAP as ExceptionMapper
    end
    box rgba(124,58,237,0.10) Tier de negocio
        participant EJB as ResourceRequestServiceBean
    end
    box rgba(5,150,105,0.10) Tier de datos
        participant REP as InMemoryResourceRequestRepository
    end

    C->>API: PUT /requests/{id}/attend
    API->>EJB: attend(id)
    activate EJB
    EJB->>REP: findById(id)

    alt La solicitud no existe
        REP-->>EJB: Optional vacío
        EJB--)MAP: RequestNotFoundException
        MAP-->>C: 404 REQUEST_NOT_FOUND
    else La solicitud ya está ATENDIDA
        REP-->>EJB: solicitud ATENDIDA
        EJB--)MAP: AlreadyAttendedException
        MAP-->>C: 409 ALREADY_ATTENDED
    else La solicitud está PENDIENTE
        REP-->>EJB: solicitud PENDIENTE
        EJB->>EJB: Cambiar estado a ATENDIDA
        EJB->>REP: save(request)
        REP-->>EJB: copia actualizada
        EJB-->>API: ResourceRequestView
        API-->>C: 200 OK
    end
    deactivate EJB
```

El EJB invoca `save()` después de cambiar el estado porque el repositorio trabaja con copias: modificar el objeto recibido no altera lo almacenado.

### Consultar las solicitudes pendientes

```mermaid
sequenceDiagram
    autonumber
    actor C as Cliente
    box rgba(37,99,235,0.10) Tier de presentación
        participant API as ResourceRequestResource
    end
    box rgba(124,58,237,0.10) Tier de negocio
        participant EJB as ResourceRequestServiceBean
    end
    box rgba(5,150,105,0.10) Tier de datos
        participant REP as InMemoryResourceRequestRepository
    end

    C->>API: GET /requests/pending
    API->>EJB: findPending()
    activate EJB
    EJB->>REP: findAll()
    REP-->>EJB: todas las solicitudes
    EJB->>EJB: Filtrar estado PENDIENTE
    EJB-->>API: List<ResourceRequestView>
    deactivate EJB
    API-->>C: 200 OK
```

El filtrado ocurre en el tier de negocio; el repositorio no conoce reglas relacionadas con los estados. La consulta de todas las solicitudes (`GET /requests`) sigue el mismo flujo sin el paso de filtrado.

## Vista física / despliegue

Muestra los nodos y contenedores donde se ejecuta el software y cómo se comunican.

### Diagrama de despliegue

```mermaid
flowchart TB
    classDef client fill:#F1F5F9,stroke:#475569,stroke-width:2px,color:#0F172A
    classDef api fill:#DBEAFE,stroke:#2563EB,stroke-width:2px,color:#0F172A
    classDef biz fill:#EDE9FE,stroke:#7C3AED,stroke-width:2px,color:#0F172A
    classDef data fill:#D1FAE5,stroke:#059669,stroke-width:2px,color:#0F172A
    classDef infra fill:#F3F4F6,stroke:#6B7280,stroke-width:1px,color:#0F172A

    CLIENT["<b>Cliente HTTP</b><br/>Postman · Newman · curl"]:::client

    subgraph HOST["Máquina anfitriona · Docker"]
        subgraph CT["Contenedor «recursos»"]
            subgraph PAYARA["Payara Server 6 · JDK 17"]
                subgraph WAR["recursos.war"]
                    JAXRS["<b>Contenedor web</b><br/>JAX-RS · /recursos/api/*"]:::api
                    EJBC["<b>Contenedor EJB</b><br/>pool de ResourceRequestServiceBean"]:::biz
                    CDI["<b>CDI</b><br/>InMemoryResourceRequestRepository<br/>instancia única"]:::data
                    HEAP[("<b>Heap de la JVM</b><br/>ConcurrentHashMap")]:::data
                end
                ADMIN["Consola de administración"]:::infra
            end
        end
    end

    CLIENT ==>|"HTTP · JSON<br/>puerto 8080"| JAXRS
    CLIENT -.->|"HTTPS · 4848<br/>(opcional)"| ADMIN
    JAXRS -->|"llamada local<br/>@Local"| EJBC
    EJBC -->|"llamada local<br/>@Inject"| CDI
    CDI --> HEAP

    style HOST fill:#F8FAFC,stroke:#94A3B8
    style CT fill:#F1F5F9,stroke:#64748B
    style PAYARA fill:#E2E8F0,stroke:#475569
    style WAR fill:#FFFFFF,stroke:#334155,stroke-width:2px
```

| Nodo / artefacto | Detalle |
|---|---|
| Máquina anfitriona | Cualquier equipo con Docker; un solo nodo |
| Contenedor `recursos` | Imagen construida con el `Dockerfile` del proyecto |
| Servidor de aplicaciones | Payara Server 6 (Jakarta EE 10) sobre JDK 17 |
| Artefacto desplegado | `recursos.war`, copiado a `$DEPLOY_DIR` y desplegado al arrancar |
| Puertos | `8080` HTTP (API REST); `4848` consola de administración (opcional) |
| Almacenamiento | Sin almacenamiento externo: los datos residen en el heap de la JVM |

Todas las llamadas internas (REST → EJB → repositorio) son **locales** dentro de la misma JVM (`@Local`), sin serialización ni red. La única comunicación por red es la del cliente con la API.

### Construcción de la imagen

```mermaid
flowchart LR
    classDef src fill:#FEF3C7,stroke:#D97706,stroke-width:2px,color:#0F172A
    classDef stage fill:#F3F4F6,stroke:#6B7280,stroke-width:2px,color:#0F172A
    classDef out fill:#DBEAFE,stroke:#2563EB,stroke-width:2px,color:#0F172A

    SRC["<b>Código fuente</b><br/>pom.xml · src/"]:::src
    B1["<b>Etapa 1 · build</b><br/>Maven 3.9 · JDK 17<br/>mvn package"]:::stage
    WARF[["recursos.war"]]:::out
    B2["<b>Etapa 2 · runtime</b><br/>Payara Server 6 · JDK 17<br/>despliegue automático"]:::stage
    IMG[("<b>Imagen</b><br/>recursos-negocio")]:::out

    SRC --> B1 --> WARF --> B2 --> IMG
```

Imágenes base: `maven:3.9-eclipse-temurin-17` para compilar y `payara/server-full:6.2024.6-jdk17` para ejecutar; el WAR se copia a `$DEPLOY_DIR` y Payara lo despliega al arrancar. La imagen final contiene únicamente Payara y el WAR, sin Maven ni el código fuente. No se requiere Java ni Maven en la máquina anfitriona.

```bash
docker build -t recursos-negocio .
docker run --rm -p 8080:8080 --name recursos recursos-negocio
```

### Implicaciones del despliegue

- **Datos volátiles.** Al reiniciar o eliminar el contenedor se pierden las solicitudes, comportamiento esperado para una colección en memoria.
- **Una sola instancia.** Con varios contenedores detrás de un balanceador, cada uno tendría su propio `Map` y los datos serían inconsistentes.
- **Evolución.** Para persistir y escalar horizontalmente basta con agregar un nodo de base de datos y una implementación del puerto (por ejemplo, JPA). Gracias al desacoplamiento descrito en la vista de desarrollo, el EJB y la API no cambian.

## Escenarios

El diagrama muestra los casos de uso del sistema y las reglas de negocio que aplica cada uno. Cada escenario recorre las demás vistas: entra por el tier de presentación ([desarrollo](#vista-de-desarrollo)), aplica las reglas en el EJB ([lógica](#vista-lógica)), sigue una de las secuencias de la [vista de procesos](#vista-de-procesos) y se ejecuta en el contenedor descrito en la [vista física](#vista-física--despliegue). Todos están verificados en las [evidencias](evidencias/README.md).

```mermaid
flowchart LR
    classDef actor fill:#F1F5F9,stroke:#475569,stroke-width:2px,color:#0F172A
    classDef uc fill:#EDE9FE,stroke:#7C3AED,stroke-width:2px,color:#0F172A
    classDef rule fill:#FFFFFF,stroke:#D97706,stroke-width:1px,stroke-dasharray:4 3,color:#0F172A

    ACTOR["👤<br/><b>Centro de atención</b>"]:::actor

    subgraph SYS["Gestión de recursos"]
        UC1(["Registrar solicitud"]):::uc
        UC2(["Consultar todas"]):::uc
        UC3(["Consultar por id"]):::uc
        UC4(["Consultar pendientes"]):::uc
        UC5(["Atender solicitud"]):::uc
    end

    R1["Cantidad mayor que cero<br/>Descripción obligatoria<br/>Tipo de recurso obligatorio<br/>Prioridad válida<br/>Estado inicial PENDIENTE"]:::rule
    R2["La solicitud<br/>debe existir"]:::rule
    R3["Debe existir y<br/>estar PENDIENTE"]:::rule

    ACTOR --- UC1 & UC2 & UC3 & UC4 & UC5
    UC1 -.- R1
    UC3 -.- R2
    UC5 -.- R3

    style SYS fill:#F5F3FF,stroke:#C4B5FD
```

| Escenario | Flujo principal | Resultado |
|---|---|---|
| Registrar una solicitud válida | El cliente envía descripción, tipo de recurso, cantidad y prioridad | Solicitud creada con identificador y estado `PENDIENTE` (201) |
| Registrar datos inválidos | Falta un campo obligatorio, la cantidad no es positiva o la prioridad no existe | `BusinessValidationException` (400) |
| Consultar una solicitud | El cliente consulta un identificador existente | Se devuelve la solicitud (200) |
| Consultar un identificador inexistente | El repositorio no encuentra la solicitud | `RequestNotFoundException` (404) |
| Consultar solicitudes pendientes | El EJB consulta todas y filtra por estado | Solo solicitudes `PENDIENTE` (200) |
| Atender una solicitud pendiente | El EJB cambia el estado y la guarda nuevamente | Solicitud en estado `ATENDIDA` (200) |
| Atender una solicitud ya atendida | El estado actual es `ATENDIDA` | `AlreadyAttendedException` (409) |
| Atender una solicitud inexistente | El repositorio no encuentra la solicitud | `RequestNotFoundException` (404) |
