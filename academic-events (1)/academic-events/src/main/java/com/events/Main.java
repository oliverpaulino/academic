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

        Server h2 = Server.createTcpServer(
                "-tcp", "-tcpAllowOthers", "-tcpPort", "9092", "-ifNotExists").start();
        log.info("H2 TCP server: {}", h2.getURL());

        Server h2Web = null;
        if ("true".equalsIgnoreCase(System.getenv("H2_CONSOLE"))) {
            h2Web = Server.createWebServer("-web", "-webAllowOthers", "-webPort", "8082").start();
            log.info("H2 Web Console → http://localhost:8082");
        }

        HibernateUtil.getSessionFactory();
        log.info("Hibernate ready.");

        UserService.initAdminUser();

        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));

        Javalin app = Javalin.create(config -> {

            config.jetty.port = port;

            config.staticFiles.add("/public", Location.CLASSPATH);

            config.bundledPlugins.enableCors(cors -> {
                cors.addRule(it -> {
                    it.reflectClientOrigin = true;
                    it.allowCredentials = true;
                });
            });

            Routes.register(config);

            config.routes.exception(Exception.class, (e, ctx) -> {
                log.error("Unhandled exception on {}: {}", ctx.path(), e.getMessage(), e);
                ctx.status(500).json(java.util.Map.of("error", "Error interno del servidor: " + e.getMessage()));
            });

            config.routes.error(401, ctx -> ctx.json(java.util.Map.of("error", "No autenticado")));
            config.routes.error(403, ctx -> ctx.json(java.util.Map.of("error", "Acceso denegado")));
            config.routes.error(404, ctx -> ctx.json(java.util.Map.of("error", "Recurso no encontrado")));

        }).start();

        log.info("  Eventos Académicos PUCMM → http://localhost:{}", port);
        log.info("  Admin: usuario=admin  |  contraseña=admin");

        final Server finalH2Web = h2Web;
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down...");
            app.stop();
            HibernateUtil.shutdown();
            h2.stop();
            if (finalH2Web != null)
                finalH2Web.stop();
        }));
    }
}
