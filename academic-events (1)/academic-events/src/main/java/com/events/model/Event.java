package com.events.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private LocalDateTime dateTime;

    @Column(nullable = false, length = 300)
    private String location;

    @Column(nullable = false)
    private int maxCapacity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventStatus status;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public Event() {
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null)
            createdAt = LocalDateTime.now();
        if (status == null)
            status = EventStatus.DRAFT;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public String getLocation() {
        return location;
    }

    public int getMaxCapacity() {
        return maxCapacity;
    }

    public EventStatus getStatus() {
        return status;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setId(Long v) {
        this.id = v;
    }

    public void setTitle(String v) {
        this.title = v;
    }

    public void setDescription(String v) {
        this.description = v;
    }

    public void setDateTime(LocalDateTime v) {
        this.dateTime = v;
    }

    public void setLocation(String v) {
        this.location = v;
    }

    public void setMaxCapacity(int v) {
        this.maxCapacity = v;
    }

    public void setStatus(EventStatus v) {
        this.status = v;
    }

    public void setCreatedBy(User v) {
        this.createdBy = v;
    }

    public void setCreatedAt(LocalDateTime v) {
        this.createdAt = v;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String title, description, location;
        private LocalDateTime dateTime;
        private int maxCapacity;
        private EventStatus status;
        private User createdBy;

        public Builder title(String v) {
            this.title = v;
            return this;
        }

        public Builder description(String v) {
            this.description = v;
            return this;
        }

        public Builder dateTime(LocalDateTime v) {
            this.dateTime = v;
            return this;
        }

        public Builder location(String v) {
            this.location = v;
            return this;
        }

        public Builder maxCapacity(int v) {
            this.maxCapacity = v;
            return this;
        }

        public Builder status(EventStatus v) {
            this.status = v;
            return this;
        }

        public Builder createdBy(User v) {
            this.createdBy = v;
            return this;
        }

        public Event build() {
            Event e = new Event();
            e.title = title;
            e.description = description;
            e.dateTime = dateTime;
            e.location = location;
            e.maxCapacity = maxCapacity;
            e.status = status;
            e.createdBy = createdBy;
            return e;
        }
    }
}
