package com.events.controller;

import com.events.model.*;
import com.events.service.*;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import io.javalin.http.NotFoundResponse;
import io.javalin.http.UnauthorizedResponse;
import io.javalin.http.ForbiddenResponse;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Javalin 7 — ALL routes MUST be registered inside config.routes.*
 * (registering routes after Javalin.create() is NOT supported in v7)
 */
public class Routes {

    private static final DateTimeFormatter DTF = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public static void register(JavalinConfig config) {
        config.routes.get("/", ctx -> ctx.redirect("/index.html"));

        // ── Auth ──────────────────────────────────────────────────────────
        config.routes.post("/api/auth/login",    Routes::login);
        config.routes.post("/api/auth/logout",   Routes::logout);
        config.routes.post("/api/auth/register", Routes::registerUser);
        config.routes.get ("/api/auth/me",       Routes::me);

        // ── Events (public + protected) ───────────────────────────────────
        config.routes.get   ("/api/events",              Routes::listEvents);
        config.routes.get   ("/api/events/{id}",         Routes::getEvent);
        config.routes.post  ("/api/events",              Routes::createEvent);
        config.routes.put   ("/api/events/{id}",         Routes::updateEvent);
        config.routes.post  ("/api/events/{id}/publish",   Routes::publishEvent);
        config.routes.post  ("/api/events/{id}/unpublish", Routes::unpublishEvent);
        config.routes.post  ("/api/events/{id}/cancel",    Routes::cancelEvent);
        config.routes.delete("/api/events/{id}",           Routes::deleteEvent);
        config.routes.get   ("/api/my-events",             Routes::myEvents);

        // ── Registrations & QR ────────────────────────────────────────────
        config.routes.post  ("/api/events/{id}/register",     Routes::registerForEvent);
        config.routes.delete("/api/events/{id}/register",     Routes::cancelRegistration);
        config.routes.get   ("/api/events/{id}/registrations",Routes::listRegistrations);
        config.routes.get   ("/api/my-registrations",         Routes::myRegistrations);
        config.routes.get   ("/api/registrations/{token}/qr", Routes::getQR);
        config.routes.post  ("/api/registrations/scan",       Routes::scanQR);

        // ── Statistics ────────────────────────────────────────────────────
        config.routes.get("/api/events/{id}/stats", Routes::getStats);

        // ── Admin ─────────────────────────────────────────────────────────
        config.routes.get   ("/api/admin/users",              Routes::adminListUsers);
        config.routes.put   ("/api/admin/users/{id}/block",   Routes::adminBlockUser);
        config.routes.put   ("/api/admin/users/{id}/unblock", Routes::adminUnblockUser);
        config.routes.put   ("/api/admin/users/{id}/role",    Routes::adminChangeRole);
        config.routes.get   ("/api/admin/events",             Routes::adminListEvents);
        config.routes.delete("/api/admin/events/{id}",        Routes::adminDeleteEvent);
    }

    // =========================================================================
    // AUTH
    // =========================================================================

    private static void login(Context ctx) {
        Map<String, String> body = ctx.bodyAsClass(Map.class);
        String username = body.get("username");
        String password = body.get("password");
        if (username == null || password == null) { ctx.status(400).json(err("Campos requeridos")); return; }

        UserService.login(username, password).ifPresentOrElse(
                user -> {
                    ctx.sessionAttribute("userId",   user.getId());
                    ctx.sessionAttribute("userRole", user.getRole().name());
                    ctx.json(userMap(user));
                },
                () -> ctx.status(401).json(err("Credenciales inválidas o cuenta bloqueada"))
        );
    }

    private static void logout(Context ctx) {
        ctx.req().getSession().invalidate();
        ctx.json(Map.of("message", "Sesión cerrada"));
    }

    private static void registerUser(Context ctx) {
        Map<String, String> body = ctx.bodyAsClass(Map.class);
        String username = body.get("username");
        String email    = body.get("email");
        String password = body.get("password");
        if (username == null || email == null || password == null) { ctx.status(400).json(err("Todos los campos son requeridos")); return; }
        if (password.length() < 6) { ctx.status(400).json(err("La contraseña debe tener mínimo 6 caracteres")); return; }
        try {
            User user = UserService.register(username, email, password);
            ctx.sessionAttribute("userId",   user.getId());
            ctx.sessionAttribute("userRole", user.getRole().name());
            ctx.status(201).json(userMap(user));
        } catch (IllegalArgumentException e) {
            ctx.status(409).json(err(e.getMessage()));
        }
    }

    private static void me(Context ctx) {
        Long userId = ctx.sessionAttribute("userId");
        if (userId == null) throw new UnauthorizedResponse("No has iniciado sesión");
        ctx.json(userMap(UserService.findById(userId).orElseThrow(() -> new UnauthorizedResponse())));
    }

    // =========================================================================
    // EVENTS
    // =========================================================================

    private static void listEvents(Context ctx) {
        Long userId = ctx.sessionAttribute("userId");
        List<Event> events;
        if (userId != null) {
            User user = UserService.findById(userId).orElse(null);
            events = (user != null && user.getRole() != Role.PARTICIPANT)
                    ? EventService.findAll()
                    : EventService.findAllPublished();
        } else {
            events = EventService.findAllPublished();
        }
        ctx.json(events.stream().map(e -> eventMap(e, userId)).toList());
    }

    private static void getEvent(Context ctx) {
        Long id     = Long.parseLong(ctx.pathParam("id"));
        Long userId = ctx.sessionAttribute("userId");
        Event event = EventService.findById(id).orElseThrow(() -> new NotFoundResponse("Evento no encontrado"));
        ctx.json(eventMap(event, userId));
    }

    private static void createEvent(Context ctx) {
        requireOrganizerOrAdmin(ctx);
        Long userId = ctx.sessionAttribute("userId");
        Map<String, Object> body = ctx.bodyAsClass(Map.class);
        try {
            User creator = UserService.findById(userId).orElseThrow();
            Event event = EventService.create(
                    (String) body.get("title"),
                    (String) body.get("description"),
                    LocalDateTime.parse((String) body.get("dateTime"), DTF),
                    (String) body.get("location"),
                    ((Number) body.get("maxCapacity")).intValue(),
                    creator
            );
            ctx.status(201).json(eventMap(event, userId));
        } catch (Exception e) {
            ctx.status(400).json(err(e.getMessage()));
        }
    }

    private static void updateEvent(Context ctx) {
        requireOrganizerOrAdmin(ctx);
        Long userId  = ctx.sessionAttribute("userId");
        Long eventId = Long.parseLong(ctx.pathParam("id"));
        Map<String, Object> body = ctx.bodyAsClass(Map.class);
        try {
            Event event = EventService.update(eventId,
                    (String) body.get("title"),
                    (String) body.get("description"),
                    LocalDateTime.parse((String) body.get("dateTime"), DTF),
                    (String) body.get("location"),
                    ((Number) body.get("maxCapacity")).intValue(),
                    userId
            );
            ctx.json(eventMap(event, userId));
        } catch (IllegalArgumentException | SecurityException e) {
            ctx.status(400).json(err(e.getMessage()));
        }
    }

    private static void publishEvent(Context ctx) {
        requireOrganizerOrAdmin(ctx);
        Long userId = ctx.sessionAttribute("userId");
        ctx.json(eventMap(EventService.setStatus(Long.parseLong(ctx.pathParam("id")), EventStatus.PUBLISHED, userId), userId));
    }

    private static void unpublishEvent(Context ctx) {
        requireOrganizerOrAdmin(ctx);
        Long userId = ctx.sessionAttribute("userId");
        ctx.json(eventMap(EventService.setStatus(Long.parseLong(ctx.pathParam("id")), EventStatus.DRAFT, userId), userId));
    }

    private static void cancelEvent(Context ctx) {
        requireOrganizerOrAdmin(ctx);
        Long userId = ctx.sessionAttribute("userId");
        ctx.json(eventMap(EventService.setStatus(Long.parseLong(ctx.pathParam("id")), EventStatus.CANCELLED, userId), userId));
    }

    private static void deleteEvent(Context ctx) {
        requireAdmin(ctx);
        EventService.delete(Long.parseLong(ctx.pathParam("id")));
        ctx.json(Map.of("message", "Evento eliminado"));
    }

    private static void myEvents(Context ctx) {
        Long userId = ctx.sessionAttribute("userId");
        if (userId == null) throw new UnauthorizedResponse();
        ctx.json(EventService.findByCreator(userId).stream().map(e -> eventMap(e, userId)).toList());
    }

    // =========================================================================
    // REGISTRATIONS & QR
    // =========================================================================

    private static void registerForEvent(Context ctx) {
        Long userId = requireLoggedIn(ctx);
        Long eventId = Long.parseLong(ctx.pathParam("id"));
        try {
            Registration reg = RegistrationService.register(eventId, userId);
            ctx.status(201).json(regMap(reg));
        } catch (IllegalArgumentException e) {
            ctx.status(400).json(err(e.getMessage()));
        }
    }

    private static void cancelRegistration(Context ctx) {
        Long userId  = requireLoggedIn(ctx);
        Long eventId = Long.parseLong(ctx.pathParam("id"));
        try {
            RegistrationService.cancel(eventId, userId);
            ctx.json(Map.of("message", "Inscripción cancelada"));
        } catch (IllegalArgumentException e) {
            ctx.status(400).json(err(e.getMessage()));
        }
    }

    private static void listRegistrations(Context ctx) {
        requireOrganizerOrAdmin(ctx);
        Long eventId = Long.parseLong(ctx.pathParam("id"));
        ctx.json(RegistrationService.findByEvent(eventId).stream().map(Routes::regMap).toList());
    }

    private static void myRegistrations(Context ctx) {
        Long userId = requireLoggedIn(ctx);
        ctx.json(RegistrationService.findByUser(userId).stream().map(Routes::regMap).toList());
    }

    private static void getQR(Context ctx) {
        Long userId = requireLoggedIn(ctx);
        String token = ctx.pathParam("token");
        Registration reg = RegistrationService.findByToken(token)
                .orElseThrow(() -> new NotFoundResponse("Inscripción no encontrada"));
        User user = UserService.findById(userId).orElseThrow();
        if (!reg.getUser().getId().equals(userId) && user.getRole() == Role.PARTICIPANT)
            throw new ForbiddenResponse("No autorizado");
        byte[] qr = QRService.generateQR(reg.getEvent().getId(), reg.getUser().getId(), token);
        ctx.contentType("image/png").result(qr);
    }

    private static void scanQR(Context ctx) {
        requireOrganizerOrAdmin(ctx);
        Map<String, String> body = ctx.bodyAsClass(Map.class);
        String qrContent = body.get("qrContent");
        if (qrContent == null || qrContent.isBlank()) { ctx.status(400).json(err("Contenido QR requerido")); return; }
        String token = QRService.extractToken(qrContent);
        if (token == null) { ctx.status(400).json(err("Formato QR inválido")); return; }
        try {
            Registration reg = RegistrationService.markAttendance(token);
            ctx.json(Map.of("message", "Asistencia registrada", "registration", regMap(reg)));
        } catch (IllegalArgumentException e) {
            ctx.status(400).json(err(e.getMessage()));
        }
    }

    // =========================================================================
    // STATISTICS
    // =========================================================================

    private static void getStats(Context ctx) {
        requireOrganizerOrAdmin(ctx);
        Long eventId = Long.parseLong(ctx.pathParam("id"));
        Event event = EventService.findById(eventId).orElseThrow(() -> new NotFoundResponse("Evento no encontrado"));
        List<Registration> regs = RegistrationService.findByEvent(eventId);
        long attended = regs.stream().filter(Registration::isAttended).count();
        double pct = regs.isEmpty() ? 0.0 : Math.round((double) attended / regs.size() * 10000.0) / 100.0;

        TreeMap<String, Long> byDay = regs.stream().collect(Collectors.groupingBy(
                r -> r.getRegisteredAt().toLocalDate().toString(), TreeMap::new, Collectors.counting()));
        TreeMap<String, Long> byHour = regs.stream().filter(Registration::isAttended).collect(Collectors.groupingBy(
                r -> String.format("%02d:00", r.getAttendedAt().getHour()), TreeMap::new, Collectors.counting()));

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("eventId", eventId);
        stats.put("eventTitle", event.getTitle());
        stats.put("eventDate", event.getDateTime().toString());
        stats.put("maxCapacity", event.getMaxCapacity());
        stats.put("totalRegistered", regs.size());
        stats.put("totalAttended", attended);
        stats.put("attendancePercentage", pct);
        stats.put("inscriptionsByDay", byDay);
        stats.put("attendanceByHour", byHour);
        ctx.json(stats);
    }

    // =========================================================================
    // ADMIN
    // =========================================================================

    private static void adminListUsers(Context ctx) {
        requireAdmin(ctx);
        ctx.json(UserService.findAll().stream().map(Routes::userMap).toList());
    }

    private static void adminBlockUser(Context ctx) {
        requireAdmin(ctx);
        try { ctx.json(userMap(UserService.setBlocked(Long.parseLong(ctx.pathParam("id")), true))); }
        catch (IllegalArgumentException e) { ctx.status(400).json(err(e.getMessage())); }
    }

    private static void adminUnblockUser(Context ctx) {
        requireAdmin(ctx);
        try { ctx.json(userMap(UserService.setBlocked(Long.parseLong(ctx.pathParam("id")), false))); }
        catch (IllegalArgumentException e) { ctx.status(400).json(err(e.getMessage())); }
    }

    private static void adminChangeRole(Context ctx) {
        requireAdmin(ctx);
        Map<String, String> body = ctx.bodyAsClass(Map.class);
        try {
            Role role = Role.valueOf(body.get("role").toUpperCase());
            ctx.json(userMap(UserService.setRole(Long.parseLong(ctx.pathParam("id")), role)));
        } catch (IllegalArgumentException e) { ctx.status(400).json(err(e.getMessage())); }
    }

    private static void adminListEvents(Context ctx) {
        requireAdmin(ctx);
        Long userId = ctx.sessionAttribute("userId");
        ctx.json(EventService.findAll().stream().map(e -> eventMap(e, userId)).toList());
    }

    private static void adminDeleteEvent(Context ctx) {
        requireAdmin(ctx);
        EventService.delete(Long.parseLong(ctx.pathParam("id")));
        ctx.json(Map.of("message", "Evento eliminado"));
    }

    // =========================================================================
    // HELPERS — authorization guards
    // =========================================================================

    private static Long requireLoggedIn(Context ctx) {
        Long userId = ctx.sessionAttribute("userId");
        if (userId == null) throw new UnauthorizedResponse("No has iniciado sesión");
        return userId;
    }

    private static void requireOrganizerOrAdmin(Context ctx) {
        Long userId = requireLoggedIn(ctx);
        User user = UserService.findById(userId).orElseThrow(() -> new UnauthorizedResponse());
        if (user.getRole() == Role.PARTICIPANT) throw new ForbiddenResponse("Se requiere rol Organizador o Administrador");
    }

    private static void requireAdmin(Context ctx) {
        Long userId = requireLoggedIn(ctx);
        User user = UserService.findById(userId).orElseThrow(() -> new UnauthorizedResponse());
        if (user.getRole() != Role.ADMIN) throw new ForbiddenResponse("Solo los administradores pueden hacer esto");
    }

    // =========================================================================
    // HELPERS — DTO maps
    // =========================================================================

    public static Map<String, Object> userMap(User u) {
        return Map.of("id", u.getId(), "username", u.getUsername(), "email", u.getEmail(),
                "role", u.getRole().name(), "blocked", u.isBlocked());
    }

    public static Map<String, Object> eventMap(Event e, Long currentUserId) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",           e.getId());
        m.put("title",        e.getTitle());
        m.put("description",  e.getDescription());
        m.put("dateTime",     e.getDateTime().toString());
        m.put("location",     e.getLocation());
        m.put("maxCapacity",  e.getMaxCapacity());
        m.put("status",       e.getStatus().name());
        m.put("createdBy",    Map.of("id", e.getCreatedBy().getId(), "username", e.getCreatedBy().getUsername()));
        m.put("createdAt",    e.getCreatedAt().toString());
        long reg = RegistrationService.countByEvent(e.getId());
        m.put("registeredCount", reg);
        m.put("spotsLeft",       Math.max(0, e.getMaxCapacity() - reg));
        m.put("isRegistered", currentUserId != null &&
                RegistrationService.findByEventAndUser(e.getId(), currentUserId).isPresent());
        return m;
    }

    public static Map<String, Object> regMap(Registration r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",           r.getId());
        m.put("token",        r.getToken());
        m.put("attended",     r.isAttended());
        m.put("attendedAt",   r.getAttendedAt() != null ? r.getAttendedAt().toString() : null);
        m.put("registeredAt", r.getRegisteredAt().toString());
        m.put("event", Map.of(
                "id",       r.getEvent().getId(),
                "title",    r.getEvent().getTitle(),
                "dateTime", r.getEvent().getDateTime().toString(),
                "location", r.getEvent().getLocation(),
                "status",   r.getEvent().getStatus().name()
        ));
        m.put("user", Map.of(
                "id",       r.getUser().getId(),
                "username", r.getUser().getUsername(),
                "email",    r.getUser().getEmail()
        ));
        return m;
    }

    private static Map<String, String> err(String msg) {
        return Map.of("error", msg);
    }
}
