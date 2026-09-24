# Site Framework

`world/site/` is where places live. A **Site** is somewhere a player can be, and how it behaves is config, not a
subclass: lifecycle, admission, selection, rejoin, anchorable and dormancy are fields on `SitePolicy`, read from
`configs/sites.yml`. Adding a place is a row in that file.

| Question | File |
|---|---|
| What places exist? | `SiteRegistry` + `sites.yml` |
| Which world does this group go to? | `SiteInstances.locate(SiteKey, Party)` |
| How does a group form before it travels? | `crew/` — `Crew`, `CrewService` |
| May this party join that instance? | `Admission` · which of several? `Selection` |
| How is a world cloned, adopted, woken, deleted? | `SiteWorlds` |
| What survives a restart? | `SiteStore` (the `site_instances` table, `recover()` on boot) |
| Where does a player appear on login? | `Residency` |
| Whose instance of an owned site is this? | `SiteOwners` + `SiteOwnership` |
| Where is a site's record kept? | `SiteStorage`, resolved by `SiteStorages` (`LocalSiteStorage` ships) |
| Does this run on another server? | `Placement` — **and nowhere else** |
| Who can this player perceive? | `Presence` |
| Where does a party land inside a world? | `ArrivalPoints` + `ArrivalDistribution` |

`Residency` decides where a player materialises on login, from two records in `site_residency` (one row per client per
kind, via `ResidencyStore`): the *residence* is the instance they were last in and the spot they stood on, the *anchor*
is the last site that will have them back. Only a site with `anchorable: true` becomes an anchor, which is why a player
who leaves a released island is never stranded. Both rows are read once while the connection is being configured and
held for the session, so `Residency.fallback` costs no query. Call it whenever a player has to be moved and there is
nowhere obvious to send them.

`Presence` answers "who can this player perceive", and is what any list, message or sound meant for "everyone" should
ask. Two players perceive each other when they are in the same world, which is enough because one instance holds one
world: two parties on separate copies of a site are in separate worlds, and so is a crew at sea. Entities need no
handling, since a player in another world is never sent to the client at all. The tab list is the exception and
`PresenceListener` owns it, listing and unlisting on join and world change. Perception is not visibility: a vanished
player is still perceived and still receives chat.

## Crews

`world/site/crew/` is how a group forms before it travels. A `Crew` **holds** a `Party` rather than duplicating one:
the party is the roster, and the crew is the assembly around it — where it is gathering, what it holds, who has asked
to come aboard, and whether it has left. Membership replaces the party rather than editing one, since a party is
immutable, so what `Placement.locate` is handed at departure needs no conversion and cannot change under it.
`CrewService.depart` freezes the roster, which is what makes party-first allocation race-free.

`CrewService` knows nothing of Bukkit, and nothing of what the group is gathering on or who counts as a friend. It is
handed decisions ("this player may join outright") and answers what the roster becomes, so every membership rule is
testable without a server. The assembly point is an opaque id plus a world name, which a caller fills in with whatever
it gathers on. Turning physical presence into membership belongs to the feature, not here: see
`clans/world/ship/crew/` for the ship's version of that.

## Owned sites

An owned site has one instance per owner, and `SiteKey` carries that owner as a plain `long` so core never learns
what an owner is. The module that does implements `SiteOwnership` and registers it:

```java
owners.register("camp", ownership);   // ownerOf(player) -> which instance is theirs
```

The same interface answers `templateFor(key)`, which is what an owner's world is built from when they get to choose.
Answering **empty means "no choice"**, deliberately not "the site's default": a world already on disk is then left
exactly as it is. That matters because a record that has not finished loading would otherwise read as a choice and
rebuild somebody's world out from under them. `SiteWorlds` records what a folder was built from inside the folder
itself, so a changed answer rebuilds it and an unchanged one does not.

`SiteStorage` is where a site's record lives: everything about an instance that its world does not hold by itself.
It is bytes as far as core is concerned, since only the module that wrote a record knows what is in it. Local files
are what ships (`LocalSiteStorage`). `SiteStorages` resolves which implementation this server uses.

`SiteInstanceDormantEvent` fires once an instance's world has closed, which is the last moment anything inside it
can have changed and therefore where a record gets written down.

`menu/navigation/` is the generic "pick somewhere to go" menu: a `Destination` is a name, an icon and what choosing
it does, `DestinationProvider` supplies the ones a player is offered, and `NavigationMenu` renders them. It holds no
opinion about where the player ends up or how long arriving takes, so a destination that teleports and one that
starts a journey ending minutes later are the same thing to it. Whatever refuses the choice, such as a combat check,
belongs to whoever opens the menu.

Ships, crews and oceans are game fiction and live in `clans/world/sailing/`.
