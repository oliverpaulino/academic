package com.events.service;

import com.events.model.Event;
import com.events.model.EventStatus;
import com.events.model.Role;
import com.events.model.User;
import com.events.repository.EventRepository;
import com.events.repository.RegistrationRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class EventService {

    private static final EventRepository eventRepo = new EventRepository();
    private static final RegistrationRepository regRepo = new RegistrationRepository();

    public static Event create(String title, String description, LocalDateTime dateTime,
                               String location, int maxCapacity, User creator) {
        return eventRepo.save(Event.builder()
            .title(title).description(description)
            .dateTime(dateTime).location(location)
            .maxCapacity(maxCapacity)
            .status(EventStatus.DRAFT)
            .createdBy(creator)
            .build());
    }

    public static Event update(Long eventId, String title, String description,
                               LocalDateTime dateTime, String location, int maxCapacity,
                               Long requestUserId) {
        Event event = eventRepo.findById(eventId)
            .orElseThrow(() -> new IllegalArgumentException("Evento no encontrado"));
        User requester = UserService.findById(requestUserId).orElseThrow();
        if (!event.getCreatedBy().getId().equals(requestUserId) && requester.getRole() != Role.ADMIN)
            throw new SecurityException("No autorizado para editar este evento");
        if (event.getStatus() == EventStatus.CANCELLED)
            throw new IllegalArgumentException("No se puede editar un evento cancelado");
        event.setTitle(title); event.setDescription(description);
        event.setDateTime(dateTime); event.setLocation(location);
        event.setMaxCapacity(maxCapacity);
        return eventRepo.update(event);
    }

    public static Event setStatus(Long eventId, EventStatus newStatus, Long requestUserId) {
        Event event = eventRepo.findById(eventId)
            .orElseThrow(() -> new IllegalArgumentException("Evento no encontrado"));
        User requester = UserService.findById(requestUserId).orElseThrow();
        if (!event.getCreatedBy().getId().equals(requestUserId) && requester.getRole() != Role.ADMIN)
            throw new SecurityException("No autorizado");
        event.setStatus(newStatus);
        return eventRepo.update(event);
    }

    public static void delete(Long id)                         { eventRepo.delete(id); }
    public static Optional<Event> findById(Long id)            { return eventRepo.findById(id); }
    public static List<Event> findAll()                        { return eventRepo.findAll(); }
    public static List<Event> findAllPublished()               { return eventRepo.findByStatus(EventStatus.PUBLISHED); }
    public static List<Event> findByCreator(Long userId)       { return eventRepo.findByCreator(userId); }

    public static boolean hasCapacity(Long eventId) {
        Event e = eventRepo.findById(eventId).orElseThrow();
        return regRepo.countByEvent(eventId) < e.getMaxCapacity();
    }
}
