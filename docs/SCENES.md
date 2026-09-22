# Scenes

Scenes are the shared way BetterPvP places and manages server-owned objects in the world: NPCs, modeled props, text displays, item displays, and similar content. A scene is not a separate world or map format. It is the collection of objects a module decides to load for a world or area, plus the rules for when those objects should be refreshed.

The goal is to keep map content predictable. A module can say "load the hub NPCs from Mapper points" or "reload shops NPCs after ModelEngine finishes rebuilding models" without each NPC listener having to manage its own cleanup, reload timing, or registry bookkeeping.

## How Scenes Work

Scene content starts with a `SceneObject`. This is the base wrapper around a Bukkit entity. The wrapper has a stable scene id, knows which entity represents it in the world, and knows how to remove itself cleanly. NPCs, props, text displays, and item displays are all scene objects.

Some scene objects are also `SceneEntity` objects. Scene entities can carry behaviors and child entities. NPCs and props use this path because they often need extra per-tick logic, attached nameplates, animations, or visual helpers that should disappear when the parent object is removed.

When a scene object is spawned, it is usually registered in `SceneObjectRegistry`. The registry lets the rest of the system find the scene object behind a clicked entity or an object id. That is how the interaction listener can turn a player right-click on an NPC's backing entity into a call on the correct NPC object.

World content owns groups of scene objects. A `WorldContent` reads an external source, such as Mapper data-points, and creates the objects for one world at a time. The `WorldContentService` tracks what each piece of content put into each world, and removes exactly that when the world unloads or the content is reloaded. That gives scene content a simple replacement model: reloads rebuild the content instead of trying to patch old objects in place, and one world loading never rebuilds another.

### Behaviors

Behaviors are small reusable pieces of logic attached to a scene entity. They let a model or NPC gain extra behavior without putting every feature directly into the object class.

Examples include:

- showing a nameplate or tag attached to a model bone
- running an animation sequence
- reacting to ModelEngine script keyframes
- patrolling between waypoints

Each behavior can start, tick, and stop. The scene ticker calls attached behaviors every server tick. When the owning scene entity is removed, its behaviors are stopped automatically, so temporary displays or script hooks can clean themselves up with the object they belong to.

### Object Types

- `SceneObject`: the base managed object. It wraps one Bukkit entity and handles common initialization, registration, and removal.
- `SceneEntity`: a scene object that can have behaviors and attached child entities. NPCs and props extend this.
- `NPC`: an interactive scene entity. Player right-clicks are routed to the NPC through the shared scene interaction listener.
- `Prop`: a non-player scene entity for static or decorative world content. Props can still have behaviors, but they are not generally player-interactive.
- `ModeledNPC` and `ModeledProp`: NPCs and props backed by ModelEngine models.
- `SceneTextDisplay`: a managed text display entity.
- `SceneItemDisplay`: a managed item display entity.

Factories create command-spawnable object types, especially NPCs and props. World content can also construct objects directly when they are specific to one place.

### World Content

Content lives in `core/world/content/`. A module declares a `WorldContentBinding`: which worlds the content belongs to (a `WorldSelector`) and the content itself. It registers the binding with `WorldContentService`, naming the plugin that owns it.

```java
contentService.register(plugin, new WorldContentBinding(WorldSelector.named(BPvPWorld.MAIN_WORLD_NAME),
        () -> List.of(content)).withRequiresModels(true));
```

The service drives it off the server lifecycle:

- Nothing is installed until the server has started, because Mapper data is only readable by then.
- A world that loads gets every binding that matches it. A world that unloads loses exactly its own content.
- `withRequiresModels(true)` holds content back until ModelEngine has registered its models, and applies it again after every ModelEngine reload. Use this for modeled NPCs and props.
- Reloading a plugin re-applies only that plugin's bindings. `withOnReload` runs first, for content that caches what it read.
- Disabling a plugin removes everything it registered.

A `WorldContent` implements whichever of these fit:

- `zones(world, regions)`: capability zones.
- `sceneObjects(world, regions)`: chunk-managed scene objects, which spawn their body when their chunk loads and come back after it unloads.
- `install(world, regions, scope)`: anything else. Eagerly spawned objects (`scope.spawn`, `scope.adopt`), things with their own teardown (`scope.onRelease`), or content that adds to the world after it has loaded, since content may keep its scope.

`FactorySpawnPoints` covers the common case of spawning a factory's objects at data-points named `<prefix>:<type>`.

## Scene Package File Hierarchy

The shared scene framework lives under:

```text
core/src/main/java/me/mykindos/betterpvp/core/scene/
|-- SceneObject.java
|-- SceneEntity.java
|-- SceneObjectRegistry.java
|-- SceneObjectFactory.java
|-- SceneObjectFactoryManager.java
|-- HasModeledEntity.java
|-- behavior/
|-- command/
|-- controller/
|-- display/
|-- listener/
|-- npc/
`-- prop/
```

Major files and folders:

- `SceneObject.java`: base lifecycle for any managed scene object. It binds to one Bukkit entity, exposes the scene id, and removes the backing entity when the object is removed.
- `SceneEntity.java`: adds behavior support and child-entity cleanup for scene objects that need ongoing logic or attached visuals.
- `SceneObjectRegistry.java`: global registry of active scene objects. Used for lookup by id, Bukkit entity, or entity UUID.
- `SceneObjectFactory.java`: base factory for command-spawnable scene objects.
- `SceneObjectFactoryManager.java`: stores registered scene object factories by factory name.
- `HasModeledEntity.java`: marker for objects backed by a ModelEngine modeled entity, so shared behaviors can operate on their model.
- `behavior/`: reusable logic that can be attached to scene entities, such as bone tags, animation sequences, ModelEngine script handling, and waypoint patrols.
- `command/`: admin commands for scene NPCs, including spawn, list, and remove.
- `controller/`: background scene controllers. The ticker drives behavior ticks, and cleanup removes registered scene objects when needed.
- `display/`: managed Bukkit display wrappers such as `SceneTextDisplay` and `SceneItemDisplay`.
- `listener/`: shared event routing, especially player interaction with registered scene objects.
- `npc/`: NPC base types, NPC factories, human/model-backed NPC support, and player-list visibility handling.
- `prop/`: prop base types and factories for non-interactive scene content.

Module-specific content lives in the owning modules, for example:

```text
hub/src/main/java/me/mykindos/betterpvp/hub/feature/npc/HubSceneContent.java
clans/src/main/java/me/mykindos/betterpvp/clans/scene/ClansSceneContent.java
shops/src/main/java/me/mykindos/betterpvp/shops/npc/ShopsSceneContent.java
```

Those files translate module content into scene objects. They choose Mapper data-point names, create the right NPC or display classes, register the relevant factory, and choose which worlds the content belongs to.
