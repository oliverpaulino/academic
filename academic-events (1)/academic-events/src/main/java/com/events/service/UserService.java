package com.events.service;

import com.events.model.Role;
import com.events.model.User;
import com.events.repository.UserRepository;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private static final UserRepository repo = new UserRepository();

    public static void initAdminUser() {
        if (repo.findByUsername("admin").isEmpty()) {
            User admin = User.builder()
                .username("admin")
                .email("admin@pucmm.edu.do")
                .password(BCrypt.hashpw("Admin123!", BCrypt.gensalt()))
                .role(Role.ADMIN)
                .blocked(false)
                .deletable(false)
                .build();
            repo.save(admin);
            log.info("Admin creado → usuario: admin | contraseña: Admin123!");
        }
    }

    public static User register(String username, String email, String rawPassword) {
        if (repo.findByUsername(username).isPresent())
            throw new IllegalArgumentException("El nombre de usuario ya está en uso");
        if (repo.findByEmail(email).isPresent())
            throw new IllegalArgumentException("El correo ya está registrado");

        return repo.save(User.builder()
            .username(username)
            .email(email)
            .password(BCrypt.hashpw(rawPassword, BCrypt.gensalt()))
            .role(Role.PARTICIPANT)
            .blocked(false)
            .deletable(true)
            .build());
    }

    public static Optional<User> login(String username, String rawPassword) {
        return repo.findByUsername(username)
            .filter(u -> !u.isBlocked())
            .filter(u -> BCrypt.checkpw(rawPassword, u.getPassword()));
    }

    public static Optional<User> findById(Long id)    { return repo.findById(id); }
    public static List<User>     findAll()             { return repo.findAll(); }

    public static User setBlocked(Long userId, boolean blocked) {
        User u = repo.findById(userId).orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        if (!u.isDeletable()) throw new IllegalArgumentException("No se puede modificar el usuario del sistema");
        u.setBlocked(blocked);
        return repo.update(u);
    }

    public static User setRole(Long userId, Role role) {
        User u = repo.findById(userId).orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        if (!u.isDeletable()) throw new IllegalArgumentException("No se puede cambiar el rol del usuario del sistema");
        u.setRole(role);
        return repo.update(u);
    }
}
