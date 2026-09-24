# Cutscenes

`core/cutscene/` runs a **Cutscene**: a camera track, a dialogue track, or both, played to one player.

## The shape

Neither track owns the other. One `CutsceneSession` holds the player; the tracks coordinate by announcing and
waiting on `Signal`s, so both directions are expressible in the same cutscene.

```java
Cutscene.builder("whats_new")
    .skip(SkipPolicy.seenBefore(SkipPolicy.all()))       // first watch mandatory, replays skippable
    .dialogue(Conversations.builder("whats_new")
        .node("greet", n -> n.speaker("Chronicler").bodyKey("cutscene.whatsnew.greet")
            .response(r -> r.id("show_me").labelKey("...").onSkip().goTo("mine_intro")))
        .node("mine_intro", n -> n.speaker("Chronicler")
            .await(Signal.beatKey("whats_new/mine"))     // waits for the camera to land
            .bodyKey("cutscene.whatsnew.mine"))
        .build())
    .camera(camera -> camera
        .shot("whats_new/gate", s -> s.cut()
            .until(Signal.response("greet", "show_me")))
        .shot("whats_new/mine", s -> s.travel(2.5, Easing.EASE_IN_OUT)
            .until(Signal.nodeEnd("mine_intro"))
            .during(ShotEffects.pointer(foreman::getLocation, arrowItem)))
        .shot("whats_new/harbour", s -> s.travel(4, Easing.EASE_IN)
            .hold(Duration.ofSeconds(3))
            .skip(SkipPolicy.none())))                   // this shot always plays
    .build();
```

Register it so quests, conversations and `/cutscene` can name it:

```java
cutsceneRegistry.register("whats_new", "What's New tour", WhatsNew::build);
```

A factory, not an instance — it is built per viewer, so gates and text can already know who is watching.

## Camera markers

A shot names a Mapper `camera` data-point (a **perspective** marker — the facing *is* the shot) by its `id:` tag:

```
camera   id:whats_new/mine
```

Ids are namespaced by cutscene by convention. Inserting a shot mid-timeline is one marker plus one line; nothing
renumbers, and the same cutscene runs in every world cloned from a template.

## Signals

Everything announces itself, so the common cases need no signal strings:

| Emitted by | Signal |
|---|---|
| a beat arriving / leaving | `beat:<id>` / `beat:<id>:end` |
| a node starting / being answered | `node:<id>` / `node:<id>:end` |
| a specific response | `response:<node>:<id>` |
| the dialogue finishing | `conversation:end` |

A beat waits with `.until(Signal…)`; a node waits with `.await(Signal.beatKey(…))`. Waits count from the beat's
**start**, not its arrival — otherwise a long travel outlasting its dialogue would wait forever for a signal that
already fired.

## Gating

Spectator mode already blocks movement, damage in and out, skill activation and block interaction, so the manager
adds only what it does not: ESC-dismount, spectate-teleport, and disconnect. `CutsceneOriginStore` persists the
viewer's position and game mode **before** the game mode changes, so a crash mid-cutscene is recoverable on rejoin.

## Letterbox

The top bar rides the boss-bar overlay and the bottom bar the action bar, because each sits a fixed GUI-pixel
distance from its own screen edge (the distance between edges is unknowable server-side). Thickness is config, not a
pack rebuild — the `betterpvp:cutscene` font holds the same two images at a ladder of ascents:

```yaml
cutscene.letterbox.topHeight: 28
cutscene.letterbox.bottomHeight: 56
cutscene.letterbox.slideTicks: 6
```

Dialogue is **composited into** the bottom bar rather than being outranked by it, and the conversation's own backdrop
is suppressed (`ConversationOptions.managedByCaller()`) so the two never double-darken.

## Skipping

`SkipPolicy` is evaluated per `SkipScope` (`BEAT` / `CUTSCENE`) and overridable per beat. Tap SNEAK to skip a beat,
hold it to skip the cutscene. Skipping still runs each response's side effects — a skipped tutorial that withholds
what it was meant to hand over is a bug — taking the response marked `.onSkip()`, else the first available. A beat
skip is refused while the player is sitting on an unanswered question.

## Authoring commands

`/cutscene play <id>` · `from <id> <beat>` (start mid-timeline) · `markers` · `list` · `validate [id]` · `stop` ·
`reload` (re-read markers) · `forget <id>` (replay as a first viewing).

`CutsceneValidator` runs at boot and catches the expensive failure: a wait nothing announces. That does not throw —
it leaves the player behind a camera forever — so it is a set difference done up front.

## Console

Cutscenes are code, so they reach the admin console through the **manifest** (`game_cutscenes`), not the content
table — the same route items and zones take. Console-authored quests and conversations reference one by a validated
`cutscene_ref`, with no publish step. Primitives: `action.start_cutscene`, `action.begin_cinematic` (grows a camera
track on a live conversation without ending it).
