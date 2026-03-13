# Sistema de Gestión de Eventos Académicos
**Programación Web - ICC-352 | PUCMM**

## Stack Tecnológico
| Capa | Tecnología |
|------|-----------|
| Backend | **Javalin 7.0.0** (Jetty 12, Jakarta Servlet) |
| ORM | **Hibernate 6.4** |
| Base de datos | **H2 2.2** en modo servidor TCP |
| Build | **Gradle 8.7** + Shadow Plugin (fat JAR) |
| Frontend | Bootstrap 5, Vanilla JS, Fetch API |
| QR Gen (server) | ZXing 3.5.3 |
| QR Scan (client) | html5-qrcode 2.3.8 |
| Charts | Chart.js 4.4 |
| Infra | Docker multi-stage, Docker Compose, Nginx SSL/TLS |

## Cambios aplicados de Javalin 6 → 7 (migration guide)
| Cambio | Aplicado |
|--------|----------|
| Rutas dentro de `config.routes.*` | ✅ `Routes.register(config)` |
| Exceptions dentro de `config.routes.exception()` | ✅ |
| `jetty.defaultPort` → `jetty.port` | ✅ `config.jetty.port = port` |
| `createAndStart()` eliminado → `create().start()` | ✅ |
| Java 17 requerido | ✅ `sourceCompatibility = JavaVersion.VERSION_17` |
| `javax.servlet` → `jakarta.servlet` (Jetty 12) | ✅ (transitivo vía javalin-bundle) |

## Estructura del Proyecto
```
academic-events/
├── build.gradle                          # Gradle + Shadow plugin
├── settings.gradle
├── Dockerfile                            # Multi-stage (Gradle builder + JRE alpine)
├── docker-compose.yml                    # App + Nginx SSL
├── nginx/nginx.conf                      # HTTP→HTTPS, proxy_pass
└── src/main/
    ├── java/com/events/
    │   ├── Main.java                     # Javalin 7 startup (config.jetty.port, .start())
    │   ├── config/HibernateUtil.java
    │   ├── model/        User, Event, Registration, enums
    │   ├── repository/   UserRepo, EventRepo, RegistrationRepo (Hibernate 6)
    │   ├── service/      UserService, EventService, RegistrationService, QRService
    │   └── controller/Routes.java        # TODOS los endpoints en config.routes.*
    └── resources/public/
        ├── index.html          # Listado de eventos (Bootstrap grid)
        ├── login.html          # Autenticación
        ├── register.html       # Registro de participantes
        ├── dashboard.html      # Panel organizador / admin
        ├── admin.html          # Panel administrador (usuarios + todos los eventos)
        ├── my-registrations.html  # Inscripciones con QR descargable
        ├── scan-qr.html           # Escaneo de cámara (html5-qrcode)
        └── event-stats.html       # Estadísticas + Chart.js
```

## Credenciales por defecto
| Campo | Valor |
|-------|-------|
| Usuario | `admin` |
| Contraseña | `Admin123!` |

## Ejecutar con Gradle
```bash
# Compilar y construir fat JAR
./gradlew shadowJar

# Ejecutar
java -jar build/libs/academic-events.jar

# O ejecutar directamente
./gradlew run     # requiere aplicar plugin 'application' en build.gradle

# Acceder:  http://localhost:8080
# Admin:    admin / Admin123!
```

## Docker — Build y Run
```bash
# Construir imagen
docker build -t academic-events .

# Correr (con volumen para la base de datos)
docker run -p 8080:8080 -v eventos_data:/app/data academic-events
```

## Docker Compose — Producción con SSL
```bash
# 1. Generar certificados (desarrollo — self-signed)
mkdir -p nginx/certs
openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
  -keyout nginx/certs/key.pem \
  -out    nginx/certs/cert.pem \
  -subj "/CN=tu-dominio.com"

# Para producción usa Let's Encrypt / Certbot

# 2. Levantar todos los servicios
docker-compose up -d

# Acceder:
#   http://localhost   → redirige a https://localhost (puerto 80→443)
#   https://localhost  → app completa
```

## Publicar en Docker Hub
```bash
docker build -t tu-usuario/academic-events:latest .
docker push tu-usuario/academic-events:latest
```

## API REST

### Autenticación
```
POST /api/auth/login      { username, password }
POST /api/auth/register   { username, email, password }
POST /api/auth/logout
GET  /api/auth/me
```

### Eventos
```
GET    /api/events                  Lista pública / completa (por rol)
GET    /api/events/{id}
POST   /api/events                  Crear (ORGANIZER/ADMIN)
PUT    /api/events/{id}             Editar
POST   /api/events/{id}/publish
POST   /api/events/{id}/unpublish
POST   /api/events/{id}/cancel
DELETE /api/events/{id}             Solo ADMIN
GET    /api/my-events               Eventos creados por el usuario actual
```

### Inscripciones y QR
```
POST   /api/events/{id}/register    Inscribirse
DELETE /api/events/{id}/register    Cancelar inscripción
GET    /api/events/{id}/registrations  Lista inscritos (ORGANIZER/ADMIN)
GET    /api/my-registrations
GET    /api/registrations/{token}/qr   Imagen PNG del QR
POST   /api/registrations/scan      { qrContent } → marcar asistencia
```

### Estadísticas
```
GET /api/events/{id}/stats   → { totalRegistered, totalAttended, attendancePercentage,
                                  inscriptionsByDay, attendanceByHour, ... }
```

### Admin
```
GET    /api/admin/users
PUT    /api/admin/users/{id}/block
PUT    /api/admin/users/{id}/unblock
PUT    /api/admin/users/{id}/role    { role: "ORGANIZER" }
GET    /api/admin/events
DELETE /api/admin/events/{id}
```

## Roles
| Rol | Permisos |
|-----|----------|
| **ADMIN** | Gestión total de usuarios y eventos |
| **ORGANIZER** | Crear/editar/publicar/cancelar eventos, escanear QR, ver stats |
| **PARTICIPANT** | Ver eventos publicados, inscribirse, ver su QR |

## Formato del QR
```
EVENT:{eventId}|USER:{userId}|TOKEN:{uuid}
```
Ejemplo: `EVENT:3|USER:7|TOKEN:550e8400-e29b-41d4-a716-446655440000`

## H2 Console (desarrollo)
```bash
H2_CONSOLE=true java -jar build/libs/academic-events.jar
# Web Console: http://localhost:8082
# JDBC URL:    jdbc:h2:tcp://localhost:9092/./data/academic_events
# Usuario:     sa  |  Contraseña: (vacío)
```
