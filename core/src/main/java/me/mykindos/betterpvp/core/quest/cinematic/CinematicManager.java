package me.mykindos.betterpvp.core.quest.cinematic;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.quest.event.QuestMilestoneEvent;
import me.mykindos.betterpvp.core.quest.primitive.QuestPrimitiveHandlers;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Plays cinematics as an approximation (no packet camera yet): the player is
 * frozen, the camera is driven by per-tick teleports along camera keyframes,
 * and subtitle / sound / event keyframes fire on their tick. Started via the
 * {@code action.start_cinematic} primitive (registered here).
 */
@Singleton
@BPvPListener
@CustomLog
public class CinematicManager implements Listener {

    private final Core core;
    private final CinematicRegistry registry;
    private final EffectManager effectManager;

    private final Map<UUID, BukkitRunnable> running = new ConcurrentHashMap<>();
    private final Map<UUID, Location> origins = new ConcurrentHashMap<>();

    @Inject
    public CinematicManager(Core core, CinematicRegistry registry, EffectManager effectManager, QuestPrimitiveHandlers handlers) {
        this.core = core;
        this.registry = registry;
        this.effectManager = effectManager;
        handlers.registerAction("action.start_cinematic", (player, data) -> play(player, data.getString("cinematic")));
    }

    public void play(Player player, String cinematicId) {
        if (cinematicId == null) return;
        CinematicDefinition def = registry.get(cinematicId).orElse(null);
        if (def == null) {
            log.warn("Tried to play unknown cinematic {}", cinematicId).submit();
            return;
        }
        final UUID uuid = player.getUniqueId();
        cancel(uuid);
        origins.put(uuid, player.getLocation().clone());
        effectManager.addEffect(player, null, EffectTypes.FROZEN, "Cinematic", 1, (def.getDurationTicks() * 50L) + 2000, true, true);

        BukkitRunnable runnable = new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                Player online = Bukkit.getPlayer(uuid);
                if (online == null) {
                    cancel();
                    running.remove(uuid);
                    origins.remove(uuid);
                    return;
                }
                applyTick(online, def, tick);
                if (tick++ >= def.getDurationTicks()) {
                    cancel();
                    finish(online);
                }
            }
        };
        running.put(uuid, runnable);
        runnable.runTaskTimer(core, 0L, 1L);
    }

    private void applyTick(Player player, CinematicDefinition def, int tick) {
        for (CinTrack track : def.getTracks()) {
            for (CinKeyframe keyframe : track.getKeyframes()) {
                if (keyframe.getTick() != tick) continue;
                switch (track.getKind()) {
                    case "camera" -> player.teleport(new Location(player.getWorld(),
                            keyframe.getDouble("x", player.getX()), keyframe.getDouble("y", player.getY()),
                            keyframe.getDouble("z", player.getZ()),
                            (float) keyframe.getDouble("yaw", 0), (float) keyframe.getDouble("pitch", 0)));
                    case "subtitle" -> {
                        int fade = (int) keyframe.getDouble("fadeTicks", 10);
                        player.showTitle(Title.title(Component.empty(),
                                Component.text(keyframe.getString("text") == null ? "" : keyframe.getString("text")),
                                Title.Times.times(Duration.ofMillis(fade * 50L), Duration.ofSeconds(3), Duration.ofMillis(fade * 50L))));
                    }
                    case "sound" -> {
                        String key = keyframe.getString("key");
                        if (key != null && !key.isBlank()) {
                            player.playSound(player.getLocation(), key,
                                    (float) keyframe.getDouble("volume", 1), (float) keyframe.getDouble("pitch", 1));
                        }
                    }
                    case "action" -> UtilServer.callEvent(new QuestMilestoneEvent(player, keyframe.getString("eventKey")));
                    default -> { /* unknown track kind */ }
                }
            }
        }
    }

    private void finish(Player player) {
        final UUID uuid = player.getUniqueId();
        running.remove(uuid);
        effectManager.removeEffect(player, EffectTypes.FROZEN);
        player.clearTitle();
        Location origin = origins.remove(uuid);
        if (origin != null) {
            player.teleport(origin);
        }
    }

    private void cancel(UUID uuid) {
        BukkitRunnable existing = running.remove(uuid);
        if (existing != null) existing.cancel();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cancel(event.getPlayer().getUniqueId());
        origins.remove(event.getPlayer().getUniqueId());
    }
}
