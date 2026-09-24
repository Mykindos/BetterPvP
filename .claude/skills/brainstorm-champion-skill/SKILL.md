---
name: brainstorm-champion-skill
description: >
  Brainstorm new Champions skill ideas for the BetterPvP Minecraft server. Use this skill whenever
  the user asks for skill ideas, wants inspiration for new champion abilities, or says things like
  "give me sword skill ideas for Knight", "what axe skills could I add to Brute", "passive A ideas
  for Ranger", or "brainstorm some Mage skills". Also trigger when the user asks what content to add
  to a specific role, or just says "give me skill ideas" with or without a role specified.
---

# Brainstorm Champion Skill Ideas

## What You're Doing
Read the existing skills for the requested role and slot, then brainstorm original ideas that fill gaps, complement what's already there, and have interesting synergy with skills in other slots.

## Step 1 — Clarify if needed

Parse the user's request for:
- **Slot**: sword, axe, bow, passive, global
- **Role**: Assassin, Knight, Brute, Ranger, Mage, Warlock — or unspecified (any role)

**If the user said "passive"**: ask whether they mean PASSIVE_A or PASSIVE_B before proceeding.
- PASSIVE_A: can be a passive (always-on), a toggle skill, or a drop-triggered skill
- PASSIVE_B: passive (always-on) skills only

**If no role was specified and the slot isn't GLOBAL**: ask which role, or offer to brainstorm for all roles.

## Step 2 — Read existing skills

Skills live at:
```
champions/src/main/java/me/mykindos/betterpvp/champions/champions/skills/skills/
├── <role>/
│   ├── sword/
│   ├── axe/
│   ├── bow/
│   └── passives/      ← both PASSIVE_A and PASSIVE_B skills live here
└── global/
```

Read in this order:
1. The **target slot folder** for the requested role — understand what mechanics already exist so ideas are fresh
2. **All other slot folders** for the same role — you'll use these for combination suggestions
3. The **global/** folder — global skills apply to all builds, so they're always combinable

Skim each file for: the skill name, its activation type, and what it does. You don't need to read full implementation detail — the `getName()`, `getDescription()`, and class declaration are enough.

## Step 3 — Brainstorm and output ideas

Generate **5–8 ideas**. Each idea should feel meaningfully different from existing skills in that slot — avoid retreading the same core mechanic.

### Activation type constraints (respect these)
| Slot | Allowed activation types |
|---|---|
| SWORD | Passive, InteractSkill (click), ChannelSkill (hold block with sword) |
| AXE | Passive, InteractSkill (click) |
| BOW | Passive, PrepareArrowSkill (left-click prepare → fire) |
| PASSIVE_A | Passive (always-on), ToggleSkill / CooldownToggleSkill (drop weapon to activate) |
| PASSIVE_B | Passive (always-on) only |
| GLOBAL | Passive only |

### Output format per idea

```
**[Skill Name]** · [Role] · [Slot] · [Activation type]

Mechanic: [What happens when activated / what it passively does. Be specific about numbers,
conditions, and duration where it adds clarity.]

What makes it fun/unique: [The interesting decision, counterplay, or feel it creates.
Why would a player want this over alternatives?]

Combines well with:
- [Existing skill name] ([slot]): [why these two together create something interesting]
- [Existing skill name] ([slot]): [synergy explanation]
```

### What makes a good idea
- Has a clear **activation condition** and **payoff** — the player knows exactly what they're doing and why
- Creates **interesting decisions** or **counterplay** — either for the user or their opponent
- Fits the **role identity**: Assassin = burst/mobility/stealth, Knight = aggressive sustain, Brute = CC/tankiness, Ranger = ranged/precision, Mage = elemental/support, Warlock = health manipulation/proximity
- Combinations should be **mechanically meaningful** — not just "both do damage" but "skill A sets up skill B" or "skill B rewards the positioning skill A creates"

### Example idea (for reference — don't copy this)
```
**Parting Blow** · Knight · SWORD · InteractSkill (right-click)

Mechanic: Right-clicking launches the player backward 6 blocks. The next sword hit within
4 seconds deals 30% bonus damage. Higher levels increase the damage bonus and reduce the cooldown.

What makes it fun/unique: Turns retreating into an offensive tool. The player has to commit
to the gap — back off and then re-engage, rewarding aggressive repositioning rather than
just tanking hits.

Combines well with:
- Riposte (SWORD): Can't combine — same slot. (Don't suggest same-slot combinations.)
- Break Fall (PASSIVE_B/Global): Landing from the knockback safely lets you re-engage immediately.
- Iron Will (PASSIVE_A): The window to land the boosted hit synergizes with a stacking damage buff.
```

> Don't suggest combinations between two skills of the same slot — a build can only equip one skill per slot.
