package gg.doomsday.core.nations;

public class Disaster {
    private final String id;
    private final boolean enabled;
    private final int minInterval;
    private final int maxInterval;
    private final int duration;
    private final double probability;
    private final String message;
    private long lastOccurrence;
    private long nextCheck;
    private boolean active;
    private long endTime;

    public Disaster(String id, boolean enabled, int minInterval, int maxInterval, 
                   int duration, double probability, String message) {
        this.id = id;
        this.enabled = enabled;
        this.minInterval = minInterval;
        this.maxInterval = maxInterval;
        this.duration = duration;
        this.probability = probability;
        this.message = message;
        this.lastOccurrence = 0;
        this.nextCheck = System.currentTimeMillis();
        this.active = false;
        this.endTime = 0;
    }

    public String getId() { return id; }
    public boolean isEnabled() { return enabled; }
    public int getMinInterval() { return minInterval; }
    public int getMaxInterval() { return maxInterval; }
    public int getDuration() { return duration; }
    public double getProbability() { return probability; }
    public String getMessage() { return message; }
    
    public long getLastOccurrence() { return lastOccurrence; }
    public void setLastOccurrence(long lastOccurrence) { this.lastOccurrence = lastOccurrence; }
    
    public long getNextCheck() { return nextCheck; }
    public void setNextCheck(long nextCheck) { this.nextCheck = nextCheck; }
    
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    
    public long getEndTime() { return endTime; }
    public void setEndTime(long endTime) { this.endTime = endTime; }

    public void scheduleNext() {
        int interval = minInterval + (int) (Math.random() * (maxInterval - minInterval));
        long intervalMillis = interval * 50L; // Convert ticks to milliseconds (1 tick = 50ms)
        nextCheck = System.currentTimeMillis() + intervalMillis;
    }

    public void start() {
        active = true;
        lastOccurrence = System.currentTimeMillis();
        endTime = lastOccurrence + (duration * 50L); // Convert ticks to milliseconds
        scheduleNext();
    }

    public void end() {
        active = false;
        endTime = 0;
    }

    @Override
    public String toString() {
        return "Disaster{id='" + id + "', enabled=" + enabled + 
               ", minInterval=" + minInterval + ", maxInterval=" + maxInterval + 
               ", probability=" + probability + "}";
    }
}