package gg.doomsday.core.nations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Nation {
    private final String id;
    private final String displayName;
    private final NationBorders borders;
    private final Map<String, Disaster> disasters;
    private int totalPlayers;
    private double centerX;
    private double centerZ;
    private List<String> missileTypes;

    public Nation(String id, String displayName, NationBorders borders) {
        this.id = id;
        this.displayName = displayName;
        this.borders = borders;
        this.disasters = new HashMap<>();
        this.totalPlayers = 0;
        this.centerX = 0;
        this.centerZ = 0;
        this.missileTypes = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public NationBorders getBorders() {
        return borders;
    }

    public Map<String, Disaster> getDisasters() {
        return disasters;
    }

    public void addDisaster(String id, Disaster disaster) {
        disasters.put(id, disaster);
    }

    public boolean containsLocation(double x, double y, double z) {
        return borders.contains(x, y, z);
    }

    public int getTotalPlayers() {
        return totalPlayers;
    }

    public void setTotalPlayers(int totalPlayers) {
        this.totalPlayers = totalPlayers;
    }

    public double getCenterX() {
        return centerX;
    }

    public void setCenterX(double centerX) {
        this.centerX = centerX;
    }

    public double getCenterZ() {
        return centerZ;
    }

    public void setCenterZ(double centerZ) {
        this.centerZ = centerZ;
    }

    public List<String> getMissileTypes() {
        return new ArrayList<>(missileTypes);
    }

    public void setMissileTypes(List<String> missileTypes) {
        this.missileTypes = new ArrayList<>(missileTypes);
    }

    public void addMissileType(String missileType) {
        if (!missileTypes.contains(missileType)) {
            missileTypes.add(missileType);
        }
    }

    @Override
    public String toString() {
        return "Nation{id='" + id + "', displayName='" + displayName + "', borders=" + borders + 
               ", totalPlayers=" + totalPlayers + ", center=(" + centerX + "," + centerZ + ")}";
    }
}