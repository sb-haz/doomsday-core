package gg.doomsday.core.nations;

public enum NationRole {
    PRESIDENT("President", "&4", 1, true),
    ARMYCHIEF("ArmyChief", "&c", 2, true),
    SOLDIER("Soldier", "&6", 3, false),
    BUILDER("Builder", "&a", 4, false),
    HEALER("Healer", "&d", 5, false),
    TRADER("Trader", "&e", 6, false),
    CITIZEN("Citizen", "&7", 7, false);

    private final String displayName;
    private final String defaultColorCode;
    private final int priority;
    private final boolean singleSlot;

    NationRole(String displayName, String defaultColorCode, int priority, boolean singleSlot) {
        this.displayName = displayName;
        this.defaultColorCode = defaultColorCode;
        this.priority = priority;
        this.singleSlot = singleSlot;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDefaultColorCode() {
        return defaultColorCode;
    }

    public String getColoredName() {
        return defaultColorCode + displayName;
    }

    public String getColoredName(String colorCode) {
        return (colorCode != null ? colorCode : defaultColorCode) + displayName;
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