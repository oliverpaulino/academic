package com.events.repository;

import com.events.config.HibernateUtil;
import com.events.model.Event;
import com.events.model.EventStatus;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import java.util.List;
import java.util.Optional;

public class EventRepository {

    private final SessionFactory sf = HibernateUtil.getSessionFactory();

    public Event save(Event event) {
        try (Session s = sf.openSession()) {
            Transaction tx = s.beginTransaction();
            s.persist(event);
            tx.commit();
            return event;
        }
    }

    public Event update(Event event) {
        try (Session s = sf.openSession()) {
            Transaction tx = s.beginTransaction();
            Event merged = s.merge(event);
            tx.commit();
            return merged;
        }
    }

    public void delete(Long id) {
        try (Session s = sf.openSession()) {
            Transaction tx = s.beginTransaction();
            Event e = s.get(Event.class, id);
            if (e != null) s.remove(e);
            tx.commit();
        }
    }

    public Optional<Event> findById(Long id) {
        try (Session s = sf.openSession()) {
            return Optional.ofNullable(s.get(Event.class, id));
        }
    }

    public List<Event> findAll() {
        try (Session s = sf.openSession()) {
            return s.createQuery("FROM Event ORDER BY dateTime DESC", Event.class).list();
        }
    }

    public List<Event> findByStatus(EventStatus status) {
        try (Session s = sf.openSession()) {
            return s.createQuery("FROM Event WHERE status = :st ORDER BY dateTime ASC", Event.class)
                    .setParameter("st", status)
                    .list();
        }
    }

    public List<Event> findByCreator(Long userId) {
        try (Session s = sf.openSession()) {
            return s.createQuery(
                    "FROM Event WHERE createdBy.id = :uid ORDER BY dateTime DESC", Event.class)
                    .setParameter("uid", userId)
                    .list();
        }
    }
}
