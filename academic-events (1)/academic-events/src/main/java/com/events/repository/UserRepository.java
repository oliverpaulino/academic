package com.events.repository;

import com.events.config.HibernateUtil;
import com.events.model.Role;
import com.events.model.User;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import java.util.List;
import java.util.Optional;

public class UserRepository {

    private final SessionFactory sf = HibernateUtil.getSessionFactory();

    public User save(User user) {
        try (Session s = sf.openSession()) {
            Transaction tx = s.beginTransaction();
            s.persist(user);
            tx.commit();
            return user;
        }
    }

    public User update(User user) {
        try (Session s = sf.openSession()) {
            Transaction tx = s.beginTransaction();
            User merged = s.merge(user);
            tx.commit();
            return merged;
        }
    }

    public Optional<User> findById(Long id) {
        try (Session s = sf.openSession()) {
            return Optional.ofNullable(s.get(User.class, id));
        }
    }

    public Optional<User> findByUsername(String username) {
        try (Session s = sf.openSession()) {
            return s.createQuery("FROM User WHERE username = :u", User.class)
                    .setParameter("u", username)
                    .uniqueResultOptional();
        }
    }

    public Optional<User> findByEmail(String email) {
        try (Session s = sf.openSession()) {
            return s.createQuery("FROM User WHERE email = :e", User.class)
                    .setParameter("e", email)
                    .uniqueResultOptional();
        }
    }

    public List<User> findAll() {
        try (Session s = sf.openSession()) {
            return s.createQuery("FROM User ORDER BY createdAt DESC", User.class).list();
        }
    }
}
