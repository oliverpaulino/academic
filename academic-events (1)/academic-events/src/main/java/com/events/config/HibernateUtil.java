package com.events.config;

import com.events.model.Event;
import com.events.model.Registration;
import com.events.model.User;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HibernateUtil {

    private static final Logger log = LoggerFactory.getLogger(HibernateUtil.class);
    private static volatile SessionFactory sessionFactory;

    public static SessionFactory getSessionFactory() {
        if (sessionFactory == null) {
            synchronized (HibernateUtil.class) {
                if (sessionFactory == null) {
                    try {
                        String dbUrl = System.getenv().getOrDefault(
                            "DB_URL",
                            "jdbc:h2:tcp://localhost:9092/./data/academic_events"
                        );
                        Configuration cfg = new Configuration()
                            .setProperty("hibernate.connection.driver_class", "org.h2.Driver")
                            .setProperty("hibernate.connection.url", dbUrl)
                            .setProperty("hibernate.connection.username", "sa")
                            .setProperty("hibernate.connection.password", "")
                            .setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect")
                            .setProperty("hibernate.hbm2ddl.auto", "update")
                            .setProperty("hibernate.show_sql", "false")
                            .setProperty("hibernate.connection.pool_size", "10")
                            .addAnnotatedClass(User.class)
                            .addAnnotatedClass(Event.class)
                            .addAnnotatedClass(Registration.class);

                        sessionFactory = cfg.buildSessionFactory();
                        log.info("Hibernate SessionFactory ready.");
                    } catch (Exception e) {
                        log.error("Failed to build SessionFactory", e);
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        return sessionFactory;
    }

    public static void shutdown() {
        if (sessionFactory != null && !sessionFactory.isClosed()) {
            sessionFactory.close();
        }
    }
}
