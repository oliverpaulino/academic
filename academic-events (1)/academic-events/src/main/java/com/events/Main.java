package com.events;

import com.events.config.HibernateUtil;
import com.events.controller.Routes;
import com.events.service.UserService;
import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import org.h2.tools.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws Exception {

        // ── 1. Start H2 in TCP server mode ───────────────────────────────
        Server h2 = Server.createTcpServer(
            "-tcp", "-tcpAllowOthers", "-tcpPort", "9092", "-ifNotExists"
        ).start();
        log.info("H2 TCP server: {}", h2.getURL());

        // Optional: H2 Web Console for development (set env H2_CONSOLE=true)
        Server h2Web = null;
        if ("true".equalsIgnoreCase(System.getenv("H2_CONSOLE"))) {
            h2Web = Server.createWebServer("-web", "-webAllowOthers", "-webPort", "8082").start();
            log.info("H2 Web Console → http://localhost:8082");
        }

        // ── 2. Initialize Hibernate (schema auto-create/update) ───────────
        HibernateUtil.getSessionFactory();
        log.info("Hibernate ready.");

        // ── 3. Seed default admin user ────────────────────────────────────
        UserService.initAdminUser();

        // ── 4. Build Javalin 7 app ────────────────────────────────────────
        //
        // KEY JAVALIN 7 CHANGES applied here:
        //  • config.jetty.port  (NOT defaultPort — renamed in v7)
        //  • All routes inside config.routes.xxx()  (NOT app.get() after create)
        //  • Exception handlers inside config.routes.exception()  (NOT app.exception())
        //  • Javalin.create(config -> { ... }).start()  (createAndStart() removed)
        //
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));

        Javalin app = Javalin.create(config -> {

            // Server port (v7: jetty.port, not jetty.defaultPort)
            config.jetty.port = port;

            // Serve static files from classpath /public
            config.staticFiles.add("/public", Location.CLASSPATH);

            // CORS — allow credentials for session cookies
            config.bundledPlugins.enableCors(cors -> {
                cors.addRule(it -> {
                    it.reflectClientOrigin = true; // Esto soluciona el error
                    it.allowCredentials = true;
                });
            });

            // ── Register ALL routes inside config.routes (Javalin 7 requirement) ──
            Routes.register(config);

            // ── Exception handlers inside config.routes (Javalin 7 requirement) ──
            config.routes.exception(Exception.class, (e, ctx) -> {
                log.error("Unhandled exception on {}: {}", ctx.path(), e.getMessage(), e);
                ctx.status(500).json(java.util.Map.of("error", "Error interno del servidor: " + e.getMessage()));
            });

            config.routes.error(401, ctx ->
                ctx.json(java.util.Map.of("error", "No autenticado"))
            );
            config.routes.error(403, ctx ->
                ctx.json(java.util.Map.of("error", "Acceso denegado"))
            );
            config.routes.error(404, ctx ->
                ctx.json(java.util.Map.of("error", "Recurso no encontrado"))
            );

        }).start(); // .start() with no args — port was already set via config.jetty.port

        log.info("═══════════════════════════════════════════════");
        log.info("  Eventos Académicos PUCMM → http://localhost:{}", port);
        log.info("  Admin: usuario=admin  |  contraseña=Admin123!");
        log.info("═══════════════════════════════════════════════");

        // ── 5. Graceful shutdown ──────────────────────────────────────────
        final Server finalH2Web = h2Web;
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down...");
            app.stop();
            HibernateUtil.shutdown();
            h2.stop();
            if (finalH2Web != null) finalH2Web.stop();
        }));
    }
}
