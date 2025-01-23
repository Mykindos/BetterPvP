package me.mykindos.betterpvp.hub.feature;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.hub.Hub;
import org.bukkit.GameMode;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@BPvPListener
@Singleton
public class DoubleJumpListener implements Listener {

    @Inject
    private Hub hub;

    private final List<Player> doubleJumped = new ArrayList<>();

    private void queueFlight(Player player) {
        UtilServer.runTaskLater(hub, () -> {
            player.setAllowFlight(true);
        }, 1L);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        queueFlight(event.getPlayer());
    }

    @EventHandler
    public void onGamemodeChange(PlayerGameModeChangeEvent event) {
        queueFlight(event.getPlayer());
    }

    @EventHandler
    public void onToggleFlight(PlayerToggleFlightEvent event) {
        final Player player = event.getPlayer();
        if (player.getGameMode().equals(GameMode.CREATIVE)) {
            return; // creative players can still fly, and people who double jumped can't double jump again
        }

        // todo: zone system cancel event

        event.setCancelled(true);
        event.getPlayer().setAllowFlight(false);
        final Vector direction = player.getLocation().getDirection().multiply(new Vector(2, 1.5, 2));
        player.setVelocity(direction);
        Particle.CLOUD.builder()
                .location(player.getLocation())
                .count(10)
                .extra(0.2)
                .receivers(player)
                .offset(0.5, 0.5, 0.5)
                .spawn();
        new SoundEffect(Sound.ENTITY_BLAZE_SHOOT, 1, 0.5f).play(player);
        doubleJumped.add(player);
    }

    @UpdateEvent
    public void update() {
        final Iterator<Player> iterator = doubleJumped.iterator();
        while (iterator.hasNext()) {
            final Player player = iterator.next();
            if (player == null || !player.isValid()) {
                iterator.remove();
                continue;
            }

            if (UtilBlock.isGrounded(player)) {
                iterator.remove();
                queueFlight(player);
            }
        }
    }

}
