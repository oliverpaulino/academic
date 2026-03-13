package com.events.service;

import com.events.model.Event;
import com.events.model.EventStatus;
import com.events.model.Registration;
import com.events.model.User;
import com.events.repository.RegistrationRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class RegistrationService {

    private static final RegistrationRepository repo = new RegistrationRepository();

    public static Registration register(Long eventId, Long userId) {
        Event event = EventService.findById(eventId)
            .orElseThrow(() -> new IllegalArgumentException("Evento no encontrado"));
        User user = UserService.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        if (event.getStatus() != EventStatus.PUBLISHED)
            throw new IllegalArgumentException("El evento no está publicado");
        if (event.getDateTime().isBefore(LocalDateTime.now()))
            throw new IllegalArgumentException("No puedes inscribirte a un evento pasado");
        if (repo.findByEventAndUser(eventId, userId).isPresent())
            throw new IllegalArgumentException("Ya estás inscrito en este evento");
        if (!EventService.hasCapacity(eventId))
            throw new IllegalArgumentException("El evento está lleno");

        return repo.save(Registration.builder()
            .event(event).user(user)
            .token(UUID.randomUUID().toString())
            .attended(false)
            .build());
    }

    public static void cancel(Long eventId, Long userId) {
        Registration reg = repo.findByEventAndUser(eventId, userId)
            .orElseThrow(() -> new IllegalArgumentException("Inscripción no encontrada"));
        if (reg.getEvent().getDateTime().isBefore(LocalDateTime.now()))
            throw new IllegalArgumentException("No puedes cancelar después de la fecha del evento");
        repo.delete(reg.getId());
    }

    public static Registration markAttendance(String token) {
        Registration reg = repo.findByToken(token)
            .orElseThrow(() -> new IllegalArgumentException("Token QR inválido"));
        if (reg.isAttended())
            throw new IllegalArgumentException("La asistencia ya fue marcada");
        reg.setAttended(true);
        reg.setAttendedAt(LocalDateTime.now());
        return repo.update(reg);
    }

    public static Optional<Registration> findByToken(String token)              { return repo.findByToken(token); }
    public static Optional<Registration> findByEventAndUser(Long e, Long u)     { return repo.findByEventAndUser(e, u); }
    public static List<Registration>     findByEvent(Long eventId)              { return repo.findByEvent(eventId); }
    public static List<Registration>     findByUser(Long userId)                { return repo.findByUser(userId); }
    public static long countByEvent(Long eventId)                               { return repo.countByEvent(eventId); }
    public static long countAttendedByEvent(Long eventId)                       { return repo.countAttendedByEvent(eventId); }
}
