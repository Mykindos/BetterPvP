---
name: new-champion-skill
description: >
  Scaffold a new Champions skill in the BetterPvP repository. Use this skill when the user asks to
  create, add, or implement a new skill for a champion role (Assassin, Knight, Brute, Ranger, Mage,
  Warlock) or a global skill. Also trigger when the user describes a skill mechanic and asks to
  implement it, even if they don't say "Champions skill" explicitly. This skill handles file
  placement, class structure, type interface selection, config loading, state cleanup, and description
  formatting — all the boilerplate that is easy to get wrong.
---

# New Champion Skill

## Your Job
Create a fully working Champions skill Java file in the right location, with the right type interfaces, config loading, and player state cleanup. Don't ask about things you can infer. Do ask if the role, skill slot, or mechanic are ambiguous.

## Project Context
- Module: `:champions`
- Base package: `me.mykindos.betterpvp.champions`
- Skill base class: `me.mykindos.betterpvp.champions.champions.skills.Skill`
- Skills are auto-discovered via `@Singleton @BPvPListener` — no manual registration needed

## Roles and Skill Slots

### Roles (`Role` enum)
`ASSASSIN`, `KNIGHT`, `BRUTE`, `RANGER`, `MAGE`, `WARLOCK` — or `null` for global (all roles).

### Skill slots (`SkillType` enum)
`SWORD`, `AXE`, `BOW`, `PASSIVE_A`, `PASSIVE_B`, `GLOBAL`

Slots are NOT role-restricted — any role can have any slot. However:
- `BOW` skills only make sense on `RANGER` and `ASSASSIN` (only they can use bows)
- `PASSIVE_B` is **exclusively** for `PassiveSkill` — no other activation type is valid there
- `GLOBAL` skills apply to all roles (`getClassType()` returns `null`)

## File Placement

Organized by **role folder** + **weapon folder**:

```
champions/.../champions/skills/skills/
├── assassin/  knight/  brute/  ranger/  mage/  warlock/
│   ├── sword/      ← SWORD slot skills
│   ├── axe/        ← AXE slot skills
│   ├── bow/        ← BOW slot skills (only meaningful for ranger/assassin)
│   └── passives/   ← PASSIVE_A and PASSIVE_B skills
└── global/         ← GLOBAL skills (Role = null)
```

## Activation Types

The activation type determines HOW the skill is triggered. Pick exactly one. The valid slots for each are strict.

---

### `PassiveSkill` — always active
Valid slots: any (`SWORD`, `AXE`, `BOW`, `PASSIVE_A`, `PASSIVE_B`, `GLOBAL`)

No activation method. Listen for events with `@EventHandler`.

```java
@Singleton @BPvPListener
public class MySkill extends Skill implements PassiveSkill, BuffSkill {
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEvent(DamageEvent event) {
        if (!(event.getDamagee() instanceof Player player)) return;
        int level = getLevel(player);
        if (level <= 0) return;
        // apply effect
    }
}
```

---

### `InteractSkill` — point-and-click activation
Valid slots: **SWORD, AXE only**

Triggered by right-click or left-click. Requires `activate(Player, int)` and `getActions()`. Pair with `CooldownSkill`.

```java
@Singleton @BPvPListener
public class MySkill extends Skill implements InteractSkill, CooldownSkill, OffensiveSkill {
    @Override
    public boolean activate(Player player, int level) {
        // do the effect — return true if used, false to cancel (refunds cooldown)
        return true;
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK; // or LEFT_CLICK
    }

    @Override
    public double getCooldown(int level) {
        return cooldown - ((level - 1) * cooldownDecreasePerLevel);
    }
}
```

---

### `ChannelSkill` — blocking-sword held activation
Valid slots: **SWORD only** (requires the player to block with their sword)

Extend `ChannelSkill` (abstract class) instead of `Skill`. Inherits `active` Set<UUID> tracking who is channeling. The channel runs while the player holds right-click with their sword raised.

```java
@Singleton @BPvPListener
public class MySkill extends ChannelSkill implements InteractSkill, CooldownSkill {
    @Override
    public boolean activate(Player player, int level) {
        active.add(player.getUniqueId());
        player.getWorld().playSound(...);
        return true;
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @UpdateEvent
    public void onUpdate() {
        Iterator<UUID> it = active.iterator();
        while (it.hasNext()) {
            Player player = Bukkit.getPlayer(it.next());
            if (player == null) { it.remove(); continue; }
            int level = getLevel(player);
            if (level <= 0) { it.remove(); continue; }
            if (!player.isHandRaised()) { it.remove(); continue; } // channel ended
            // tick effect
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) { active.remove(event.getPlayer().getUniqueId()); }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) { active.remove(event.getEntity().getUniqueId()); }

    @Override
    public double getCooldown(int level) {
        return cooldown - ((level - 1) * cooldownDecreasePerLevel);
    }
}
```

Description line: `"Hold right click with a Sword to activate"`

---

### `PrepareArrowSkill` — left-click to prepare, then fire
Valid slots: **BOW only**

Extend `PrepareArrowSkill` (abstract class) instead of `Skill`. Must implement `onHit()` and `displayTrail()`. The parent handles `EntityShootBowEvent` and arrow tracking automatically.

```java
@Singleton @BPvPListener
public class MySkill extends PrepareArrowSkill implements DamageSkill, OffensiveSkill {
    @Override
    public void onHit(Player damager, LivingEntity target, int level) {
        // called when the prepared arrow hits a living entity
    }

    @Override
    public void displayTrail(Location location) {
        // particle shown each tick while arrow is in flight
    }

    @Override
    public Action[] getActions() { return SkillActions.LEFT_CLICK; }

    @Override
    public double getCooldown(int level) {
        return cooldown - ((level - 1) * cooldownDecreasePerLevel);
    }
}
```

Description line: `"Left click with a Bow to prepare"`

---

### `ToggleSkill` / `CooldownToggleSkill` — drop-triggered
Valid slots: **PASSIVE_A only**

Activated when the player drops their sword or axe (Q key). Use `CooldownToggleSkill` when a cooldown is needed. `toggle()` is called each time the player drops.

```java
@Singleton @BPvPListener
public class MySkill extends Skill implements CooldownToggleSkill, MovementSkill {
    @Override
    public void toggle(Player player, int level) {
        // called when the player drops their weapon
    }

    @Override
    public double getCooldown(int level) {
        return cooldown - ((level - 1) * cooldownDecreasePerLevel);
    }
}
```

Description line: `"Drop your Sword / Axe to activate"`

---

## Tag Interfaces (mix and match — affect tooltip tags only)

| Interface | Tooltip tag | Use when... |
|---|---|---|
| `CooldownSkill` | — | Has a cooldown; **also auto-loads `cooldown` + `cooldownDecreasePerLevel` config** |
| `EnergySkill` | Energy | Costs energy per activation |
| `EnergyChannelSkill` | Energy | Drains energy while channeling |
| `DamageSkill` | Damage | Deals damage |
| `OffensiveSkill` | Offensive | Primarily offensive |
| `DefensiveSkill` | Defensive | Primarily defensive |
| `BuffSkill` | Buff | Buffs the player |
| `DebuffSkill` | Debuff | Debuffs enemies |
| `MovementSkill` | Movement | Affects movement |
| `AreaOfEffectSkill` | AoE | Affects an area |
| `CrowdControlSkill` | Crowd Control | Stuns/slows/roots |
| `HealthSkill` | Health | Heals or modifies HP |
| `FireSkill` | Fire | Fire-based |
| `TeamSkill` | Team | Affects teammates |
| `WorldSkill` | World | Modifies terrain |
| `UtilitySkill` | Utility | General utility |

---

## Key Implementation Rules

### Config loading
- Load all values in `loadSkillConfig()` — never in the constructor
- Config path is auto-derived: `skills.<role>.<skillnamestripped>.<key>` or `skills.global.<skillnamestripped>.<key>`
- `cooldown` and `cooldownDecreasePerLevel` are **inherited protected fields** from `Skill`, loaded automatically when `CooldownSkill` is implemented. **Never redeclare them.**

### Scaling values
```java
private double baseValue;
private double valueIncreasePerLevel;

public double getValue(int level) {
    return baseValue + ((level - 1) * valueIncreasePerLevel);
}

// In loadSkillConfig():
baseValue = getConfig("baseValue", 3.0, Double.class);
valueIncreasePerLevel = getConfig("valueIncreasePerLevel", 1.0, Double.class);

// In getDescription() — shows current value with per-level delta:
getValueString(this::getValue, level)
```

### Dealing damage — use `UtilDamage`, not Bukkit `entity.damage()`
```java
import me.mykindos.betterpvp.champions.combat.damage.SkillDamageCause;
import me.mykindos.betterpvp.core.utilities.UtilDamage;

UtilDamage.doDamage(new DamageEvent(target, player, null,
    new SkillDamageCause(this), getDamage(level), getName()));
```

### State tracking — always clean up on quit and death
```java
private final HashMap<UUID, SomeData> playerState = new HashMap<>();

@EventHandler
public void onQuit(PlayerQuitEvent event) {
    playerState.remove(event.getPlayer().getUniqueId());
}

@EventHandler
public void onDeath(PlayerDeathEvent event) {
    playerState.remove(event.getEntity().getUniqueId());
}
```

### `trackPlayer` / `invalidatePlayer` — for skills needing pre-equipped state
Override these when the skill needs to start tracking data the moment a player equips it:
```java
@Override
public void trackPlayer(Player player, Gamer gamer) {
    data.put(player, new MyData(getLevel(player)));
}

@Override
public void invalidatePlayer(Player player, Gamer gamer) {
    data.remove(player);
}
```

### Sending messages
```java
// Role-specific:
UtilMessage.simpleMessage(player, getClassType().getName(), "You used <green>%s %d</green>.", getName(), level);
// Global (getClassType() == null):
UtilMessage.simpleMessage(player, "Champions", "You used <green>%s %d</green>.", getName(), level);
```

---

## Pre-Submit Checklist
- [ ] Extends the right base class (`Skill`, `ChannelSkill`, or `PrepareArrowSkill`)
- [ ] Annotated `@Singleton @BPvPListener`
- [ ] `getClassType()` returns correct `Role` (or `null` for global)
- [ ] `getType()` returns correct `SkillType`
- [ ] Activation type matches the slot — `ChannelSkill` on SWORD only, `InteractSkill` on SWORD/AXE only, `PrepareArrowSkill` on BOW only, toggle on PASSIVE_A only, `PASSIVE_B` is `PassiveSkill` only
- [ ] `cooldown` / `cooldownDecreasePerLevel` NOT redeclared (inherited)
- [ ] All config fields loaded in `loadSkillConfig()`, not the constructor
- [ ] `PlayerQuitEvent` and `PlayerDeathEvent` clean up all UUID maps
- [ ] `getDescription()` uses `getValueString()` for all scaled values
- [ ] File placed in correct `<role>/<folder>/` directory
