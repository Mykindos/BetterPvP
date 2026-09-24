# Champions Module

Class-based combat. Players pick a role, slot skills into a build, and use them in combat. Source root:
`champions/src/main/java/me/mykindos/betterpvp/champions/`.

## Where things live

| Package | What it holds |
|---|---|
| `champions/ChampionsManager` | Top-level manager |
| `champions/roles/` | `RoleManager`, role effects and sounds. The `Role` enum is in core (`core/components/champions/`) |
| `champions/builds/` | Per-player, per-role builds |
| `champions/npc/` | Role selection NPCs |
| `champions/skills/Skill.java` | Base class for every skill |
| `champions/skills/ChampionsSkillManager` | Skill lookup by name, role and type |
| `champions/skills/types/` | Capability mixins: `PassiveSkill`, `ToggleSkill`, `ChannelSkill`, `InteractSkill`, `CooldownSkill`, `EnergySkill`, `PrepareSkill`, `MovementSkill` and more |
| `champions/skills/skills/<role>/` | Concrete skills, plus `global/` |
| `champions/skills/traits/` | Role traits |
| `combat/`, `effects/` | Champions damage handling and reusable skill visuals |

## Notes

- To add a skill, use the `new-champion-skill` skill. It knows the placement, type interfaces, config loading and cleanup rules.
- `ChampionsClansRewardListener` bridging kills to clan XP lives in `:clans`, not here.
