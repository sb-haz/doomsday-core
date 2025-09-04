package gg.doomsday.core.nations;

public class NationBorders {
    private final int minX;
    private final int maxX;
    private final int minZ;
    private final int maxZ;
    private final int minY;
    private final int maxY;

    public NationBorders(int minX, int maxX, int minZ, int maxZ, int minY, int maxY) {
        this.minX = minX;
        this.maxX = maxX;
        this.minZ = minZ;
        this.maxZ = maxZ;
        this.minY = minY;
        this.maxY = maxY;
    }

    public boolean contains(double x, double y, double z) {
        return x >= minX && x <= maxX && 
               z >= minZ && z <= maxZ && 
               y >= minY && y <= maxY;
    }

    public int getMinX() { return minX; }
    public int getMaxX() { return maxX; }
    public int getMinZ() { return minZ; }
    public int getMaxZ() { return maxZ; }
    public int getMinY() { return minY; }
    public int getMaxY() { return maxY; }

    public int getCenterX() { return (minX + maxX) / 2; }
    public int getCenterZ() { return (minZ + maxZ) / 2; }
    
    @Override
    public String toString() {
        return "NationBorders{" +
               "x=" + minX + " to " + maxX + 
               ", z=" + minZ + " to " + maxZ + 
               ", y=" + minY + " to " + maxY + '}';
    }
}