package com.events.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "registrations",
        uniqueConstraints = @UniqueConstraint(columnNames = {"event_id", "user_id"}))
public class Registration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, unique = true, length = 100)
    private String token;

    @Column(nullable = false)
    private boolean attended = false;

    private LocalDateTime attendedAt;

    @Column(nullable = false)
    private LocalDateTime registeredAt;

    public Registration() {}

    @PrePersist
    protected void onCreate() {
        if (registeredAt == null) registeredAt = LocalDateTime.now();
    }

    // Getters
    public Long          getId()           { return id; }
    public Event         getEvent()        { return event; }
    public User          getUser()         { return user; }
    public String        getToken()        { return token; }
    public boolean       isAttended()      { return attended; }
    public LocalDateTime getAttendedAt()   { return attendedAt; }
    public LocalDateTime getRegisteredAt() { return registeredAt; }

    // Setters
    public void setId(Long v)                  { this.id = v; }
    public void setEvent(Event v)              { this.event = v; }
    public void setUser(User v)                { this.user = v; }
    public void setToken(String v)             { this.token = v; }
    public void setAttended(boolean v)         { this.attended = v; }
    public void setAttendedAt(LocalDateTime v) { this.attendedAt = v; }
    public void setRegisteredAt(LocalDateTime v){ this.registeredAt = v; }

    // Builder
    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private Event   event;
        private User    user;
        private String  token;
        private boolean attended = false;

        public Builder event(Event v)      { this.event = v;    return this; }
        public Builder user(User v)        { this.user = v;     return this; }
        public Builder token(String v)     { this.token = v;    return this; }
        public Builder attended(boolean v) { this.attended = v; return this; }

        public Registration build() {
            Registration r = new Registration();
            r.event = event; r.user = user;
            r.token = token; r.attended = attended;
            return r;
        }
    }
}
