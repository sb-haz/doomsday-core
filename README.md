# DoomsdayCore - Advanced Rocket Warfare & Natural Disasters System

A comprehensive Minecraft plugin that adds realistic rocket warfare, defensive systems, nation territories, and dynamic natural disasters to your server.

## Features

### 🚀 Rocket System
- **Multiple Missile Types**: 5 different missile variants with unique behaviors
  - **R1 (Standard)**: Basic explosive missile with standard damage
  - **R2 (Bunker Buster)**: Penetrates deep underground with drilling explosions
  - **R3 (Cluster Bomb)**: Horizontal spread explosions in expanding waves
  - **R4 (Thermobaric)**: Powerful explosive with magma block placement
  - **R5 (Nuclear)**: Massive explosion with mushroom cloud, crater, fires, and flash effects

- **Realistic Flight Physics**: Missiles follow ballistic trajectories with configurable speed and arc
- **Visual Effects**: Smoke trails, particle effects, and sound effects
- **Collision Detection**: Smart collision system for terrain interaction

### 🛡️ Anti-Aircraft Defense System
- **Automated Defenses**: AI-controlled interceptor systems
- **Manual Defenses**: Player-operated systems requiring nearby operators
- **Configurable Parameters**: Range, accuracy, speed, and reload times
- **Real-time Interception**: Dynamic missile tracking and destruction
- **Visual Feedback**: Launch effects and status messages

### 🏗️ Block Reinforcement System
- **Reinforcement Powder**: Craft using Iron Ingot + Stone (shapeless recipe)
- **Block Protection**: Right-click blocks with powder to reinforce them
- **Configurable Resistance**: Different materials have different protection levels
- **Persistence**: Reinforcements saved across server restarts
- **Detection System**: Special helmet to visualize reinforced blocks

### 🔍 Detection & Visualization
- **Detector Helmet**: Wear to see reinforced blocks with pulsing particles
- **Range-based**: 16-block detection radius
- **Real-time Updates**: Continuous scanning while worn
- **Visual Indicators**: Particle effects on block faces

### 🎯 Advanced Explosion System
- **Multiple Explosion Types**: Each with unique effects and behaviors
- **Reinforcement Integration**: Blocks resist explosions based on reinforcement level
- **Protection Periods**: Temporary immunity during multi-wave explosions
- **Cleanup System**: Automatic removal of invalid reinforcements

### 🗺️ Nations System
- **4 Geographic Nations**: America, Europe, Africa, and Antarctica with defined borders
- **Realistic Borders**: Each nation occupies a 129x129 block area positioned geographically
- **Location Detection**: Automatic detection of which nation players are in
- **Interactive GUI**: Visual nation overview with real-time information
- **Teleportation**: Quick travel to nation centers via GUI or commands

### 🌪️ Natural Disasters System
- **Nation-Specific Disasters**: Each nation has unique disasters based on real geography
- **Dynamic Weather Events**: Randomly triggered disasters with configurable timing
- **Real Visual Effects**: Particles, sounds, environmental changes, and player effects
- **Configurable System**: Full control over disaster frequency, duration, and intensity
- **Environmental Impact**: Disasters actually affect the world (flooding, fires, drought)

### 🏆 Seasons System
- **Game Season Management**: Create, activate, and manage competitive game seasons
- **Automatic Duration**: Seasons default to 7-day periods with start/end tracking
- **Status Management**: Three season states - PLANNED, ACTIVE, ARCHIVED
- **Scoreboard Integration**: Active seasons display in real-time scoreboard with countdown
- **Admin Controls**: Full season lifecycle management through commands

#### **🇺🇸 America Disasters**
- **💫 Meteor Showers**: TNT meteors fall with flame trails, drop rare ores on impact
- **🔥 Wildfires**: Spreads fire through forests and grasslands during dry periods
- **🌪️ Tornadoes** (rare): Moving vortex that pulls players and creates chaos

#### **🇪🇺 Europe Disasters**
- **🌊 Flooding**: Rivers overflow, farmland gets wiped, realistic water spread
- **☠️ Plagues** (low frequency): Sickness spreads slowly causing hunger and weakness
- **⛈️ Storms**: Lightning strikes with fire chance and environmental damage

#### **🌍 Africa Disasters**
- **🏜️ Droughts**: Water sources dry up, crops fail, severe hunger effects
- **💨 Sandstorms**: Massive visibility reduction with blindness and slow movement

#### **🧊 Antarctica Disasters**
- **❄️ Blizzards**: Heavy snow, near-zero visibility, severe movement penalties
- **🧊 Ice Storms**: Freezing conditions, water turns to ice, cold damage

## Commands

### Main Commands
- `/rocket <missile_name>` or `/r <missile_name>` - Launch a missile
  - Available missiles: `r1`, `r2`, `r3`, `r4`, `r5`
- `/r reload` or `/r r` - Reload all configurations
- `/r options` or `/r o` - View current configuration
- `/r c` - Create testing environment with markers

### Item Commands
- `/r powder [amount]` or `/r p [amount]` - Get reinforcement powder (1-64)
- `/r helmet [amount]` or `/r h [amount]` - Get detector helmet (1-64)
- `/r items` or `/r i` - Reload custom items configuration

### Anti-Air Commands
- `/r aa status` - View all defense system status
- `/r aa reload` - Reload all anti-air defenses
- `/r aa online <defense_name>` - Bring defense system online
- `/r aa offline <defense_name>` - Take defense system offline

### Nations Commands
- `/r nation` or `/r n` - Check your current location and nation information
- `/r show` - Open interactive GUI (includes nations overview)

### Seasons Commands
- `/season current` or `/s current` - View current season information and status
- `/season create <id>` - Create new season with 7-day duration (admin only)
- `/season activate` - Activate planned season to make it live (admin only)
- `/season archive` - Archive current season when finished (admin only)

### Utility Commands
- `/cc [message]` - Send colored chat message (use & for color codes)
- `/rr` - Quick plugin reload (requires permission)

## Recipes

### Reinforcement Powder
**Type**: Shapeless Recipe
**Ingredients**: 
- 1x Iron Ingot
- 1x Stone
**Result**: 1x Reinforcement Powder

*Used to reinforce blocks against explosions. Right-click on valid blocks to apply.*

## Permissions

- `rocket.use` - Use basic rocket commands and view options (default: true)
- `rocket.reload` - Reload configurations and custom items (default: op)
- `rocket.powder` - Get reinforcement powder (default: op)
- `rocket.helmet` - Get detector helmets (default: op)
- `rocket.antiair` - Manage anti-air defense systems (default: op)
- `season.admin` - Create, activate, and archive seasons (default: op)
- `colorchat.use` - Use colored chat commands (default: true)

## Configuration

### Main Config (`config.yml`)
- **Sound Settings**: Configurable sound radius for explosions
- **Rocket Definitions**: Start/end coordinates, speed, arc, explosion type
- **Anti-Air Systems**: Defense locations, range, accuracy, operational status
- **Reinforcement Settings**: Valid blocks and resistance percentages
- **Nuclear Effects**: Flash radius, crater size, fire spread, cloud duration

### Custom Items (`custom_items.yml`)
- **Reinforcement Powder**: Material, model data, display name, lore
- **Detector Helmet**: Material, model data, display name, lore
- **Recipe Configuration**: Ingredients and crafting method

### Messages (`messages.yml`)
- **Missile Warnings**: Launch notifications and type announcements
- **Anti-Air Messages**: Launch, intercept, and failure notifications
- **Command Responses**: Success and error messages
- **Reload Confirmations**: Configuration reload status

### Nations Config (`nations.yml`)
- **Nation Definitions**: Borders, display names, and geographic positioning
- **Disaster Configuration**: Timing, probability, duration for each disaster type
- **Global Settings**: Announcement options, check intervals, debug mode

### Seasons Config (`seasons.yml`)
- **Season Storage**: Holds current season information (automatically managed)
- **Auto-generated**: Created when first season is made via commands
- **Backup Safe**: Contains full season metadata for recovery

## Block Resistance System

### Default Resistance (Non-Reinforced)
- **Obsidian**: 95% resistance
- **Bedrock**: 100% resistance (indestructible)

### Configurable Reinforced Block Resistance
Configure resistance percentages in `config.yml` under `reinforcement.resistance`:
```yaml
reinforcement:
  validBlocks:
    - STONE
    - COBBLESTONE
    - BRICKS
    # ... more blocks
  resistance:
    STONE: 0.50      # 50% chance to resist
    OBSIDIAN: 0.95   # 95% chance to resist
    CONCRETE: 0.60   # 60% chance to resist
```

## Anti-Aircraft Configuration

### Defense System Setup
```yaml
antiair:
  defenses:
    patriot_battery:
      displayName: "Patriot Battery"
      location:
        x: 100
        y: 70
        z: 200
      range: 150.0
      accuracy: 0.85
      interceptorSpeed: 3.0
      reloadTime: 5.0
      startupTime: 2.0
      automatic: true
```

### Parameters Explained
- **Range**: Maximum interception distance
- **Accuracy**: Probability of successful hit (0.0-1.0)
- **Interceptor Speed**: Speed of interceptor projectiles
- **Reload Time**: Cooldown between shots (seconds)
- **Startup Time**: Delay before firing (seconds)
- **Automatic**: If false, requires player within 5 blocks to operate

## Nuclear Explosion Effects

### Visual Effects
- **Nuclear Flash**: Blinding white flash with head shake for nearby players
- **Mushroom Cloud**: Massive static cloud structure with configurable duration
- **Shockwave Rings**: Expanding particle rings with knockback
- **Crater Formation**: Multi-phase explosion creating realistic bowl crater
- **Fire Spread**: Extensive fire placement across impact zone
- **Debris Field**: Scattered blocks and magma pools

### Configuration Options
```yaml
nuclear:
  effects:
    flashRadius: 100
    headShakeRadius: 60
    shockwaveRadius: 80
    fireSpreadRadius: 35
  crater:
    depth: 15
    radius: 25
  cloudDuration: 15        # seconds
  particleDuration: 6      # seconds
```

## Nations & Disasters Configuration

### Nation Boundaries
Each nation occupies a precise 129x129 block area positioned geographically:

- **🌍 Africa** (Center): X: -64 to 64, Z: -64 to 64
- **🇺🇸 America** (West): X: -256 to -128, Z: -64 to 64  
- **🇪🇺 Europe** (North): X: -64 to 64, Z: 64 to 192
- **🧊 Antarctica** (South): X: -64 to 64, Z: -192 to -64

### Disaster Configuration Example
```yaml
nations:
  america:
    displayName: "America"
    borders:
      minX: -256
      maxX: -128
      minZ: -64
      maxZ: 64
    disasters:
      meteor_showers:
        enabled: true
        minInterval: 2400    # 40 minutes in ticks
        maxInterval: 6000    # 100 minutes in ticks
        duration: 180        # 3 minutes duration
        probability: 0.2     # 20% chance when checked
        message: "&4Meteor shower detected over America!"
```

### Global Disaster Settings
```yaml
global:
  enableDisasters: true
  announceToAll: true          # Announce to all players vs nearby only
  announceRadius: 200          # Announce radius if announceToAll is false
  checkInterval: 600           # Check every 30 seconds (600 ticks)
  debug: false                 # Enable debug logging
```

### Interactive GUI Features
- **Nations Overview**: Click the "Nations & Disasters" button in `/r show`
- **Current Location**: See your exact coordinates and current nation
- **Active Disasters**: Real-time display of ongoing disasters with countdowns
- **Nation Teleportation**: Click any nation to teleport to its center
- **Geographic Layout**: Nations positioned in their real-world relative locations

## Scoreboard System

### Real-time Display
- **Dynamic Title**: Shows "DOOMSDAY S1" format when season is active
- **Season Dates**: Displays start and end dates in (MMM DD - MMM DD) format  
- **Season Countdown**: Shows time remaining until season ends (e.g., "Ends in: 3d 12h")
- **Nation Counts**: Live online player count for each nation with color coding
- **Server Info**: Shows play.doomsdayCore.gg server address
- **Clean Layout**: Only appears when a season is active, otherwise shows minimal display
- **Performance Optimized**: Uses centralized nation player tracking for instant updates

### Nation Colors
- **America**: Blue
- **Europe**: Green  
- **Africa**: Gold
- **Asia**: Red
- **Antarctica**: White

## Technical Features

### Performance Optimized
- **Concurrent Data Structures**: Thread-safe reinforcement storage and nation player tracking
- **Cleanup Systems**: Automatic removal of invalid data
- **Efficient Particle Systems**: Optimized visual effects
- **Configurable Limits**: Adjustable ranges and durations
- **In-Memory Nation Tracking**: Real-time player-to-nation mapping for instant lookups

### Data Persistence
- **Reinforced Blocks**: Saved to `reinforced_blocks.yml`
- **Block Validation**: Automatic cleanup of invalid locations
- **Cross-restart Compatibility**: Data survives server restarts

### Advanced Physics
- **Ballistic Trajectories**: Realistic missile flight paths
- **Collision Detection**: Smart terrain interaction
- **Anti-Air Interception**: Dynamic pursuit algorithms
- **Explosion Mechanics**: Multiple damage and effect systems

### Disaster Effects Engine
- **Real Environmental Impact**: Floods add water, droughts remove it, fires spread naturally
- **Version Compatibility**: Works across different Minecraft/Bukkit versions
- **Particle Systems**: Rich visual effects with particles, sounds, and environmental changes  
- **Player Effects**: Potion effects, movement changes, health impact based on disaster type
- **Cleanup Systems**: Automatic restoration when disasters end (floods recede, etc.)

### Centralized Nation Player Tracking
- **In-Memory Cache**: Real-time tracking of which online players are in which nations
- **Event-Driven Updates**: Automatically maintains accuracy through player join/quit/nation change events
- **Performance Optimized**: Eliminates repeated scanning of all online players
- **Thread-Safe**: Uses `ConcurrentHashMap` for multi-threaded server environments
- **Instant Lookups**: O(1) access to online player counts and nation memberships

#### Tracking System Usage

The plugin automatically maintains an in-memory cache of online players by nation. This system:

**Initialization:**
- Builds cache on plugin startup by scanning all currently online players
- Creates empty tracking sets for all configured nations

**Automatic Updates:**
- **Player Join**: Adds player to their nation's online set (if they have one)
- **Player Quit**: Removes player from all tracking
- **Nation Changes**: Updates tracking when players join/leave nations
- **Server Restart**: Rebuilds cache from current online players

**API Methods for Developers:**
```java
// Get all online players in a specific nation
Set<UUID> playersInAmerica = nationPlayerManager.getOnlinePlayersInNation("america");

// Get online player count for a nation
int onlineCount = nationPlayerManager.getOnlinePlayerCountInNation("africa");

// Get counts for all nations (used by scoreboard)
Map<String, Integer> allCounts = nationPlayerManager.getOnlinePlayerCountsByNation();

// Quick nation lookup for online players
String playerNation = nationPlayerManager.getOnlinePlayerNation(playerId);
```

**Current Usage in Plugin:**
- **Scoreboard**: Displays real-time online/total citizen counts (e.g., "America: 5, Europe: 3")
- **Messaging System**: Sends missiles/disasters/anti-air alerts only to online nation members
- **Nation GUIs**: Shows "Online/Total" format (e.g., "Citizens: 5/12")
- **Broadcasting**: Efficiently targets specific nations without scanning all players

**Performance Benefits:**
- **Before**: Each scoreboard update scanned all online players (expensive)
- **After**: Instant lookup from cached data structure
- **Before**: Each nation message iterated through all players
- **After**: Direct access to target nation's player set

## Installation

1. Download the plugin JAR file
2. Place in your server's `plugins` folder
3. Start/restart your server
4. Configure settings in the generated config files:
   - `config.yml` - Main plugin settings, rockets, anti-air defenses
   - `nations.yml` - Nation boundaries and disaster configurations
   - `custom_items.yml` - Reinforcement items and recipes
5. Set up rocket coordinates and anti-air positions
6. Configure nation boundaries and disaster settings as desired
7. Grant appropriate permissions to players

## Development Info

**Package**: `gg.doomsdayCore.core`
**Main Class**: `DoomsdayCore.java`
**Version**: 2.0.0

## Latest Updates (v2.0.0)

### 🗺️ Nations System
- Added 4 geographic nations with realistic borders (129x129 blocks each)
- Interactive GUI for nation overview and teleportation
- Real-time location detection and nation information

### 🌪️ Natural Disasters
- **12 Different Disasters** across all nations with unique effects:
  - America: Meteor showers, wildfires, tornadoes
  - Europe: Flooding, plagues, storms  
  - Africa: Droughts, sandstorms
  - Antarctica: Blizzards, ice storms
- **Real Environmental Impact**: Waters dry up, fires spread, floods occur
- **Player Effects**: Movement changes, vision effects, health impact
- **Configurable System**: Full control over timing, intensity, and frequency

### 🎮 Enhanced GUI
- Nations overview with real-time disaster status
- Click-to-teleport functionality
- Geographic nation layout matching real-world positions
- Active disaster countdown timers

---

*This plugin provides a complete warfare and survival simulation system for Minecraft servers, featuring realistic ballistics, defensive systems, dynamic weather disasters, and spectacular visual effects.*