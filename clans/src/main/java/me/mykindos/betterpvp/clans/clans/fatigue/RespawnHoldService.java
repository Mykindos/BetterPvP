package me.mykindos.betterpvp.clans.clans.fatigue;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.fatigue.punishment.FatiguePunishment;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.display.title.TitleComponent;
import me.mykindos.betterpvp.core.world.model.BPvPWorld;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Owns the void-world "you are recovering" hold. Mirrors the game module's
 * {@code ParticipantRespawnHandler} pattern (deferred respawn + tick-up
 * countdown with title/chat/vignette/heartbeat) but for clans, and adds a
 * single authoritative failsafe so a player can never get stuck "respawning".
 * <p>
 * Immobilisation and invulnerability are free: {@code VoidWorldListener} already
 * cancels movement and damage for non-ops in the void world.
 */
@Singleton
@BPvPListener
@CustomLog
public class RespawnHoldService implements Listener {

    /** Per-player active hold. */
    private static final class HoldSession {
        private FatigueTier tier;
        private long endTime;
        private BukkitTask task;
        private boolean teleported; // true once we've placed them in the void
        /** Where the player would have respawned if not for the hold (the true home). */
        private Location returnLocation;
    }

    private final Clans plugin;
    private final ClientManager clientManager;
    private final BattleFatigueManager fatigueManager;
    private final Set<FatiguePunishment> punishments;
    private final BattleFatigueMessages messages;

    private final Map<UUID, HoldSession> sessions = new ConcurrentHashMap<>();
    private final World voidWorld;

    @Inject
    @Config(path = "clans.fatigue.hold.secondsPerTier", defaultValue = "7.0")
    private double holdSecondsPerTier;

    @Inject
    public RespawnHoldService(Clans plugin,
                              ClientManager clientManager,
                              BattleFatigueManager fatigueManager,
                              Set<FatiguePunishment> punishments,
                              BattleFatigueMessages messages) {
        this.plugin = plugin;
        this.clientManager = clientManager;
        this.fatigueManager = fatigueManager;
        this.punishments = punishments;
        this.messages = messages;
        this.voidWorld = new BPvPWorld(BPvPWorld.VOID_WORLD_NAME).getWorld();
    }

    public boolean isHeld(Player player) {
        return sessions.containsKey(player.getUniqueId());
    }

    /**
     * Begin holding a player who just died at a hold-worthy tier. We force a
     * respawn shortly after death (the game module's trick); {@link #onRespawn}
     * then captures wherever the normal respawn listeners decided to send them
     * and detours them through the void for the hold.
     */
    public void beginHold(Player player, FatigueTier tier) {
        if (isHeld(player)) {
            return;
        }

        final HoldSession session = new HoldSession();
        session.tier = tier;
        session.endTime = System.currentTimeMillis() + (long) (holdSecondsPerTier * tier.ordinal() * 1000L);
        sessions.put(player.getUniqueId(), session);
        fatigueManager.getOrCreate(player.getUniqueId()).setRespawnHold(true);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline() && player.isDead()) {
                    player.spigot().respawn();
                }
            }
        }.runTaskLater(plugin, 3L);
    }

    /**
     * Runs at {@code MONITOR}, after every other respawn listener
     * (notably {@code VoidWorldListener} at {@code HIGHEST}) has had its say.
     * We therefore read the <i>final</i> respawn location — the player's true
     * home, whether that's a clan core, a bed, or world spawn — capture it for
     * the release, and only then redirect this respawn into the void for the
     * hold. We never reimplement or know the home logic; we just use its output.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        final Player player = event.getPlayer();
        final HoldSession session = sessions.get(player.getUniqueId());
        if (session == null) {
            return;
        }

        // Capture the real destination the normal listeners settled on, then
        // detour into the void so the player materialises straight in the hold.
        session.returnLocation = event.getRespawnLocation().clone();
        event.setRespawnLocation(voidWorld.getSpawnLocation());

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !sessions.containsKey(player.getUniqueId())) {
                    return;
                }
                player.teleport(voidWorld.getSpawnLocation());
                player.setGameMode(GameMode.ADVENTURE);
                startCountdown(player, session);
            }
        }.runTaskLater(plugin, 1L);
    }

    private void startCountdown(Player player, HoldSession session) {
        session.teleported = true;
        final Gamer gamer = clientManager.search().online(player).getGamer();
        messages.onRespawnHoldStart(player, session.tier, remainingSeconds(session));

        session.task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !sessions.containsKey(player.getUniqueId())) {
                    cancel();
                    return;
                }

                final double remaining = remainingSeconds(session);
                if (remaining <= 0.0) {
                    cancel();
                    release(player, session); // returns them to the captured home
                    return;
                }

                // Live countdown title + escalating heartbeat (game-module feel).
                gamer.getTitleQueue().add(10, TitleComponent.subtitle(0, 1.15, 0.0, false,
                        gmr -> Component.text(String.format("Recovering... %.0f", Math.ceil(remaining)), NamedTextColor.RED)));
                UtilPlayer.setWarningEffect(player, 1);
                if (remaining <= 3.0) {
                    new SoundEffect(Sound.ENTITY_WARDEN_HEARTBEAT, 0.9f, 1.4f).play(player);
                } else {
                    new SoundEffect(Sound.ENTITY_WARDEN_HEARTBEAT, 0.7f, 1f).play(player);
                }
            }
        }.runTaskTimer(plugin, 1L, 20L);
    }

    private double remainingSeconds(HoldSession session) {
        return Math.max(0.0, (session.endTime - System.currentTimeMillis()) / 1000.0);
    }

    /**
     * The single authoritative cleanup path. Whether the timer finished, the
     * player was teleported out, the task died, or the failsafe caught them —
     * everything funnels through here exactly once.
     */
    private void release(Player player, HoldSession session, Location destination) {
        if (sessions.remove(player.getUniqueId()) == null) {
            return; // already released by another path
        }
        if (session.task != null) {
            session.task.cancel();
        }

        fatigueManager.getOrCreate(player.getUniqueId()).setRespawnHold(false);

        if (player.isOnline()) {
            UtilPlayer.clearWarningEffect(player);
            player.setInvulnerable(false);
            player.setGameMode(GameMode.SURVIVAL);
            if (destination != null) {
                player.teleport(destination);
            }

            new SoundEffect(Sound.BLOCK_BEACON_POWER_SELECT, 1.4f, 0.7f).play(player.getLocation());

            for (FatiguePunishment punishment : punishments) {
                if (punishment.appliesTo(session.tier)) {
                    punishment.apply(player, session.tier);
                }
            }
            messages.onRespawnHoldRelease(player, session.tier);
        }
    }

    /**
     * Failsafe reconciler. Any held player who is offline, no longer in the void
     * world, or whose countdown task has died is force-released so nobody can
     * get stuck in the "respawning" state.
     */
    @UpdateEvent(delay = 1000)
    public void reconcile() {
        for (Map.Entry<UUID, HoldSession> entry : sessions.entrySet()) {
            final UUID uuid = entry.getKey();
            final HoldSession session = entry.getValue();
            final Player player = plugin.getServer().getPlayer(uuid);

            if (player == null || !player.isOnline()) {
                // Offline: clear flag, drop the session (state is session-only).
                fatigueManager.getOrCreate(uuid).setRespawnHold(false);
                if (session.task != null) {
                    session.task.cancel();
                }
                sessions.remove(uuid);
                continue;
            }

            if (!session.teleported) {
                continue; // still in the brief death→respawn window
            }

            final boolean inVoid = player.getWorld().getName().equals(BPvPWorld.VOID_WORLD_NAME);
            final boolean taskDead = session.task == null || session.task.isCancelled();
            if (!inVoid) {
                // Yanked out by something else — finalise where they are now.
                release(player, session, null);
            } else if (taskDead) {
                release(player, session);
            }
        }
    }

    /**
     * Decoupled compatibility with {@code VoidWorldListener}: it tries to open
     * the clan travel-hub menu for every non-op in the void every 500ms. While
     * a player is in our hold, we veto any GUI open so the recovery title/chat
     * is the only thing they see. Neither listener references the other — they
     * coordinate purely through this Bukkit event.
     */
    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player player && isHeld(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        event.getPlayer().setInvulnerable(false);
        final HoldSession session = sessions.remove(event.getPlayer().getUniqueId());
        if (session != null) {
            if (session.task != null) {
                session.task.cancel();
            }
            fatigueManager.getOrCreate(event.getPlayer().getUniqueId()).setRespawnHold(false);
        }
    }

    /**
     * Called by the countdown when it completes (and the failsafe). Returns the
     * player to the home we captured at respawn. The fallback should never be
     * needed — vanilla/bed/clan respawn is never the void — but guards against a
     * lost capture so we can never strand anyone.
     */
    private void release(Player player, HoldSession session) {
        Location destination = session.returnLocation;
        if (destination == null || destination.getWorld() == null
                || destination.getWorld().equals(voidWorld)) {
            destination = plugin.getServer().getWorlds().get(0).getSpawnLocation();
        }
        release(player, session, destination);
    }
}
