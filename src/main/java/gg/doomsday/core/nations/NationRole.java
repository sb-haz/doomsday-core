package gg.doomsday.core.nations;

public enum NationRole {
    PRESIDENT("President", "&8", 1, true),
    ARMYCHIEF("ArmyChief", "&8", 2, true),
    SOLDIER("Soldier", "&8", 3, false),
    BUILDER("Builder", "&8", 4, false),
    HEALER("Healer", "&8", 5, false),
    TRADER("Trader", "&8", 6, false),
    CITIZEN("Citizen", "&8", 7, false);

    private final String displayName;
    private final String colorCode;
    private final int priority;
    private final boolean singleSlot;

    NationRole(String displayName, String colorCode, int priority, boolean singleSlot) {
        this.displayName = displayName;
        this.colorCode = colorCode;
        this.priority = priority;
        this.singleSlot = singleSlot;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getColorCode() {
        return colorCode;
    }

    public String getColoredName() {
        return colorCode + displayName;
    }

    public int getPriority() {
        return priority;
    }

    public boolean isSingleSlot() {
        return singleSlot;
    }

    public static NationRole fromString(String roleName) {
        if (roleName == null) return CITIZEN;
        
        for (NationRole role : values()) {
            if (role.displayName.equalsIgnoreCase(roleName) || 
                role.name().equalsIgnoreCase(roleName)) {
                return role;
            }
        }
        return CITIZEN;
    }

    public static NationRole[] getClaimableRoles() {
        return new NationRole[]{PRESIDENT, ARMYCHIEF, SOLDIER, BUILDER, HEALER, TRADER};
    }
}