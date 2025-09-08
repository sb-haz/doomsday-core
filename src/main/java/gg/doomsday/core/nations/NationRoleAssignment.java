package gg.doomsday.core.nations;

import java.time.Instant;
import java.util.UUID;

public class NationRoleAssignment {
    private final UUID playerId;
    private final String playerName;
    private final NationRole role;
    private final String assignmentMethod; // "CLAIMED" or "ASSIGNED"
    private final Instant assignedAt;

    public NationRoleAssignment(UUID playerId, String playerName, NationRole role, String assignmentMethod) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.role = role;
        this.assignmentMethod = assignmentMethod;
        this.assignedAt = Instant.now();
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public NationRole getRole() {
        return role;
    }

    public String getAssignmentMethod() {
        return assignmentMethod;
    }

    public Instant getAssignedAt() {
        return assignedAt;
    }

    public boolean wasClaimed() {
        return "CLAIMED".equals(assignmentMethod);
    }

    public boolean wasAssigned() {
        return "ASSIGNED".equals(assignmentMethod);
    }

    @Override
    public String toString() {
        return String.format("%s (%s) - %s [%s]", 
            playerName, playerId, role.getDisplayName(), assignmentMethod);
    }
}