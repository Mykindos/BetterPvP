# Network Layer

`framework/net/` is how servers talk to each other, with nothing above it knowing what carries the messages.
`MessageBus` has two primitives: `publish` announces something every server acts on for itself, and `request` asks and
collects the replies that arrive before a timeout. `subscribe` handles a topic, `answer` responds to questions on one.
`BusMessage` is a topic plus a flat map of strings, because every transport can carry those.

There are **three independent seams**, because a network can carry messages one way, hold shared state another and
move players a third. Joining a network that already has a message bus but uses the ordinary proxy transfer means
replacing one and leaving the others alone.

| Seam | Question | Ships as |
|---|---|---|
| `MessageBus` | how do servers talk? | `redis`, `proxy` |
| `SiteDirectory` | what exists across the network? | `redis`, `local` |
| `PlayerTransfer` | how does a player cross servers? | `proxy` |

`NetworkLayer` resolves each one **once, by name, from config**, and is the only class that chooses:

```yaml
core:
  network:
    bus: auto        # auto | redis | proxy | <a name you registered>
    directory: auto  # auto | redis | local | ...
    transfer: auto   # auto | proxy | ...
```

`auto` takes the first that can run, preferring Redis. **To integrate with another network**, register implementations
against `NetworkTransports` during startup and name them in config, with no edit to core:

```java
transports.registerBus("theirs", () -> new TheirBus(...));
transports.registerTransfer("theirs", () -> new TheirTransfer(...));
```

A factory returning `null` means "cannot run here", which is how Redis withdraws when it is not configured. A config
name nobody registered logs a warning and falls back rather than failing quietly. Jedis is shaded and relocated to
`me.mykindos.betterpvp.jedis`.

Handlers run off the main thread on both. Anything touching a world or a player schedules it. Both transports run the
same `MessageBusContract` test suite, which is what makes them interchangeable rather than merely similar.

`SiteDirectory` is the other half, and it is a **store rather than a pipe**: two servers can ask for the last place in
an instance at the same moment, so the check and the taking have to be one step nobody can interleave with. A bus
cannot do that. `RedisSiteDirectory` does it with a Lua script; `LocalSiteDirectory` is a network of one. Servers
advertise what they hold on a heartbeat (`SiteAdvertiser`) and entries carry a time to live, so a server that dies
stops renewing and its instances disappear with nothing to clean up. Real occupants and seats held for parties in
transit are counted separately, because the holding server owns the first and travellers own the second.

`PlacementProvider` picks `NetworkPlacement` when `SiteDirectory.isShared()` and `LocalPlacement` otherwise, so it
asks the directory rather than asking what is backing it. Networked
locating resolves: an instance this server holds, then one another server holds with room, then a new one on the
emptiest server. An owned site follows a sticky host, since its world lives on one machine's disk. Sending across
servers records the arrival in the directory and hands the player to the proxy; `NetworkPlacement` meets them on the
other side.

## Reach across servers

Three things are written to reach a person rather than a place, so they follow whoever they are addressed to.

**Chat.** A channel says what names it elsewhere with `IChatChannel.getNetworkKey(sender)`. Empty, the default, keeps
it on this server. A key is enough for another server to work out its own audience without the sender being there, so
clan and alliance chat carry the clan id and staff chat carries nothing. `NetworkChat` publishes what was said, and
the receiving server hands it to the ordinary `ChatReceivedEvent` pipeline, so a message from elsewhere still picks
up the clan colours, the skill hovers and the recipient's ignore list. `ChatReceivedEvent.getPlayer()` is therefore
null for one of those, since only the sender's `Client` is known on this side.

The far side answers with `NetworkChannels`, which each module registers into for the channels it owns:

```java
networkChannels.register(ChatChannel.CLAN, clanId -> membersOf(clanId));
```

**Private messages.** `PrivateMessages` is the whole of `/msg` and `/r`. A recipient on this server is written to
directly. Anyone else is asked for over the bus, and the server holding them delivers and answers, which is also how
the sender learns whether the name they typed is online anywhere. Administrating staff read what passed through
their own server. Being on another site does not hide anybody from this, since a private message is addressed rather
than perceived, and only vanish does.

**Leaderboards.** The rows are already shared, since a leaderboard reads them from the database rather than from the
server it runs on. What is local is the copy each server holds between refreshes, so `LeaderboardSync` announces
which leaderboard moved and the servers told re-read it. What travels is the name, never the standing.
