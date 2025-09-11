package gg.doomsday.core.nations;

import gg.doomsday.core.data.PlayerDataManager;
import gg.doomsday.core.seasons.Season;
import gg.doomsday.core.seasons.SeasonManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class NationRoleManager {
    private final JavaPlugin plugin;
    private final NationPlayerManager nationPlayerManager;
    private final SeasonManager seasonManager;
    private final PlayerDataManager playerDataManager;
    
    private FileConfiguration rolesConfig;
    private File rolesFile;
    
    // Cache for role assignments: Nation -> Role -> List<NationRoleAssignment>
    private final Map<String, Map<NationRole, List<NationRoleAssignment>>> roleAssignments = new ConcurrentHashMap<>();
    
    // Cache for player role lookups: PlayerId -> (Nation, Role)
    private final Map<UUID, NationRoleAssignment> playerRoleCache = new ConcurrentHashMap<>();
    
    // Configuration cache
    private final Map<String, Map<NationRole, Integer>> nationRoleSlots = new ConcurrentHashMap<>();
    private final Map<String, Double> donorWeights = new ConcurrentHashMap<>();
    private final Map<NationRole, String> roleColors = new ConcurrentHashMap<>();
    private int claimWindowMinutes = 60;
    private boolean weightedAssignment = true;

    public NationRoleManager(JavaPlugin plugin, NationPlayerManager nationPlayerManager, SeasonManager seasonManager, PlayerDataManager playerDataManager) {
        this.plugin = plugin;
        this.nationPlayerManager = nationPlayerManager;
        this.seasonManager = seasonManager;
        this.playerDataManager = playerDataManager;
        loadConfiguration();
        loadRoleAssignments();
    }

    private void loadConfiguration() {
        rolesFile = new File(plugin.getDataFolder(), "roles.yml");
        
        if (!rolesFile.exists()) {
            plugin.saveResource("roles.yml", false);
        }
        
        rolesConfig = YamlConfiguration.loadConfiguration(rolesFile);
        
        // Load settings
        claimWindowMinutes = rolesConfig.getInt("settings.claim-window-minutes", 60);
        weightedAssignment = rolesConfig.getBoolean("settings.weighted-assignment", true);
        
        // Load donor weights
        donorWeights.clear();
        ConfigurationSection weightsSection = rolesConfig.getConfigurationSection("donor-weights");
        if (weightsSection != null) {
            for (String rank : weightsSection.getKeys(false)) {
                donorWeights.put(rank.toLowerCase(), weightsSection.getDouble(rank, 1.0));
            }
        }
        
        // Load nation role slots
        nationRoleSlots.clear();
        ConfigurationSection nationsSection = rolesConfig.getConfigurationSection("nations");
        if (nationsSection != null) {
            for (String nationId : nationsSection.getKeys(false)) {
                Map<NationRole, Integer> roleSlots = new HashMap<>();
                ConfigurationSection nationSection = nationsSection.getConfigurationSection(nationId);
                
                if (nationSection != null) {
                    for (NationRole role : NationRole.getClaimableRoles()) {
                        int slots = nationSection.getInt(role.getDisplayName(), 0);
                        roleSlots.put(role, slots);
                    }
                }
                
                nationRoleSlots.put(nationId, roleSlots);
            }
        }
        
        // Load role colors
        roleColors.clear();
        ConfigurationSection roleInfoSection = rolesConfig.getConfigurationSection("role-info");
        if (roleInfoSection != null) {
            for (NationRole role : NationRole.values()) {
                String colorPath = role.getDisplayName() + ".color";
                String color = roleInfoSection.getString(colorPath, role.getDefaultColorCode());
                roleColors.put(role, color);
            }
        } else {
            // Use default colors if no role-info section exists
            for (NationRole role : NationRole.values()) {
                roleColors.put(role, role.getDefaultColorCode());
            }
        }
        
        plugin.getLogger().info("Loaded role configuration - Claim window: " + claimWindowMinutes + " minutes, Role colors: " + roleColors.size());
    }

    private void saveConfiguration() {
        try {
            rolesConfig.save(rolesFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save roles.yml: " + e.getMessage());
        }
    }

    private void loadRoleAssignments() {
        roleAssignments.clear();
        playerRoleCache.clear();
        
        // Initialize empty role assignment maps for each nation
        for (String nationId : nationRoleSlots.keySet()) {
            Map<NationRole, List<NationRoleAssignment>> nationRoles = new HashMap<>();
            for (NationRole role : NationRole.values()) {
                nationRoles.put(role, new ArrayList<>());
            }
            roleAssignments.put(nationId, nationRoles);
        }
        
        // Load existing role assignments from player data files
        File playerDataDir = new File(plugin.getDataFolder(), "player_data");
        if (playerDataDir.exists() && playerDataDir.isDirectory()) {
            File[] playerFiles = playerDataDir.listFiles((dir, name) -> name.endsWith(".yml"));
            if (playerFiles != null) {
                for (File playerFile : playerFiles) {
                    try {
                        String fileName = playerFile.getName();
                        String uuidString = fileName.substring(0, fileName.length() - 4);
                        UUID playerId = UUID.fromString(uuidString);
                        
                        PlayerDataManager.PlayerData playerData = playerDataManager.getPlayerData(playerId);
                        NationRole role = playerData.getCurrentRole();
                        String nationId = playerData.getCurrentNation();
                        
                        // Only load non-citizen roles and players with nations
                        if (role != NationRole.CITIZEN && !nationId.isEmpty() && roleAssignments.containsKey(nationId)) {
                            String playerName = playerData.getLastKnownUsername();
                            String assignedBy = playerData.getAssignedBy();
                            
                            NationRoleAssignment assignment = new NationRoleAssignment(playerId, playerName, role, assignedBy);
                            
                            // Add to nation role assignments
                            roleAssignments.get(nationId).get(role).add(assignment);
                            
                            // Add to player cache
                            playerRoleCache.put(playerId, assignment);
                        }
                        
                    } catch (Exception e) {
                        plugin.getLogger().warning("Failed to load role assignment from file: " + playerFile.getName() + " - " + e.getMessage());
                    }
                }
            }
        }
        
        plugin.getLogger().info("Loaded role assignments for " + roleAssignments.size() + " nations, " + playerRoleCache.size() + " players with roles");
    }

    public boolean isClaimWindowActive() {
        Season currentSeason = seasonManager.getCurrentSeason();
        if (currentSeason == null || !currentSeason.isActive()) {
            return false;
        }
        
        Instant seasonStart = currentSeason.getStartAt();
        if (seasonStart == null) {
            return false;
        }
        
        Duration elapsed = Duration.between(seasonStart, Instant.now());
        return elapsed.toMinutes() <= claimWindowMinutes;
    }

    public long getClaimWindowRemainingMinutes() {
        Season currentSeason = seasonManager.getCurrentSeason();
        if (currentSeason == null || !currentSeason.isActive()) {
            return 0;
        }
        
        Instant seasonStart = currentSeason.getStartAt();
        if (seasonStart == null) {
            return 0;
        }
        
        Duration elapsed = Duration.between(seasonStart, Instant.now());
        long remaining = claimWindowMinutes - elapsed.toMinutes();
        return Math.max(0, remaining);
    }

    public boolean canClaimRole(Player player, NationRole role) {
        if (!isClaimWindowActive()) {
            return false;
        }
        
        String playerNation = nationPlayerManager.getPlayerNation(player.getUniqueId());
        if (playerNation == null) {
            return false;
        }
        
        // Check if player already has a role
        if (hasRole(player.getUniqueId())) {
            return false;
        }
        
        // Check if role slots are available
        return getAvailableSlots(playerNation, role) > 0;
    }

    public boolean claimRole(Player player, NationRole role) {
        if (!canClaimRole(player, role)) {
            return false;
        }
        
        String playerNation = nationPlayerManager.getPlayerNation(player.getUniqueId());
        return assignRole(player.getUniqueId(), player.getName(), playerNation, role, "CLAIMED");
    }

    public boolean assignRole(UUID playerId, String playerName, String nationId, NationRole role, String method) {
        if (nationId == null || role == null) {
            return false;
        }
        
        // Remove player from any existing role first
        removePlayerRole(playerId, false);
        
        // Check if slots are available
        if (getAvailableSlots(nationId, role) <= 0) {
            return false;
        }
        
        // Save role to persistent player data
        playerDataManager.assignPlayerRole(playerId, role, method);
        
        // Update nation assignment if needed
        playerDataManager.setPlayerNation(playerId, nationId);
        
        // Create assignment for in-memory tracking
        NationRoleAssignment assignment = new NationRoleAssignment(playerId, playerName, role, method);
        
        // Add to nation role assignments
        roleAssignments.get(nationId).get(role).add(assignment);
        
        // Add to player cache
        playerRoleCache.put(playerId, assignment);
        
        plugin.getLogger().info("Assigned role " + role.getDisplayName() + " to " + playerName + " in " + nationId + " (" + method + ")");
        return true;
    }

    public boolean removePlayerRole(UUID playerId, boolean log) {
        // Get current role from persistent data
        PlayerDataManager.PlayerData playerData = playerDataManager.getPlayerData(playerId);
        NationRole currentRole = playerData.getCurrentRole();
        
        if (currentRole == NationRole.CITIZEN) {
            return false; // Already a citizen, nothing to remove
        }
        
        // Remove role from persistent data
        playerDataManager.removePlayerRole(playerId, "ADMIN");
        
        // Remove from in-memory cache
        NationRoleAssignment currentAssignment = playerRoleCache.remove(playerId);
        
        // Find player's nation and remove from role assignments
        String playerNation = nationPlayerManager.getPlayerNation(playerId);
        if (playerNation != null && roleAssignments.containsKey(playerNation)) {
            // Remove from role assignments
            List<NationRoleAssignment> roleList = roleAssignments.get(playerNation).get(currentRole);
            roleList.removeIf(assignment -> assignment.getPlayerId().equals(playerId));
        }
        
        if (log) {
            String playerName = currentAssignment != null ? currentAssignment.getPlayerName() : "Unknown";
            plugin.getLogger().info("Removed role " + currentRole.getDisplayName() + " from " + playerName);
        }
        
        return true;
    }

    public boolean hasRole(UUID playerId) {
        PlayerDataManager.PlayerData playerData = playerDataManager.getPlayerData(playerId);
        return playerData.getCurrentRole() != NationRole.CITIZEN;
    }

    public NationRole getPlayerRole(UUID playerId) {
        // Get role from persistent player data
        PlayerDataManager.PlayerData playerData = playerDataManager.getPlayerData(playerId);
        return playerData.getCurrentRole();
    }

    public NationRoleAssignment getPlayerRoleAssignment(UUID playerId) {
        return playerRoleCache.get(playerId);
    }

    public int getAvailableSlots(String nationId, NationRole role) {
        Map<NationRole, Integer> nationSlots = nationRoleSlots.get(nationId);
        if (nationSlots == null) {
            return 0;
        }
        
        int maxSlots = nationSlots.getOrDefault(role, 0);
        int currentAssignments = roleAssignments.get(nationId).get(role).size();
        
        return Math.max(0, maxSlots - currentAssignments);
    }

    public List<NationRoleAssignment> getRoleAssignments(String nationId, NationRole role) {
        Map<NationRole, List<NationRoleAssignment>> nationRoles = roleAssignments.get(nationId);
        if (nationRoles == null) {
            return new ArrayList<>();
        }
        
        return new ArrayList<>(nationRoles.getOrDefault(role, new ArrayList<>()));
    }

    public Map<NationRole, List<NationRoleAssignment>> getAllRoleAssignments(String nationId) {
        Map<NationRole, List<NationRoleAssignment>> nationRoles = roleAssignments.get(nationId);
        if (nationRoles == null) {
            return new HashMap<>();
        }
        
        return new HashMap<>(nationRoles);
    }

    public void performRandomAssignment() {
        if (isClaimWindowActive()) {
            plugin.getLogger().warning("Cannot perform random assignment while claim window is active");
            return;
        }
        
        plugin.getLogger().info("Starting random role assignment for unfilled slots...");
        
        for (String nationId : nationRoleSlots.keySet()) {
            performRandomAssignmentForNation(nationId);
        }
        
        plugin.getLogger().info("Random role assignment completed");
    }

    private void performRandomAssignmentForNation(String nationId) {
        // Get all players in nation without roles
        Set<UUID> nationPlayers = nationPlayerManager.getAllPlayersInNation(nationId);
        List<UUID> unassignedPlayers = nationPlayers.stream()
            .filter(playerId -> !hasRole(playerId))
            .collect(Collectors.toList());
        
        if (unassignedPlayers.isEmpty()) {
            return;
        }
        
        plugin.getLogger().info("Assigning " + unassignedPlayers.size() + " unassigned players in " + nationId);
        
        // Get all roles with available slots (ordered by priority)
        List<NationRole> rolesWithSlots = Arrays.stream(NationRole.getClaimableRoles())
            .filter(role -> getAvailableSlots(nationId, role) > 0)
            .sorted(Comparator.comparing(NationRole::getPriority))
            .collect(Collectors.toList());
        
        // Assign roles using weighted random selection
        Random random = new Random();
        
        for (NationRole role : rolesWithSlots) {
            int availableSlots = getAvailableSlots(nationId, role);
            
            for (int slot = 0; slot < availableSlots && !unassignedPlayers.isEmpty(); slot++) {
                UUID selectedPlayer = selectWeightedRandomPlayer(unassignedPlayers, random);
                if (selectedPlayer != null) {
                    OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(selectedPlayer);
                    assignRole(selectedPlayer, offlinePlayer.getName(), nationId, role, "ASSIGNED");
                    unassignedPlayers.remove(selectedPlayer);
                }
            }
        }
        
        // Remaining players become Citizens (if any are left)
        plugin.getLogger().info("Random assignment completed for " + nationId + ". " + unassignedPlayers.size() + " players remain as Citizens");
    }

    private UUID selectWeightedRandomPlayer(List<UUID> players, Random random) {
        if (players.isEmpty()) {
            return null;
        }
        
        if (!weightedAssignment) {
            return players.get(random.nextInt(players.size()));
        }
        
        // Calculate weights for all players
        Map<UUID, Double> playerWeights = new HashMap<>();
        double totalWeight = 0.0;
        
        for (UUID playerId : players) {
            double weight = calculatePlayerWeight(playerId);
            playerWeights.put(playerId, weight);
            totalWeight += weight;
        }
        
        // Select random player based on weight
        double randomValue = random.nextDouble() * totalWeight;
        double cumulativeWeight = 0.0;
        
        for (Map.Entry<UUID, Double> entry : playerWeights.entrySet()) {
            cumulativeWeight += entry.getValue();
            if (randomValue <= cumulativeWeight) {
                return entry.getKey();
            }
        }
        
        // Fallback to random selection
        return players.get(random.nextInt(players.size()));
    }

    private double calculatePlayerWeight(UUID playerId) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerId);
        
        // Base weight
        double weight = 1.0;
        
        // Check for donor ranks (would need permission system integration)
        Player onlinePlayer = player.getPlayer();
        if (onlinePlayer != null) {
            for (Map.Entry<String, Double> donorEntry : donorWeights.entrySet()) {
                if (onlinePlayer.hasPermission("doomsday.donor." + donorEntry.getKey())) {
                    weight = Math.max(weight, donorEntry.getValue());
                }
            }
        }
        
        return weight;
    }

    public void resetAllRoles() {
        plugin.getLogger().info("Resetting all nation roles for new season");
        
        roleAssignments.clear();
        playerRoleCache.clear();
        
        // Reinitialize empty role assignments
        loadRoleAssignments();
    }

    public void onSeasonStart() {
        resetAllRoles();
        plugin.getLogger().info("Season started - Role claim window is now active for " + claimWindowMinutes + " minutes");
    }

    public void onSeasonEnd() {
        plugin.getLogger().info("Season ended - Clearing all role assignments");
        resetAllRoles();
    }

    public void onClaimWindowEnd() {
        if (!isClaimWindowActive()) {
            plugin.getLogger().info("Claim window ended - Starting random role assignment");
            performRandomAssignment();
        }
    }

    public void reload() {
        loadConfiguration();
        plugin.getLogger().info("Nation role system reloaded");
    }

    // Admin methods
    public boolean adminAssignRole(UUID playerId, String playerName, NationRole role) {
        String playerNation = nationPlayerManager.getPlayerNation(playerId);
        if (playerNation == null) {
            return false;
        }
        
        return assignRole(playerId, playerName, playerNation, role, "ADMIN");
    }

    public boolean adminRemoveRole(UUID playerId) {
        return removePlayerRole(playerId, true);
    }

    public Map<String, Integer> getRoleSlots(String nationId) {
        Map<NationRole, Integer> slots = nationRoleSlots.get(nationId);
        if (slots == null) {
            return new HashMap<>();
        }
        
        Map<String, Integer> result = new HashMap<>();
        for (Map.Entry<NationRole, Integer> entry : slots.entrySet()) {
            result.put(entry.getKey().getDisplayName(), entry.getValue());
        }
        
        return result;
    }

    public String getRoleColor(NationRole role) {
        return roleColors.getOrDefault(role, role.getDefaultColorCode());
    }
}