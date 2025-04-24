package me.mykindos.betterpvp.game.framework.listener.player;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.display.TitleComponent;
import me.mykindos.betterpvp.game.GamePlugin;
import me.mykindos.betterpvp.game.framework.AbstractGame;
import me.mykindos.betterpvp.game.framework.ServerController;
import me.mykindos.betterpvp.game.framework.listener.player.event.ParticipantPreRespawnEvent;
import me.mykindos.betterpvp.game.framework.listener.player.event.ParticipantRespawnEvent;
import me.mykindos.betterpvp.game.framework.model.attribute.bound.RespawnTimerAttribute;
import me.mykindos.betterpvp.game.framework.model.attribute.bound.RespawnsAttribute;
import me.mykindos.betterpvp.game.framework.model.player.Participant;
import me.mykindos.betterpvp.game.framework.model.player.PlayerController;
import me.mykindos.betterpvp.game.framework.model.player.event.ParticipantDeathEvent;
import me.mykindos.betterpvp.game.framework.model.player.event.ParticipantStartSpectatingEvent;
import me.mykindos.betterpvp.game.framework.state.GameState;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@BPvPListener
@Singleton
public class ParticipantRespawnHandler implements Listener {

    private final GamePlugin plugin;
    private final ClientManager clientManager;
    private final PlayerController playerController;
    private final ServerController serverController;
    private final Map<Player, BukkitTask> respawnTasks = new ConcurrentHashMap<>();

    @Inject
    public ParticipantRespawnHandler(GamePlugin plugin, ClientManager clientManager, PlayerController playerController, ServerController serverController) {
        this.plugin = plugin;
        this.clientManager = clientManager;
        this.playerController = playerController;
        this.serverController = serverController;
        setupStateHandlers();
    }

    private void setupStateHandlers() {
        // When game ends, cancel all tasks to respawn
        serverController.getStateMachine().addExitHandler(GameState.ENDING, oldState -> clearTasks());
    }

    private void clearTasks() {
        respawnTasks.values().forEach(BukkitTask::cancel);
        respawnTasks.clear();
    }

    @EventHandler
    public void onDeath(ParticipantDeathEvent event) {
        if (serverController.getCurrentState() != GameState.IN_GAME) {
            return;
        }

        final AbstractGame<?, ?> game = serverController.getCurrentGame();
        final RespawnsAttribute respawnsAttribute = game.getAttribute(RespawnsAttribute.class);
        if (respawnsAttribute.getValue()) {
            final Player player = event.getPlayer();
            final Participant participant = playerController.getParticipant(player);

            // Call event
            final ParticipantPreRespawnEvent preRespawnEvent = new ParticipantPreRespawnEvent(participant, player);
            preRespawnEvent.callEvent();
            if (preRespawnEvent.isCancelled()) {
                return;
            }

            // Respawn later
            final Gamer gamer = clientManager.search().online(player).getGamer();
            final RespawnTimerAttribute respawnTimerAttribute = game.getAttribute(RespawnTimerAttribute.class);
            BukkitTask task = new BukkitRunnable() {
                long ticks = 0; // Start at 0 and count up

                @Override
                public void run() {
                    final long totalTicks = (long) (respawnTimerAttribute.getValue() * 20); // Convert seconds to ticks

                    ticks++; // Count up instead of down
                    if (ticks >= totalTicks) { // Check if we've counted up to the target time
                        // State
                        playerController.setAlive(player, participant, true);

                        // FX
                        UtilPlayer.clearWarningEffect(player);
                        new SoundEffect(Sound.BLOCK_BEACON_POWER_SELECT, 1.4f, 0.7f).play(player.getLocation());
                        new SoundEffect(Sound.ENTITY_WARDEN_SONIC_BOOM, 1.4f, 0.2f).play(player.getLocation());
                        Particle.SOUL.builder()
                                .location(player.getLocation())
                                .count(20)
                                .offset(0.4, 1, 0.4)
                                .extra(0)
                                .receivers(60)
                                .spawn();

                        // Call event
                        new ParticipantRespawnEvent(participant, player).callEvent();

                        cancel();
                        return;
                    }

                    double interval = 30;
                    if (ticks  >= totalTicks * 3 / 4) { // Reworked intervals based on percentage complete
                        interval = 5;
                    } else if (ticks >= totalTicks / 2) {
                        UtilPlayer.setWarningEffect(player, 1);
                        interval = 10;
                    } else if (ticks >= totalTicks / 3) {
                        interval = 20;
                    }

                    if (ticks % interval == 0) {
                        new SoundEffect(Sound.ENTITY_WARDEN_HEARTBEAT, 0.8f, 1f).play(player);
                    }
                }
            }.runTaskTimer(plugin, 1, 1);

            // Save task
            respawnTasks.put(player, task);

            // Cues
            final String chatText = String.format("You will respawn in %.1f seconds...", respawnTimerAttribute.getValue());
            UtilMessage.message(player, Component.text(chatText, NamedTextColor.WHITE, TextDecoration.BOLD));

            gamer.getTitleQueue().add(10, TitleComponent.subtitle(0,
                    1.0,
                    0.5f,
                    false,
                    gmr -> Component.text(String.format("Respawning in %.1f seconds...", respawnTimerAttribute.getValue()), NamedTextColor.WHITE)));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        final Player player = event.getPlayer();
        final BukkitTask task = respawnTasks.remove(player);
        if (task != null) {
            task.cancel();
        }
    }

    @EventHandler
    public void onSpectate(ParticipantStartSpectatingEvent event) {
        final Player player = event.getPlayer();
        final BukkitTask task = respawnTasks.remove(player);
        if (task != null) {
            task.cancel();
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        UtilServer.runTaskLater(plugin, () -> {
            event.getPlayer().spigot().respawn();
        }, 3L);
    }

}
