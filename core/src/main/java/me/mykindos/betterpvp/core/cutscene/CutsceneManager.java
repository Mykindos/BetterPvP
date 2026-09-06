package me.mykindos.betterpvp.core.cutscene;

import com.destroystokyo.paper.event.player.PlayerStopSpectatingEntityEvent;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.cutscene.camera.Beat;
import me.mykindos.betterpvp.core.cutscene.camera.CameraMarker;
import me.mykindos.betterpvp.core.cutscene.camera.CameraMarkers;
import me.mykindos.betterpvp.core.cutscene.camera.CameraPose;
import me.mykindos.betterpvp.core.cutscene.camera.Cinematic;
import me.mykindos.betterpvp.core.cutscene.hud.CutsceneHud;
import me.mykindos.betterpvp.core.cutscene.signal.Signal;
import me.mykindos.betterpvp.core.cutscene.skip.CutsceneViewStore;
import me.mykindos.betterpvp.core.cutscene.skip.SkipScope;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.quest.conversation.ConversationManager;
import me.mykindos.betterpvp.core.quest.conversation.ConversationOptions;
import me.mykindos.betterpvp.core.quest.primitive.QuestPrimitiveHandlers;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Runs cutscenes: starts them, ticks them, gates the player for their duration, and gives the camera back afterwards
 * however the cutscene ends.
 * <p>
 * The gating list is deliberately short. Spectator mode with a spectator target already blocks movement, incoming and
 * outgoing damage, skill activation and block interaction, so this class adds only what spectator does not defend:
 * dismounting the camera, teleporting away from it, and losing the player entirely to a disconnect. A second layer of
 * freezing on top of that would be two gates where the game already has one.
 *
 * @see Cutscene
 */
@Singleton
@BPvPListener
@CustomLog
public class CutsceneManager implements Listener {

    /** How long SNEAK must be held before it means the whole cutscene rather than the current beat. */
    private static final int SKIP_HOLD_TICKS = 20;

    /** How far the viewer's body may drift from the camera before it is pulled along, squared. */
    private static final double FOLLOW_DISTANCE_SQUARED = 64;

    private final Core core;
    private final CutsceneRegistry registry;
    private final CameraMarkers markers;
    private final CutsceneOriginStore origins;
    private final CutsceneViewStore views;
    private final ConversationManager conversations;
    private final CutsceneHud hud;

    private final Map<UUID, CutsceneSession> sessions = new ConcurrentHashMap<>();
    private @Nullable BukkitTask task;

    @Inject
    public CutsceneManager(Core core, CutsceneRegistry registry, CameraMarkers markers, CutsceneOriginStore origins,
                           CutsceneViewStore views, ConversationManager conversations, CutsceneHud hud,
                           QuestPrimitiveHandlers handlers) {
        this.core = core;
        this.registry = registry;
        this.markers = markers;
        this.origins = origins;
        this.views = views;
        this.conversations = conversations;
        this.hud = hud;

        // What a console-authored quest or conversation dispatches. The old action.start_cinematic name is still
        // answered so rows published against it keep working; its id now names a cutscene.
        handlers.registerAction("action.start_cutscene", (player, data) -> start(player, data.getString("cutscene")));
        handlers.registerAction("action.start_cinematic", (player, data) -> start(player, data.getString("cinematic")));
        handlers.registerAction("action.begin_cinematic", (player, data) -> registry
                .build(player, data.getString("cutscene"))
                .filter(Cutscene::hasCamera)
                .ifPresent(cutscene -> beginCamera(player, cutscene.getCamera())));

        // The only thing conversations know about cutscenes is that something answers their await keys.
        conversations.setGate((player, key) -> {
            final CutsceneSession session = sessions.get(player.getUniqueId());
            return session != null && !session.getSignals().everFired(Signal.of(key));
        });
    }

    public boolean isWatching(@NotNull Player player) {
        return sessions.containsKey(player.getUniqueId());
    }

    public @NotNull Optional<CutsceneSession> session(@NotNull Player player) {
        return Optional.ofNullable(sessions.get(player.getUniqueId()));
    }

    public @NotNull Collection<CutsceneSession> sessions() {
        return sessions.values();
    }

    /** Starts a registered cutscene by id, building it for this viewer. */
    public boolean start(@NotNull Player player, @NotNull String cutsceneId) {
        final Optional<Cutscene> built = registry.build(player, cutsceneId);
        if (built.isEmpty()) {
            log.warn("Tried to play unknown cutscene {}", cutsceneId).submit();
            return false;
        }
        return start(player, built.get());
    }

    /**
     * Starts a cutscene held in hand rather than looked up - the entry point for one built inline by a feature.
     *
     * @return true if it actually started
     */
    public boolean start(@NotNull Player player, @NotNull Cutscene cutscene) {
        if (isWatching(player)) {
            UtilMessage.simpleMessage(player, "Cutscene", "You are already watching something.");
            return false;
        }

        final CutsceneSession session = new CutsceneSession(player.getUniqueId(), cutscene,
                views.hasSeen(player.getUniqueId(), cutscene.getId()));

        if (cutscene.hasCamera() && !openCamera(player, session, cutscene.getCamera())) {
            return false;
        }

        sessions.put(player.getUniqueId(), session);
        if (cutscene.hasCamera()) {
            hud.attach(player, session);
        }
        if (cutscene.hasDialogue()) {
            // With a camera the player is already gated by spectator and the letterbox owns the screen, so the
            // conversation must not freeze them again, claim the action bar, or draw a second backdrop. Without one
            // there is nothing to composite into, and the conversation presents itself exactly as it always has.
            conversations.start(player, cutscene.getDialogue(),
                    cutscene.hasCamera() ? ConversationOptions.managedByCaller() : ConversationOptions.standalone());
        }
        ensureTicking();
        return true;
    }

    /**
     * Takes the player's camera for a cutscene's camera track.
     * <p>
     * Every beat's marker is resolved before anything visible happens, and a single missing one refuses the whole
     * cutscene: a camera track that breaks at beat six has already put the player in spectator, and putting them back
     * from there is a worse outcome than never having started.
     *
     * @return true if the camera was taken
     */
    private boolean openCamera(@NotNull Player player, @NotNull CutsceneSession session, @NotNull Cinematic cinematic) {
        final World world = player.getWorld();
        final List<CameraPose> poses = new ArrayList<>(cinematic.getBeats().size());
        for (Beat beat : cinematic.getBeats()) {
            final Optional<CameraPose> pose = markers.pose(world, beat.getId());
            if (pose.isEmpty()) {
                log.warn("Cutscene {} wants camera marker '{}', which world '{}' does not declare",
                        session.getCutsceneId(), beat.getId(), world.getName()).submit();
                UtilMessage.simpleMessage(player, "Cutscene", "That cutscene is not set up in this world.");
                return false;
            }
            poses.add(pose.get());
        }

        // Persisted before the game mode changes. Everything after this line is recoverable precisely because it ran.
        origins.put(CutsceneOrigin.capture(player.getUniqueId(), session.getCutsceneId(),
                player.getLocation(), player.getGameMode()));

        // The viewer's own body moves first, and keeps moving for the rest of the cutscene. Spectating an entity
        // changes what the client renders from, not where the player is: leave the body behind and the camera looks
        // out over chunks the server is not streaming to them, which reads as an empty world.
        final Location start = poses.getFirst().toLocation(world);
        player.teleport(start);
        player.setGameMode(GameMode.SPECTATOR);

        final CameraMarker marker = new CameraMarker(start);
        session.attachCamera(cinematic, poses, marker);
        session.enterBeat(0);

        // Deferred a tick so the marker's spawn packet has been flushed before the client is told to spectate it.
        UtilServer.runTaskLater(core, () -> {
            if (player.isOnline() && !session.isFinished()) {
                marker.attach(player);
            }
        }, 1L);
        return true;
    }

    /**
     * Grows a camera track on a cutscene that did not have one - the "a conversation starts a cinematic" case.
     * <p>
     * The conversation is not ended and not restarted: it carries on from the node it is on while the camera takes
     * over around it. That is only expressible because the two are peers inside one session rather than one owning
     * the other, and it is why a response uses this instead of the old hand-off that closed the dialogue first.
     *
     * @return true if the camera was taken
     */
    public boolean beginCamera(@NotNull Player player, @NotNull Cinematic cinematic) {
        final CutsceneSession session = sessions.get(player.getUniqueId());
        if (session == null || session.hasCamera() || cinematic.isEmpty()) {
            return false;
        }
        if (!openCamera(player, session, cinematic)) {
            return false;
        }
        conversations.manage(player);
        hud.attach(player, session);
        return true;
    }

    /**
     * Jumps a live session straight to a named beat, leaving the current one properly on the way out.
     * <p>
     * Built for authoring rather than for content: re-watching a shot deep in a timeline otherwise costs the whole
     * run-up every time it is adjusted.
     *
     * @return true if the beat existed and the camera moved to it
     */
    public boolean jumpTo(@NotNull Player player, @NotNull String beatId) {
        final CutsceneSession session = sessions.get(player.getUniqueId());
        if (session == null || session.getCamera() == null) {
            return false;
        }
        final int index = session.getCamera().indexOf(beatId);
        if (index < 0) {
            return false;
        }
        session.exitEffects();
        session.enterBeat(index);
        return true;
    }

    /**
     * Keeps the viewer's body with the camera, and keeps them welded to it.
     * <p>
     * Only on drift rather than every tick: a teleport fires a PlayerTeleportEvent through every listener on the
     * server, and twenty a second for the length of a cutscene is a real cost for no gain - the body only has to be
     * close enough that the chunks around the shot are streaming. Re-asserting the target straight afterwards matters
     * because a teleport can drop the client's camera back onto the player themselves.
     */
    private void followCamera(@NotNull Player player, @NotNull CutsceneSession session) {
        final CameraMarker marker = session.getMarker();
        if (marker == null) {
            return;
        }

        final Location camera = marker.getLocation();
        if (!player.getWorld().equals(camera.getWorld())
                || player.getLocation().distanceSquared(camera) > FOLLOW_DISTANCE_SQUARED) {
            player.teleport(camera);
        }
        marker.reattach(player);
    }

    /** Ends a cutscene early, restoring the viewer exactly as a natural ending would. */
    public void stop(@NotNull Player player) {
        final CutsceneSession session = sessions.get(player.getUniqueId());
        if (session != null) {
            finish(session, false);
        }
    }

    /**
     * Acts on a skip request, if this player's current beat permits that scope.
     *
     * @return true if something was actually skipped
     */
    public boolean skip(@NotNull Player player, @NotNull SkipScope scope) {
        final CutsceneSession session = sessions.get(player.getUniqueId());
        if (session == null || !session.skipVerdict(scope).isAllowed()) {
            return false;
        }

        // A question the player has been asked is not something a beat skip may answer on their behalf; only skipping
        // the cutscene outright is a clear enough instruction to take an answer for them.
        if (scope == SkipScope.BEAT && conversations.isAwaitingChoice(player)) {
            return false;
        }

        if (scope == SkipScope.CUTSCENE) {
            finish(session, true);
            return true;
        }
        if (!session.advance()) {
            finish(session, true);
        }
        return true;
    }

    /** Drives every live session one tick. Started with the first session and stopped with the last. */
    private void ensureTicking() {
        if (task != null && !task.isCancelled()) {
            return;
        }
        task = new BukkitRunnable() {
            @Override
            public void run() {
                for (CutsceneSession session : List.copyOf(sessions.values())) {
                    tickSession(session);
                }
                if (sessions.isEmpty()) {
                    cancel();
                    task = null;
                }
            }
        }.runTaskTimer(core, 0L, 1L);
    }

    private void tickSession(@NotNull CutsceneSession session) {
        final Player player = session.getPlayer();
        if (player == null) {
            abandon(session);
            return;
        }

        if (session.hasCamera()) {
            followCamera(player, session);
        }

        readSkipInput(session, player);
        if (session.isFinished()) {
            return;
        }
        if (!session.tick()) {
            finish(session, true);
        }
    }

    /**
     * Turns SNEAK into a skip. A tap skips the beat, a hold skips the cutscene - and the hold fires the moment it is
     * long enough rather than on release, so the player is not left wondering whether it took.
     */
    private void readSkipInput(@NotNull CutsceneSession session, @NotNull Player player) {
        if (!player.isSneaking()) {
            final int held = session.getSneakTicks();
            session.setSneakTicks(0);
            if (held > 0 && held < SKIP_HOLD_TICKS) {
                skip(player, SkipScope.BEAT);
            }
            return;
        }

        session.setSneakTicks(session.getSneakTicks() + 1);
        if (session.getSneakTicks() == SKIP_HOLD_TICKS) {
            skip(player, SkipScope.CUTSCENE);
        }
    }

    /** Ends a session and gives the viewer back their body. */
    private void finish(@NotNull CutsceneSession session, boolean completed) {
        if (session.isFinished()) {
            return;
        }
        session.setFinished(true);
        sessions.remove(session.getViewerId());
        session.exitEffects();
        session.emit(Signal.of("cutscene:end"));

        final Player player = session.getPlayer();
        if (player != null) {
            if (session.getDefinition().hasDialogue() && conversations.inConversation(player)) {
                // Run the rest of the dialogue out rather than dropping it: its responses carry the things the
                // cutscene was there to hand over, and a skip should cost the reading, not the rewards.
                if (completed) {
                    conversations.fastForward(player);
                } else {
                    conversations.end(player);
                }
            }
            hud.detach(player, session);
            restore(player, session);
            if (completed && session.getDefinition().isRecordView()) {
                views.record(player, session.getCutsceneId());
            }
        }
        session.getCompletion().complete(completed);
    }

    /** Puts the viewer back where they were and drops the durable record that said where that was. */
    private void restore(@NotNull Player player, @NotNull CutsceneSession session) {
        final CameraMarker marker = session.getMarker();
        if (marker == null) {
            return;
        }

        marker.release(player);
        final CutsceneOrigin origin = origins.get(player.getUniqueId()).orElse(null);
        final Location location = origin == null ? null : origin.toLocation();
        if (location != null) {
            player.teleport(location);
        }
        if (origin != null) {
            player.setGameMode(origin.toGameMode());
        }
        origins.remove(player.getUniqueId());
        marker.remove();
    }

    /** Drops a session whose viewer is gone, leaving the durable origin so their next join restores them. */
    private void abandon(@NotNull CutsceneSession session) {
        session.setFinished(true);
        sessions.remove(session.getViewerId());
        session.exitEffects();
        final Player player = session.getPlayer();
        if (player != null) {
            hud.detach(player, session);
        }
        final CameraMarker marker = session.getMarker();
        if (marker != null) {
            marker.remove();
        }
        session.getCompletion().complete(false);
    }

    /**
     * Restores a viewer who logged out or crashed mid-cutscene. Runs on join for anyone holding an origin record,
     * whether or not this server process ever knew about their session.
     */
    public void restoreIfStranded(@NotNull Player player) {
        final CutsceneOrigin origin = origins.get(player.getUniqueId()).orElse(null);
        if (origin == null) {
            return;
        }

        final Location location = origin.toLocation();
        // Deferred a tick: changing game mode and teleporting inside the join event itself is unreliable.
        UtilServer.runTaskLater(core, () -> {
            player.setSpectatorTarget(null);
            if (location != null) {
                player.teleport(location);
            }
            player.setGameMode(origin.toGameMode());
            origins.remove(player.getUniqueId());
            log.info("Restored {} after a disconnect during cutscene {}", player.getName(), origin.getCutscene()).submit();
        }, 2L);
    }

    /** @return a future completing when this player's cutscene ends, true only if it ran to its end. */
    public @NotNull CompletableFuture<Boolean> completion(@NotNull Player player) {
        final CutsceneSession session = sessions.get(player.getUniqueId());
        return session == null ? CompletableFuture.completedFuture(false) : session.getCompletion();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        views.load(event.getPlayer().getUniqueId());
        restoreIfStranded(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        views.unload(event.getPlayer().getUniqueId());
        final CutsceneSession session = sessions.get(event.getPlayer().getUniqueId());
        if (session != null) {
            abandon(session);
        }
    }

    /** Pressing ESC would otherwise drop the viewer out of the camera and leave them floating mid-cutscene. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onStopSpectating(PlayerStopSpectatingEntityEvent event) {
        if (isWatching(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    /** Spectator teleports are the client asking to jump to another entity; a cutscene camera is not up for debate. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.SPECTATE && isWatching(event.getPlayer())) {
            event.setCancelled(true);
        }
    }
}
