# NOT ANOTHER TOPDOWN SHOOTER

A fast-paced, action-packed top-down shooter game built with LibGDX. Battle through waves of intelligent AI enemies, collect powerful weapons, and survive intense combat scenarios in this exciting arcade-style shooter.

## Table of Contents
- [Features](#features)
- [Game Modes](#game-modes)
- [Weapons Arsenal](#weapons-arsenal)
- [Game Mechanics](#game-mechanics)
- [Controls](#controls)
- [Maps](#maps)
- [Requirements](#requirements)
- [Building from Source](#building-from-source)
- [Running the Game](#running-the-game)
- [Project Structure](#project-structure)
- [Technical Details](#technical-details)

## Features

### Core Gameplay
- **Intense Combat**: Fast-paced top-down shooting action with smooth controls
- **Smart AI Enemies**: Enemies with pathfinding AI that chase, flank, and engage tactically
- **Multiple Weapons**: Three distinct weapon types with unique characteristics
- **Health & Ammo System**: Manage your resources carefully to survive
- **Special Abilities**: Slow-motion mode for tactical advantage in heated battles
- **Progressive Difficulty**: Increasing challenge as you advance through levels

### Game Systems
- **Kill/Death Tracking**: Monitor your performance with detailed statistics
- **Collectible Items**: Health packs, ammo boxes, and colored keys
- **Environmental Obstacles**: Boxes, barrels, cactus, plants, and logs for tactical cover
- **Trap System**: Avoid deadly traps scattered throughout levels
- **Level Completion**: Collect keys and reach the exit to progress
- **Custom Maps**: Support for user-created map files

### Audio & Visuals
- **Immersive Sound Effects**: Weapon sounds, reload effects, and environmental audio
- **Dynamic Music**: Adaptive music system for menu and gameplay
- **Animated Sprites**: Smooth character animations for movement, shooting, and reloading
- **Multiple UI Skins**: Star Soldier and Comic themed UI styles

## Game Modes

### Single Player
Battle through custom-designed levels solo. Collect keys, defeat enemies, and reach the exit to complete each level.

**Features:**
- Choose from pre-made maps (Default, Arena, Tutorial)
- Load custom maps from your filesystem
- Pause and resume gameplay
- Track kills and performance

### Multiplayer
Team up or compete with other players in networked multiplayer battles.

**Features:**
- Host or join multiplayer sessions
- Real-time networked gameplay
- Multiplayer-specific screens and controls
- Shared world with other players

## Weapons Arsenal

### Pistol
Your reliable sidearm with balanced stats.
- **Damage**: 15 per shot
- **Magazine Size**: 10 rounds
- **Total Ammo**: 100 rounds
- **Fire Rate**: 0.1s between shots
- **Reload Time**: 1 second
- **Best For**: General combat, ammunition conservation

### Shotgun
Devastating close-range power with spread shot.
- **Damage**: 34 per pellet (3 pellets per shot = 102 total)
- **Magazine Size**: 3 shells
- **Total Ammo**: 15 shells
- **Fire Rate**: 0.5s between shots
- **Reload Time**: 2 seconds
- **Special**: Fires 3 projectiles in a spread pattern
- **Best For**: Close-quarters combat, multiple enemies

### Rifle
High-damage precision weapon for skilled players.
- **Damage**: 50 per shot
- **Magazine Size**: 1 round
- **Total Ammo**: 10 rounds
- **Fire Rate**: 0.4s between shots
- **Reload Time**: 1 second
- **Best For**: Long-range engagements, high-value targets

## Game Mechanics

### Combat System
- **Health Points**: Player and enemies start with 100 HP
- **Bullet Physics**: Projectiles travel at realistic speeds (600 units/sec)
- **Damage System**: Different weapons deal varying damage
- **Line of Sight**: Enemies detect and engage within detection radius (300 units)
- **Shooting Range**: Enemies open fire within 192 units

### Enemy AI Behavior
Enemies feature sophisticated AI with multiple states:
- **Patrol Mode**: Random wandering when no target detected
- **Chase Mode**: Pursue player when within detection radius
- **Combat Mode**: Stop and engage when in shooting range
- **Pathfinding**: Navigate around obstacles using A* pathfinding

### Collectible Items

#### Health Pack
Restores player health points.

#### Ammo Pack
Replenishes ammunition for your active weapon.

#### Colored Keys
Four types of keys (Red, Green, Blue, Yellow) required for level progression and unlocking areas.

#### Traps
Environmental hazards that damage players who trigger them.

### Special Abilities

#### Slow-Motion Mode
- **Activation**: Press SPACEBAR
- **Effect**: Slows down time for tactical advantage
- **Meter System**: Limited duration based on slow meter
- **Recharge**: Meter regenerates over time

## Controls

### Movement
- **W** - Move Up
- **A** - Move Left
- **S** - Move Down
- **D** - Move Right
- **W+A/W+D/S+A/S+D** - Diagonal movement

### Combat
- **Mouse Aim** - Aim your weapon
- **Left Click** - Shoot
- **R** - Reload (manual) or Auto-reload when empty
- **1** - Switch to Pistol
- **2** - Switch to Shotgun
- **3** - Switch to Rifle

### Special Actions
- **SPACEBAR** - Toggle Slow-Motion mode
- **ESC** - Pause game / Open menu

### Menu Navigation
- **Mouse** - Navigate menus and select options

## Maps

The game includes three built-in maps:

### Default.map
The standard level for learning game mechanics and controls.

### arena.map
A combat-focused arena for intense battles.

### Tutorial.map
An introductory level teaching basic gameplay.

### Custom Maps
You can create and load custom maps using the level editor:
1. Design your level as a grid-based text file
2. Use numeric codes for different tile types:
   - `0` - Ground
   - `1` - Wall
   - `2` - End Level
   - `3` - Box
   - `4` - Ammo Pack
   - `5` - Health Pack
   - `6` - Yellow Key
   - `7` - Red Key
   - `8` - Blue Key
   - `9` - Green Key
   - `13` - Trap
   - Additional codes for barrels, cactus, plants, logs
3. Save as `.map` file
4. Load through "Choose Level" option in game menu

## Requirements

### System Requirements
- **OS**: Windows, Linux, or macOS
- **Java**: JDK 8 or higher
- **RAM**: 512 MB minimum
- **Storage**: 100 MB available space
- **Graphics**: OpenGL 2.0+ compatible

### Development Requirements
- **Gradle**: 8.14 (included via wrapper)
- **Java**: JDK 8 or higher
- **LibGDX**: 1.9.6

## Building from Source

### Prerequisites
Ensure you have Java JDK 8+ installed on your system.

### Build Steps

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd IGPEGame
   ```

2. **Make gradlew executable** (Linux/macOS)
   ```bash
   chmod +x gradlew
   ```

3. **Build the project**
   ```bash
   # Linux/macOS
   ./gradlew build

   # Windows
   gradlew.bat build
   ```

4. **Create distribution JAR**
   ```bash
   # Linux/macOS
   ./gradlew dist

   # Windows
   gradlew.bat dist
   ```

The standalone JAR will be created at:
```
desktop/build/libs/desktop-1.0.jar
```

### Build Tasks
- `./gradlew build` - Compile and build all modules
- `./gradlew dist` - Create standalone distribution JAR
- `./gradlew clean` - Clean build artifacts
- `./gradlew desktop:run` - Run the game directly
- `./gradlew desktop:debug` - Run in debug mode

## Running the Game

### From Standalone JAR
After building, run the distribution JAR:
```bash
java -jar desktop/build/libs/desktop-1.0.jar
```

### From Source (Development)
Run directly without building JAR:
```bash
# Linux/macOS
./gradlew desktop:run

# Windows
gradlew.bat desktop:run
```

### Running with Custom Maps
Place your custom `.map` files in the same directory as the JAR, or use the in-game "Choose Level" option to browse for map files.

## Project Structure

```
IGPEGame/
├── core/                      # Core game logic (platform-independent)
│   ├── src/
│   │   └── it/unical/igpe/
│   │       ├── ai/           # Enemy AI and pathfinding
│   │       ├── game/         # Main game class
│   │       ├── GUI/          # User interface and screens
│   │       │   ├── screens/  # Game screens (menu, gameplay, etc.)
│   │       │   ├── HUD/      # Heads-up display
│   │       │   └── Assets.java
│   │       ├── logic/        # Game objects (Player, Enemy, Weapon)
│   │       ├── MapUtils/     # Map loading and world management
│   │       ├── net/          # Multiplayer networking
│   │       └── utils/        # Utilities and configuration
│   └── assets/               # Game assets (images, sounds, skins)
│       ├── Audio/           # Sound effects and music
│       ├── player/          # Player sprite animations
│       ├── enemy/           # Enemy sprite animations
│       └── skin/            # UI skins and themes
├── desktop/                  # Desktop launcher
│   ├── src/                 # Desktop-specific code
│   ├── Default.map          # Default game map
│   ├── arena.map            # Arena map
│   └── Tutorial.map         # Tutorial map
├── build.gradle             # Root Gradle build configuration
└── gradle/                  # Gradle wrapper files
```

## Technical Details

### Architecture
- **Game Framework**: LibGDX 1.9.6
- **Language**: Java 8
- **Build System**: Gradle 8.14
- **Graphics API**: OpenGL
- **Physics**: Custom collision detection
- **AI**: A* pathfinding algorithm
- **Networking**: LibGDX networking for multiplayer

### Key Classes

#### Core Game
- `IGPEGame` - Main game class and entry point
- `ScreenManager` - Manages game screen transitions
- `GameConfig` - Global configuration constants

#### Game Objects
- `Player` - Player character with weapons and abilities
- `Enemy` - AI-controlled enemy with pathfinding
- `Weapon` - Weapon system with unique characteristics
- `Bullet` - Projectile physics and collision
- `Tile` - Environmental objects and obstacles
- `Lootable` - Collectible items (health, ammo, keys)

#### Map System
- `World` - Game world container and update logic
- `WorldLoader` - Loads maps from .map files
- `WorldRenderer` - Renders game world and entities

#### AI System
- `EnemyManager` - Manages enemy spawning and behavior
- Pathfinding implementation for intelligent enemy movement

### Performance
- **Target FPS**: 60
- **Resolution**: 854x480 (windowed), configurable fullscreen
- **Background Resolution**: 1920x1080

### Configuration
Edit `GameConfig.java` to modify:
- Screen resolution
- Movement speeds
- Enemy AI parameters
- Weapon statistics
- Audio volumes

## Credits

### Development
- **Framework**: LibGDX (https://libgdx.com/)
- **University**: Università della Calabria (UNICAL)
- **Department**: IGPE (Interactive Game Programming and Engineering)

### Assets
Game assets include sprite animations for players and enemies, UI skins, sound effects, and music.

## License

This project was created for educational purposes at Università della Calabria.

---

**Enjoy the game! Report issues and contribute improvements to make it even better!**
