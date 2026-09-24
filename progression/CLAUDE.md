# Progression Module

Implements the professions system. Players level up gathering professions (fishing, mining, woodcutting) by performing the associated activity, unlocking passive skills and bonuses as they progress.

## Package Map
```
progression/
└── me.mykindos.betterpvp.progression/
    ├── Progression.java            # Main plugin class
    ├── ProgressionsManager.java    # Top-level manager
    ├── commands/                   # /profession commands
    ├── database/                   # Progression repositories
    ├── event/                      # Progression events
    ├── injector/                   # Guice module
    ├── item/                       # Profession-specific items
    ├── leaderboards/               # Profession leaderboards
    ├── listener/                   # Module-level listeners
    ├── profession/                 # Core profession framework
    │   ├── IProfession.java        # Profession interface
    │   ├── ProfessionHandler.java  # Manages all professions
    │   ├── ProfessionRepository.java
    │   ├── skill/                  # Profession skill base classes
    │   ├── fishing/                # Fishing profession
    │   ├── mining/                 # Mining profession
    │   └── woodcutting/            # Woodcutting profession
    ├── profile/                    # Per-player progression profile
    ├── settings/                   # Player settings for professions
    ├── tips/                       # In-game tips
    └── utility/                    # Utility helpers
```

## Core Concepts

### `IProfession`
Interface each profession implements. Defines XP gain triggers, level thresholds, and associated skills.

### Professions
| Profession | Trigger |
|---|---|
| Fishing | Catching fish |
| Mining | Breaking ore blocks |
| Woodcutting | Chopping wood |

Each profession has its own subdirectory with:
- A main class implementing `IProfession`
- Skill implementations (passive bonuses unlocked at levels)
- A repository for persisting XP data

### Profession Skills
Distinct from Champions skills: these are passive perks unlocked as a player levels a profession. Defined under each profession's `skill/` or inline subdirectory. Base classes live in `profession/skill/`.

### `ProfessionHandler`
Manages all registered professions. Used to look up a player's profession data, grant XP, and check levels.

### Player Profile
`profile/` holds the per-player data object tracking XP and level per profession. Loaded on join, persisted to DB via `ProfessionRepository`.

## Adding a New Profession
1. Create a class implementing `IProfession` under a new subdirectory in `profession/`
2. Add a repository for persistence
3. Register with `ProfessionHandler` via Guice
4. Add a listener for the relevant Bukkit event to grant XP

## Database Migrations
`progression/src/main/resources/progression-migrations/postgres/`
