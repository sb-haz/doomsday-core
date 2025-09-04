package gg.doomsday.core.seasons;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Represents a game season with its metadata and status
 */
public class Season {
    
    public enum Status {
        PLANNED, ACTIVE, ARCHIVED
    }
    
    private final int id;
    private final String displayName;
    private Status status;
    private final Instant startAt;
    private final Instant endAt;
    
    public Season(int id, String displayName, Status status, Instant startAt, Instant endAt) {
        this.id = id;
        this.displayName = displayName;
        this.status = status;
        this.startAt = startAt;
        this.endAt = endAt;
    }
    
    // Getters
    public int getId() { return id; }
    public String getDisplayName() { return displayName; }
    public Status getStatus() { return status; }
    public Instant getStartAt() { return startAt; }
    public Instant getEndAt() { return endAt; }
    
    // Status management
    public void setStatus(Status status) { this.status = status; }
    
    // Utility methods
    public boolean isActive() { return status == Status.ACTIVE; }
    public boolean isArchived() { return status == Status.ARCHIVED; }
    public boolean isPlanned() { return status == Status.PLANNED; }
    
    public boolean hasEnded() {
        return endAt != null && Instant.now().isAfter(endAt);
    }
    
    public boolean hasStarted() {
        return startAt != null && Instant.now().isAfter(startAt);
    }
    
    public long getTimeUntilEnd() {
        if (endAt == null) return -1;
        return endAt.toEpochMilli() - System.currentTimeMillis();
    }
    
    public long getTimeUntilStart() {
        if (startAt == null) return -1;
        return startAt.toEpochMilli() - System.currentTimeMillis();
    }
    
    public String getStartAtFormatted() {
        if (startAt == null) return "Not set";
        return startAt.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "Z";
    }
    
    public String getEndAtFormatted() {
        if (endAt == null) return "Not set";
        return endAt.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "Z";
    }
    
    /**
     * Validates if this season can be activated
     */
    public boolean canActivate() {
        return status == Status.PLANNED && 
               hasStarted() &&
               (endAt == null || !hasEnded());
    }
    
    /**
     * Validates if this season is in a valid state for an active season
     */
    public boolean isValidActive() {
        return status == Status.ACTIVE && 
               (endAt == null || !hasEnded());
    }
    
    @Override
    public String toString() {
        return "Season{id=" + id + ", displayName='" + displayName + "', status=" + status + 
               ", startAt=" + getStartAtFormatted() + ", endAt=" + getEndAtFormatted() + "}";
    }
}