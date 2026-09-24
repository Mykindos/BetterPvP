# BetterPvP

A multi-module Minecraft plugin suite for a clan PvP network. Paper 1.21.11, Java 21, Kotlin 2.1.0, Gradle Kotlin DSL.

## Where to read next

| Need | Read |
|---|---|
| How a module's code is laid out, its gotchas | `<module>/CLAUDE.md` (loads when you work in that module) |
| Domain language and lifecycles (Client, Gamer, Realm, clan flows) | `CONTEXT.md` and `<module>/CONTEXT.md` |
| A subsystem in depth | the `README.md` in its package, e.g. `core/.../world/site/README.md` |
| Designs, PRDs, plans | Outline, https://outline.betterpvp.net (the `outline` MCP server) |
| Scene and world-content authoring | `docs/SCENES.md` |

Prefer the IntelliJ MCP server (`intellij`) over text search for Java symbols. Its find-usages, rename and inspection tools understand imports and overloads. It needs the project open in IntelliJ.

## Modules

| Gradle id | What it is |
|---|---|
| `:core` | Shared framework: DI, database, items, menus, scenes, sites, networking, utilities |
| `:clans` | Clans, territory, world content, sailing, camps, auction house UI |
| `:champions` | Roles and skills |
| `:progression` | Professions (fishing, mining, woodcutting) |
| `:shops` | NPC shops and auction house backend |
| `:game` | Mini-game framework (CTF, Domination) |
| `:hub` | Lobby server, routing into other servers |
| `:lunar` | Lunar Client API integration |
| `:orchestration`, `:orchestration-service` | Cross-server queueing: client API and the standalone service |
| `:proxy` | Velocity proxy plugin |
| `:private:*` | Private subrepo in `private/` (events, dungeons, store, store-proxy, compatability). Included only when the folder exists. Commit to it separately. |

## Build

```bash
./gradlew :clans:compileJava   # compile one module
./gradlew shadowJar            # every module's JAR into $rootDir/build/
```

After finishing a code task, run the whole-suite `./gradlew shadowJar`, not a single module, so `build/` always holds a matching set. Skip it for doc-only changes.

## Framework patterns

- **DI** is Guice. Every plugin extends `BPvPPlugin` and exposes `getInjector()`. Outside Guice use `JavaPlugin.getPlugin(X.class).getInjector().getInstance(Y.class)`.
- **Listeners**: `@BPvPListener @Singleton` plus `implements Listener` is auto-registered by the module's `ListenerLoader`.
- **Ticks**: `@UpdateEvent(delay = ms)` only fires on registered listeners. A class that is only `@Singleton` is never ticked, so give it `@BPvPListener` and `implements Listener`.
- **Config**: `@Inject @Config(path = "...", defaultValue = "...")` on a field.
- **Events** extend `CustomEvent` with `@Data @EqualsAndHashCode(callSuper = true)`, or `@Getter` plus explicit constructors.
- **Database**: jOOQ via `database.getDslContext()`. Always `field(name("col"), Type.class)` with the explicit type. Flyway migrations live in `<module>/src/main/resources/<module>-migrations/postgres/V{YYYYMMDD}_{n}__{Description}.sql`.
- **Stores and transfer** sit behind an interface bound in the module's Guice module (`bind(Store.class).to(DatabaseStore.class)`). Callers depend on the interface only. No named registries or config keys choosing a store. Current implementations are stand-ins for a larger network.
- **Boss bars**: `addViewer`/`removeViewer`, and track viewers in your own `Set<Player>`. Iterating `BossBar.viewers()` to remove them fails on a type mismatch.
- **Delayed actions**: `core/framework/delayedactions/` (`PlayerDelayedActionEvent`) is being retired. New features own their own controller, tick and interrupts.

## Code rules

- **Player-facing text is always translated.** `Translations.component("key", args...)`, with the key added to all 12 `core/src/main/resources/translations/core_<lang>.properties` files (en, ar, de, es, fr, ja, ko, ms, nl, pl, ru, zh). MessageFormat args `{0}`, apostrophes doubled, colour applied in code. Files are UTF-8 with CRLF.
- **Logging ends in `.submit()`.** `log.warn("x {}", a).submit();`. Without it nothing is emitted.
- **Imports, not fully qualified names**, unless two imported types share a simple name.
- **Lombok `@Value`/`@Data`, not `record`.**
- **Composition over inheritance.** Capabilities are components (`ItemComponent`, `addBaseComponent`), not abstract base classes. No hidden wiring inside base classes.
- **Class size**: a class earns a file if it holds state or owns a decision that varies. Don't mint an interface plus two impls for a binary you control, and split long classes by responsibility.
- **Inline literals** used in fewer than 3 places. Font keys are always inline.
- **No throwaway array loops** like `for (double x : new double[]{a, b})`. Unroll or take discrete parameters.
- **Don't edit an existing shared listener** to accommodate a new feature. Make the new code compatible through Bukkit events.
- **Camp features are managed in-world** through an NPC or prop with nested menus. Commands are for staff only.

## Writing docs and comments

- Sparse comments that explain a non-obvious why. No banner or divider comments (`// ─── Title ───`).
- Javadocs describe the end state in the present tense. No "replaces X" or "used to". Not every type needs one.
- Describe the mechanism, not the game fiction ("teleports the crew to the staging world", not "puts the crew ashore"). This includes log messages.
- No em-dashes or semicolons. Don't explain a design decision unless asked.

## Workflow

- Never commit directly to `season-3` or `master`. Every change goes on a branch with a PR. Integration branches (e.g. `camps`) take PRs too.
- The repo is public and squash-merges. Nothing from `private/` goes into it.
- Track work on the **Network Expansion** GitHub project with the `backlog` skill.
- For camps work, keep the Outline docs "Camps: what we need from the team" and "Camps: features and what's missing" current at the end of each pass.
