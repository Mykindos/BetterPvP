# Working in BetterPvP

Minecraft plugin suite for a clan PvP network (Paper, Java 21, Gradle, Guice, jOOQ). The project itself is explained in
`docs/ARCHITECTURE.md`. Read it when you need the module map, the build, or how a framework piece works. This file is
how to work here.

## Finding your way

- The module you touch loads its own `CLAUDE.md`. Read the `README.md` in a package before changing that subsystem.
- Look up Java symbols with the `intellij` MCP tools (find usages, symbol search, rename) before text search. They
  understand imports and overloads. Fall back to Grep if IntelliJ isn't running.
- Designs and plans live in Outline (`outline` MCP). Check there before redesigning something that has a PRD.
- For library behaviour (Paper, Adventure, PacketEvents, jOOQ), use the `paper-researcher` agent or Context7 rather than
  guessing from memory.
- Use the domain terms in `CONTEXT.md` instead of inventing new ones.

## How to work

1. **Act or ask.** When the task is clear, do it end to end. When a choice changes the outcome (API shape, a game rule,
   naming other code depends on), ask first. Never hand back stubs, TODOs for the user, or exercises.
2. **Branch.** Work in a worktree on a new branch cut from the target (`season-3`, or an integration branch like
   `camps`). Never commit to `season-3` or `master` directly.
3. **Verify.** A Stop hook compiles the modules you changed and reports errors. Fix them before finishing. A
   PostToolUse hook flags convention breaks in new code. Fix those too unless one is a genuine exception.
4. **Review.** Before opening a PR, run the `convention-reviewer` agent on the branch and resolve what it finds.
5. **Build.** After a code task, run `./gradlew shadowJar` for the whole suite so `build/` holds a matching set of JARs.
   Skip it for doc-only changes.
6. **Ship.** Commit and open the PR with the `commit-and-pr` skill. Stop there. The user reviews and merges.
7. **Track.** Move the Network Expansion card with the `backlog` skill (In progress when work starts, In review when the
   PR opens). For camps work, update the Outline docs "Camps: what we need from the team" and "Camps: features and
   what's missing" at the end of each pass.

## Code rules

- **Player-facing text is always translated**: `Translations.component("key", args...)`, with the key added to all 12
  locale files in `core/src/main/resources/translations/`.
- **Every log call ends in `.submit()`**, or nothing is emitted.
- **`@UpdateEvent` only runs on `@BPvPListener` classes** that `implements Listener`. `@Singleton` alone is never ticked.
- **Imports, not fully qualified names**, unless two imported types share a simple name.
- **Lombok `@Value`/`@Data`, never `record`.**
- **Composition over inheritance.** Capabilities are components (`ItemComponent`, `addBaseComponent`), not abstract
  base classes. No hidden wiring in base classes.
- **Class size.** A class earns a file if it holds state or owns a decision that varies. No interface plus two impls for
  a binary you control. Split long classes by responsibility.
- **Stores, orchestration and transfer go behind an interface** bound in a Guice module. No registries or config keys
  choosing an implementation.
- **Inline literals** used in fewer than 3 places. Font keys are always inline.
- **No loops over throwaway arrays** (`for (x : new double[]{a, b})`).
- **Leave shared listeners alone.** Make new code compatible through Bukkit events instead of editing an existing one.
- **Don't build on `PlayerDelayedActionEvent`**. It is being retired. Own the controller, tick and interrupts.
- **Camp features are managed in-world** through an NPC or prop with nested menus. Commands are for staff only.
- **Boss bars**: track viewers in your own `Set<Player>`. Never iterate `BossBar.viewers()` to remove them.

## Writing

- Comments are sparse and explain a non-obvious why. No banner or divider comments.
- Javadocs describe the end state in the present tense. No "replaces X" or "used to". Not every type needs one.
- Describe the mechanism, not the game fiction, including in log messages.
- No em-dashes or semicolons in docs. Don't explain a design decision unless asked.
- Keep replies to the user short. No insight boxes.

## Boundaries

- The repo is public and squash-merges. Nothing from `private/` goes into it. `private/` commits go to its own repo.
- New rules belong in this file or a module `CLAUDE.md`, not in memory.
