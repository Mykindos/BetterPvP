# Scenes

Scenes are the shared way BetterPvP places and manages server-owned objects in the world: NPCs, modeled props, text displays, item displays, and similar content. A scene is not a separate world or map format. It is the collection of objects a module decides to load for a world or area, plus the rules for when those objects should be refreshed.

The goal is to keep map content predictable. A module can say "load the hub NPCs from Mapper points" or "reload shops NPCs after ModelEngine finishes rebuilding models" without each NPC listener having to manage its own cleanup, reload timing, or registry bookkeeping.

## How Scenes Work

Scene content starts with a `SceneObject`. This is the base wrapper around a Bukkit entity. The wrapper has a stable scene id, knows which entity represents it in the world, and knows how to remove itself cleanly. NPCs, props, text displays, and item displays are all scene objects.

Some scene objects are also `SceneEntity` objects. Scene entities can carry behaviors and child entities. NPCs and props use this path because they often need extra per-tick logic, attached nameplates, animations, or visual helpers that should disappear when the parent object is removed.

When a scene object is spawned, it is usually registered in `SceneObjectRegistry`. The registry lets the rest of the system find the scene object behind a clicked entity or an object id. That is how the interaction listener can turn a player right-click on an NPC's backing entity into a call on the correct NPC object.

Loaders own groups of scene objects. A loader reads an external source, such as Mapper data-points, creates the objects, and tracks them. When the loader reloads, the base loader removes every tracked object first, then loads the current set again. That gives scene content a simple replacement model: reloads rebuild the scene instead of trying to patch old objects in place.

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

Factories create command-spawnable object types, especially NPCs and props. Loaders can also construct objects directly when the content is specific to one scene.

### Loading Strategies

A loader declares when it should load by returning one or more load strategies. Strategies are the triggers; loaders are the content builders.

The built-in strategies are:

- `OnEnableLoadStrategy`: loads once as soon as the loader is registered during plugin enable. Use this for simple content that does not need world startup or ModelEngine completion.
- `ServerStartLoadStrategy`: loads once after the server start event. Use this when the world and Mapper data need to be ready, but custom ModelEngine models are not involved.
- `ModelEngineLoadStrategy`: reloads when ModelEngine finishes registering models. Use this for modeled NPCs and props so they are rebuilt after the initial ModelEngine pipeline and after ModelEngine reloads.
- `ModuleReloadLoadStrategy`: reloads when the owning module reloads. This is usually paired with another initial-load strategy so module reloads refresh the same scene content.

Most module scenes today use Mapper data-points plus `ModelEngineLoadStrategy` and `ModuleReloadLoadStrategy`. Mapper provides the positions and orientation. ModelEngine tells the loader when custom models are safe to spawn. Module reload gives operators a way to rebuild the scene without restarting.

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
|-- loader/
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
- `loader/`: scene loading lifecycle. `SceneObjectLoader` owns tracked objects, `MapperSceneLoader` reads Mapper regions, `SceneLoaderManager` binds loaders, and load strategies define reload timing.
- `npc/`: NPC base types, NPC factories, human/model-backed NPC support, and player-list visibility handling.
- `prop/`: prop base types and factories for non-interactive scene content.

Module-specific scene loaders live in the owning modules, for example:

```text
hub/src/main/java/me/mykindos/betterpvp/hub/feature/npc/HubSceneLoader.java
clans/src/main/java/me/mykindos/betterpvp/clans/scene/ClansSceneLoader.java
shops/src/main/java/me/mykindos/betterpvp/shops/npc/ShopsSceneLoader.java
```

Those files are responsible for translating module content into scene objects. For example, they choose Mapper data-point names, create the right NPC or display classes, register the relevant factory, and declare the load strategies that fit the module.
