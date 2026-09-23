# Settlers implementation plan

What we are building is in the [Settlers PRD](https://outline.betterpvp.net/doc/settlers-Hr70AaWblN). This file is how, in the order the PRs land. Every PR targets `camps`, is reviewable on its own, and leaves the server working.

## Shape

* **Core, `world/settler/`:** the framework. A settler, its profession, traits, morale, the roster it lives in, the NPC that shows it, and how it staffs a construction job. Core never learns what a clan is. Owners are a `SiteKey`, like construction.
* **Clans, `world/camp/settler/`:** the vocabulary and the numbers. Builder and Farmer, wages in coins, Dock arrivals, the hiring board, the Great Hall roster menu, Prosperity.
* **Numbers live in `configs/settlers.yml`.** Code says what exists and what it does, config says how much.
* **Players manage everything in the world, not with commands.** The Steward in the Great Hall opens one menu with the rest nested under it. Commands are for staff only.
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
  * `SettlerNPC`: a `ModeledNPC` with a nameplate (name in its rarity colour, profession above). Invulnerable. Legendary settlers give off a faint particle.
  * `SettlerLook`: model, skin and idle, walk and work animations. The site supplies it, so camps can fall back to a placeholder model while a profession's own is not installed.
  * `SettlerPresence` (`WorldContent`): spawns every settler of the camp whose world opened, follows joins, departures and assignments, and releases them on close.
  * `SettlerRoutine`: wander around home, walk to an assignment, play the work animation there. Only moves while a player is near.
  * `SettlerSite` gains `allows` (`SettlerAction`: hire, assign, dismiss, pay), `look`, `home`, `workplace` and `interact`.
  * Right-click a settler to open its card.
* Clans
  * `SettlerCardMenu`: name, history, profession, rarity, traits, morale, current assignment, with assign and dismiss buttons behind permissions.
  * Settler permissions per rank on the camp record, `permissions.settlers` defaults in `camps.yml`, and a Settlers page in `/clan permissions`. Allies get none.
  * Settlers gather at the Great Hall's `settler_home` point and work at a structure's `settler_work` point. Farmers work the middle of the farm.
  * Looks per profession, and per rarity within a profession, in `settlers.yml`.
* Cards: "Settler base model" (visual half), "Worker NPCs".

### S3. Builders and crews

* Core
  * `StructureStage.workforce`, read from `camps.yml` per stage.
  * `BuilderStats`: Workforce, Speed, efficiency, trade and compatible trades. The site works it out per job, so trades and traits can depend on the job and the crew.
  * `CrewSpeed`: the fastest Builder counts fully, the rest at their efficiency, a compatible trade already on the crew counts fully with a bonus, then the cap.
  * `CrewRule implements JobRule`
    * `holds`: the crew's summed Workforce is below the stage threshold. Repairs and moves need half the stage the structure stands at.
    * `rate`: `CrewSpeed` of the crew. Builders on strike bring nothing.
  * `CrewService`: joins a Builder to a running job, checking crew size and per-rarity limits and the working cap. Builders stay until the job ends, then are let go and counted as having finished a job. A job's crew is `Job.staff`, and a Builder's assignment is the structure's id.
  * Job rules only govern running jobs, so a finished job can always be claimed.
  * Builders walk to their job and work there. Idle Builders wander.
* Clans
  * `CampBuilders`: stats from `settlers.yml` by rarity, trades (specialty resource, extra Workforce, compatible trades) and every Builder trait, at the settler's trait strength. Frugal and Patcher hand back part of the cost when the job ends.
  * `/clan crews`: every running job with its Workforce, speed and time left, and a crew menu per job listing the crew and the free Builders to add. A Builder's card opens its crew.
  * Members in the camp are told when a job starts waiting for a crew, and arrival notices list jobs waiting for one.
  * Every new camp starts with the settlers in `starting-settlers` (two common Builders), so it can staff its first jobs.
* Tests for threshold, speed, compatibility, caps, trait effects and releasing crews.
* Cards: "Job staffing rules", "Worker traits", "Worker cap".

### S4. Wages and the Steward

* Core
  * `WageModel` interface, chosen by config.
    * `FixedWageModel`: a rate per profession and rarity over real time.
    * `IdleWorkingWageModel`: an idle rate, and a higher one while working.
  * `CoinAccount` interface: where a site's wages come from.
  * `Payroll`: settles wages from the last settlement's timestamp, so a camp closed for a day is charged for the day when its world opens. Only the server holding a camp's world settles it, so two servers never charge one fund. Part coins carry over.
  * When the fund runs out, every paid settler strikes from the moment it ran out. Strikers are not paid, bring nothing to a crew, and go back to what they were doing once the fund can pay a minute for everyone. A striker still unpaid after the strike limit leaves (`SettlerLeaveReason.UNPAID`).
  * `SettlerSite` gains `wageModel`, `wageFund`, `wageMultiplier` and `strikeLimit`.
* Clans
  * The wage fund lives on the camp record. `CampWageFund` implements `CoinAccount`. Greedy asks for 25% more.
  * **The Steward:** one NPC on the Great Hall's `steward` point, which moves with the hall. Right-clicking it opens the Great Hall menu, the hub for everything a clan manages there: Wages, Crews, Construction and Permissions, each a nested menu with Back.
  * The wage fund menu shows the fund, the hourly cost, how long it lasts and who is striking. Members whose rank may PAY put in 1,000, 10,000 or 100,000 coins.
  * Online members are told when settlers strike, return or leave unpaid. A settler's card shows its wage.
  * `/clan crews` is gone. The crews menu is reached from the Steward.
* Cards: "Wages and strikes".

### S5. Morale, losses and the roster

* Core
  * `MoraleEngine`: neutral baseline, pushed up by `MoraleSource`s (food, camp-wide traits) and down by penalties (unpaid wages across the camp, idle professionals, recent dismissals). Settlers below neutral for long enough leave.
  * Higher morale strengthens resident bonuses through one multiplier every workplace reads.
  * `FoodSource` interface with nothing behind it yet. The Granary and Mill plug in when they exist.
* Clans
  * `SettlerRosterMenu`, a Settlers page in the Steward's Great Hall menu: everyone, with filters by profession and state, the wage fund, caps and Prosperity.
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
