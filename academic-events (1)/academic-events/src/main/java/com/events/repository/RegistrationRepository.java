package com.events.repository;

import com.events.config.HibernateUtil;
import com.events.model.Registration;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import java.util.List;
import java.util.Optional;

public class RegistrationRepository {

    private final SessionFactory sf = HibernateUtil.getSessionFactory();

    public Registration save(Registration reg) {
        try (Session s = sf.openSession()) {
            Transaction tx = s.beginTransaction();
            s.persist(reg);
            tx.commit();
            return reg;
        }
    }

    public Registration update(Registration reg) {
        try (Session s = sf.openSession()) {
            Transaction tx = s.beginTransaction();
            Registration merged = s.merge(reg);
            tx.commit();
            return merged;
        }
    }

    public void delete(Long id) {
        try (Session s = sf.openSession()) {
            Transaction tx = s.beginTransaction();
            Registration r = s.get(Registration.class, id);
            if (r != null) s.remove(r);
            tx.commit();
        }
    }

    public Optional<Registration> findById(Long id) {
        try (Session s = sf.openSession()) {
            return Optional.ofNullable(s.get(Registration.class, id));
        }
    }

    public Optional<Registration> findByToken(String token) {
        try (Session s = sf.openSession()) {
            return s.createQuery("FROM Registration WHERE token = :t", Registration.class)
                    .setParameter("t", token)
                    .uniqueResultOptional();
        }
    }

    public Optional<Registration> findByEventAndUser(Long eventId, Long userId) {
        try (Session s = sf.openSession()) {
            return s.createQuery(
                    "FROM Registration WHERE event.id = :eid AND user.id = :uid", Registration.class)
                    .setParameter("eid", eventId)
                    .setParameter("uid", userId)
                    .uniqueResultOptional();
        }
    }

    public List<Registration> findByEvent(Long eventId) {
        try (Session s = sf.openSession()) {
            return s.createQuery(
                    "FROM Registration WHERE event.id = :eid ORDER BY registeredAt ASC", Registration.class)
                    .setParameter("eid", eventId)
                    .list();
        }
    }

    public List<Registration> findByUser(Long userId) {
        try (Session s = sf.openSession()) {
            return s.createQuery(
                    "FROM Registration WHERE user.id = :uid ORDER BY registeredAt DESC", Registration.class)
                    .setParameter("uid", userId)
                    .list();
        }
    }

    public long countByEvent(Long eventId) {
        try (Session s = sf.openSession()) {
            return s.createQuery(
                    "SELECT COUNT(r) FROM Registration r WHERE r.event.id = :eid", Long.class)
                    .setParameter("eid", eventId)
                    .uniqueResult();
        }
    }

    public long countAttendedByEvent(Long eventId) {
        try (Session s = sf.openSession()) {
            return s.createQuery(
                    "SELECT COUNT(r) FROM Registration r WHERE r.event.id = :eid AND r.attended = true",
                    Long.class)
                    .setParameter("eid", eventId)
                    .uniqueResult();
        }
    }
}
