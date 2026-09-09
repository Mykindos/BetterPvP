package me.mykindos.betterpvp.core.world.travel;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.display.title.TitleComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Owns every in-flight {@link Departure}. A departure holds a traveller in place for a short window, showing a
 * countdown title, before its arrival callback runs. Moving to a new block, taking damage, or quitting cancels it —
 * this is the travel package's own departure ceremony and does not touch the delayed-action framework.
 */
@BPvPListener
@Singleton
public class DepartureController implements Listener {

    private final ClientManager clientManager;
    private final Map<UUID, Departure> departures = new ConcurrentHashMap<>();

    @Inject
    public DepartureController(ClientManager clientManager) {
        this.clientManager = clientManager;
    }

    public boolean isDeparting(@NotNull Player player) {
        return departures.containsKey(player.getUniqueId());
    }

    /**
     * Starts a departure for {@code traveller} toward {@code destination}. Refuses re-entry if one is already active
     * for that player. {@code onArrive} runs once the countdown elapses; relocating the player is the caller's job.
     */
    public void begin(@NotNull Player traveller, @NotNull Destination destination, @NotNull Runnable onArrive) {
        if (isDeparting(traveller)) {
            return;
        }

        final Departure departure = new Departure(traveller, destination, onArrive, 3000L);
        departures.put(traveller.getUniqueId(), departure);
        new SoundEffect(Sound.BLOCK_PORTAL_TRIGGER, 0.8f, 1.2f).play(traveller);
        pushCountdown(traveller, departure);
    }

    private void interrupt(@NotNull Player traveller, @NotNull Component reason) {
        if (departures.remove(traveller.getUniqueId()) == null) {
            return;
        }
        UtilMessage.simpleMessage(traveller, "Travel", reason);
    }

    @UpdateEvent(delay = 100)
    public void tick() {
        for (Map.Entry<UUID, Departure> entry : departures.entrySet()) {
            final UUID uuid = entry.getKey();
            final Departure departure = entry.getValue();
            final Player traveller = departure.getTraveller();

            if (!traveller.isOnline()) {
                departures.remove(uuid);
                continue;
            }

            if (departure.isElapsed()) {
                departures.remove(uuid);
                departure.getOnArrive().run();
                continue;
            }

            pushCountdown(traveller, departure);
        }
    }

    private void pushCountdown(@NotNull Player traveller, @NotNull Departure departure) {
        final Gamer gamer = clientManager.search().online(traveller).getGamer();
        final double remainingSeconds = departure.remainingMillis() / 1000.0;
        gamer.getTitleQueue().add(10, TitleComponent.subtitle(0, 0.3, 0, false,
                gmr -> Component.text(String.format("Departing in %.1fs", remainingSeconds), NamedTextColor.AQUA)));
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!event.hasChangedBlock()) {
            return;
        }
        interrupt(event.getPlayer(), Component.text("Travel interrupted — you moved.", NamedTextColor.RED));
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        interrupt(player, Component.text("Travel interrupted — you took damage.", NamedTextColor.RED));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        departures.remove(event.getPlayer().getUniqueId());
    }
}
