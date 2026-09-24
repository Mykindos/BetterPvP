# Lunar Module

Provides integration with the Lunar Client API. Lunar Client is a popular Minecraft client that exposes a server-side API for custom UI elements, waypoints, notifications, and cosmetics.

## Package Map
```
lunar/
└── me.mykindos.betterpvp.lunar/
    ├── Lunar.java          # Main plugin class
    ├── commands/           # Lunar-related admin commands
    ├── injector/           # Guice module
    └── listener/           # Listeners for Lunar Client events and Bukkit events
```

## Purpose
- Sends custom waypoints to players using Lunar Client
- Displays server-side titles/notifications via Lunar Client UI
- Can detect if a player is using Lunar Client and enable/disable features accordingly
- May bridge Lunar cosmetics or client-side mods with server state

## Notes
- Small, self-contained module: minimal cross-module dependencies
- Relies on the Lunar Client Plugin Messaging API (custom channel packets)
- Any Lunar-specific feature requests should live in `listener/` as a new listener class
