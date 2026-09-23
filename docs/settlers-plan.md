# Settlers implementation plan

What we are building is in the [Settlers PRD](https://outline.betterpvp.net/doc/settlers-Hr70AaWblN). This file is how, in the order the PRs land. Every PR targets `camps`, is reviewable on its own, and leaves the server working.

## Shape

* **Core, `world/settler/`:** the framework. A settler, its profession, traits, morale, the roster it lives in, the NPC that shows it, and how it staffs a construction job. Core never learns what a clan is. Owners are a `SiteKey`, like construction.
* **Clans, `world/camp/settler/`:** the vocabulary and the numbers. Builder and Farmer, wages in coins, Dock arrivals, the hiring board, the Great Hall roster menu, Prosperity.
* **Numbers live in `configs/settlers.yml`.** Code says what exists and what it does, config says how much.
* **Every store sits behind an interface**, as with `SiteStorage` and `Placement`. The roster rides on the camp record, which already goes through `SiteStorage`. Prosperity gets its own `ProsperityStore`.

## How it connects to what exists

| Existing piece | What settlers do with it |
|----------------|--------------------------|
| `JobRule` (`holds`, `rate`) | `CrewRule` holds a job until its crew meets the Workforce threshold, and sets its rate from crew Speed. This replaces "jobs run without workers". |
| `StructureStage` | Gains `workforce`, the threshold for jobs on that stage. |
| `Camp` record and `CampStore` | Gains the roster, wage fund, pending arrivals and hiring board. Saved on change like everything else. |
| `ModeledNPC`, `PathfindNavigator`, `WaypointPatrolBehavior` | The settler body, walking and wandering. |
| `WorldContent` | `SettlerPresence` spawns a camp's settlers when its world opens and lets them go when it closes. |
| `CampPermissions` | Gains settler actions: hire, assign, dismiss, pay into the wage fund. |
| `CampArrivalNotices` | Gains lines for striking and departed settlers. |
| Core leaderboards | The Prosperity leaderboard. |

## PRs

### S1. Settler model and roster

The data, with nothing in the world yet.

* Core
  * `Settler`: id, name, history line (a translation key and what it fills in), `SettlerRarity`, profession id (nullable), specialty (a Builder's trade), trait ids, morale, assignment, state (working, idle, striking, leaving), timestamps.
  * `Profession` and `ProfessionRegistry`: id, what it works at (`WorkplaceKind`: construction job, or a named workplace such as the farm), the model to show, and its specialties.
  * `Trait` and `TraitRegistry`: group, whether it is a trade-off, the least rarity that rolls it and the professions that can. Strength comes from the settler's rarity when a trait is read, so rebalancing reaches settlers who already exist.
  * `Roster`: the settlers one site owns, with lookups by profession, assignment and state.
  * `SettlerSite`: what an owner supplies, like `ConstructionSite` does. Roster, `changed()`, the population cap and a working cap per profession. The population cap limits who lives there, the working cap limits how many of a profession work at once, and the rest wander.
  * `SettlerService`: grant, dismiss, assign and unassign, cap checks, events (`SettlerJoinedEvent`, `SettlerLeftEvent`, `SettlerAssignedEvent`).
  * `SettlerGenerator`: rolls a settler from a template (rarity, profession, source) and a `SettlerTable` (rarity numbers, name lists, history lines by source).
* Clans
  * `Camp` gains the roster. `CampSettlers` implements `SettlerSite`. Population cap from the Great Hall stage, per-role caps from config, Workshop bonus to the Builder cap.
  * Builder and Farmer registered as professions.
  * `/settler grant|list|dismiss` for staff, so the rest can be tested before any source exists.
  * Settler permissions (hire, assign, dismiss, pay) wait for S2, where the card is the first place players act on a settler.
* Tests for caps, rolls and roster persistence round trips.
* Cards: "Settler base model" (data half).

### S2. Settlers in the world

* Core
  * `SettlerNPC`: a `ModeledNPC` with a nameplate (name, rarity colour). Invulnerable. Falls back to a plain placeholder model when a profession's model is missing, so nothing breaks before art lands.
  * `SettlerPresence` (`WorldContent`): spawns every settler of the camp whose world opened, and releases them on close.
  * Behaviours: wander inside the camp's build zones and paths, walk to an assignment, work in place (animation name from config).
  * Right-click a settler to open its card.
* Clans
  * `SettlerCardMenu`: name, history, profession, rarity, traits, morale, current assignment, with assign and dismiss buttons behind permissions.
* Cards: "Settler base model" (visual half), "Worker NPCs".

### S3. Builders and crews

* Core
  * `StructureStage.workforce`, read from `camps.yml` per stage.
  * `BuilderStats`: Workforce, Speed, falloff, compatible types, perks. Comes from the Builder's type and rarity in `settlers.yml`.
  * `CrewRule implements JobRule`
    * `holds`: the crew's summed Workforce is below the stage threshold.
    * `rate`: crew Speed with per-type diminishing returns, no falloff between compatible pairs, then the speed cap.
  * Crew limits: max crew size, per-rarity limit per job. Builders join a running job but can't leave it until it ends.
  * Builders walk to their job and work there. Idle Builders wander.
* Clans
  * Crew section in the construction flow: after placing a blueprint, and on any job, a menu shows the threshold, what the crew brings and what is missing, with the free Builders to add.
* Tests for threshold, falloff, compatibility, caps and pausing when a crew shrinks.
* Cards: "Job staffing rules", "Worker traits", "Worker cap".

### S4. Wages

* Core
  * `WageModel` interface, chosen by config.
    * `FixedWageModel`: a rate per Builder type over real time.
    * `IdleWorkingWageModel`: an idle rate, and a higher one while on a job.
  * `Payroll`: charges wages from a `CoinAccount` interface in real time, computed from timestamps like jobs, so it is right even while the camp world is closed.
  * Unpaid Builders go on strike at once (a `StrikeRule` holds their jobs) and leave after 3 days still unpaid.
* Clans
  * The wage fund lives on the camp record. `CampWageFund` implements `CoinAccount`. Members pay coins in at the Great Hall roster.
* Cards: "Wages and strikes".

### S5. Morale, losses and the roster

* Core
  * `MoraleEngine`: neutral baseline, pushed up by `MoraleSource`s (food, camp-wide traits) and down by penalties (unpaid wages across the camp, idle professionals, recent dismissals). Settlers below neutral for long enough leave.
  * Higher morale strengthens resident bonuses through one multiplier every workplace reads.
  * `FoodSource` interface with nothing behind it yet. The Granary and Mill plug in when they exist.
* Clans
  * `SettlerRosterMenu` at the Great Hall and `/clan settlers`: everyone, with filters by profession and state, the wage fund, caps and Prosperity.
  * Entry notices for strikes and settlers who left.
* Cards: part of "Settler base model".

### S6. Getting settlers

* Clans
  * `DockArrivals`: every so often (real time, per camp) a boat brings a few candidates to the Dock's `settler_arrival` point. They wait for a while. Common ones join for free if there is room, and rarer or perk-carrying ones show a coin price.
  * `HiringBoard`: a rotating pool per camp with rarity weights, refreshed on an interval, plus hiring a chosen candidate for coins.
  * Milestones: clan level milestones in config grant a set settler, through `ClanLevelUpEvent`.
  * `SettlerGrant` API, which dungeons, bosses and resource islands call when they get their hooks. Staff command until then.
* Cards: "Hiring".

### S7. Farmers and workplaces

* Core
  * `Workplace`: something with resident slots and a bonus. A structure is one kind of workplace. The camp farm is another, keyed by the farm zone.
  * `WorkplaceBonus`: reads the residents' stats, traits and the morale multiplier.
* Clans
  * `FarmWorkplace`: Farmers give the farm better crop yields and faster growth, as numbers from config.
  * Structure menus show resident slots.
* Cards: "Upgradable farm" gets its first bonus source.

### S8. Camp-wide traits and Prosperity

* Core
  * `CampWideTrait`: traits that act from anywhere, such as raising everyone's morale.
* Clans
  * `Prosperity`: population, rarity and morale into one score, recomputed on change.
  * `ProsperityStore` interface with a database implementation, so the leaderboard can rank camps that are not loaded.
  * Prosperity leaderboard on the core leaderboard framework.

## Order and parallel work

S1 comes first. S2 and S3 can go in parallel after it. S4 needs S3. S5 needs S4 for the strike penalty. S6, S7 and S8 need only S1 and S2 and can go in any order after them.

## Needed from the team

Added to the camps checklist in Outline as each PR lands.

* Custom models for Builder, Farmer and no-profession settlers, with walk, idle and work animations
* `settler_arrival` point at the Dock on every camp island
* Name and history line lists
* The Builder roster, trait list, camp-wide traits and every number in `settlers.yml`
* What else rarity should change, if anything
