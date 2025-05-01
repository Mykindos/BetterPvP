package me.mykindos.betterpvp.game.framework.listener.state;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.display.TitleComponent;
import me.mykindos.betterpvp.game.GamePlugin;
import me.mykindos.betterpvp.game.framework.AbstractGame;
import me.mykindos.betterpvp.game.framework.ServerController;
import me.mykindos.betterpvp.game.framework.model.player.PlayerController;
import me.mykindos.betterpvp.game.framework.state.GameState;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

import static me.mykindos.betterpvp.core.utilities.Resources.Font.SMALL_CAPS;

/**
 * Handles slow player movement when the game is ending, cool effect
 */
@BPvPListener
@CustomLog
@Singleton
public class EndingStateHandler implements Listener {

    private final ServerController serverController;
    private final PlayerController playerController;
    private final ClientManager clientManager;
    private final GamePlugin plugin;

    @Inject
    public EndingStateHandler(ServerController serverController, PlayerController playerController, ClientManager clientManager, GamePlugin plugin) {
        this.serverController = serverController;
        this.playerController = playerController;
        this.clientManager = clientManager;
        this.plugin = plugin;
        setupStateHandlers();
    }

    public void setupStateHandlers() {
        // Slow people down
        serverController.getStateMachine().addEnterHandler(GameState.ENDING, oldState -> {
            final AbstractGame<?, ?> game = serverController.getCurrentGame();

            // Announce game end and cleanup
            announceEnd(game.getWinners(), game);

            // Title
            showTitle(game);

            // Sounds
            playSounds(game);

            // Slow effect
            Bukkit.getServer().getServerTickManager().setTickRate(5L);
        });

        // Speed people up
        serverController.getStateMachine().addExitHandler(GameState.ENDING, oldState -> {
            Bukkit.getServer().getServerTickManager().setTickRate(20L);

            for (Player player : playerController.getParticipants().keySet()) {
                player.getInventory().clear();
            }
        });
    }

    private void showTitle(AbstractGame<?, ?> game) {
        final Component winnerTitle = Component.text("VICTORY", TextColor.color(255, 213, 0), TextDecoration.BOLD).font(SMALL_CAPS);
        final Component loserTitle = Component.text("DEFEAT", TextColor.color(204, 3, 0), TextDecoration.BOLD).font(SMALL_CAPS);
        for (Audience participant : game.getParticipants()) {
            final Component title = game.getWinners().contains(participant) ? winnerTitle : loserTitle;
            participant.forEachAudience(audience -> {
                if (audience instanceof Player player) {
                    final Gamer gamer = clientManager.search().online(player).getGamer();
                    gamer.getTitleQueue().add(-10, TitleComponent.title(0, 6, 1, false, gmr -> title));
                }
            });
        }

        for (Player player : playerController.getSpectators().keySet()) {
            final Gamer gamer = clientManager.search().online(player).getGamer();
            gamer.getTitleQueue().add(-10, TitleComponent.title(0, 6, 1, false,
                    gmr -> Component.text("Game Ended", NamedTextColor.YELLOW)));
        }
    }

    private void playSounds(AbstractGame<?, ? extends Audience> game) {
        final SoundEffect victory = new SoundEffect("betterpvp", "game.victory");
        final SoundEffect defeat = new SoundEffect("betterpvp", "game.defeat");
        final List<? extends Audience> winners = game.getWinners();
        for (Audience participant : game.getParticipants()) {
            if (winners.contains(participant)) {
                victory.play(participant);
            } else {
                defeat.play(participant);
            }
        }
        new SoundEffect(Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 10f).broadcast();
        final List<SoundEffect> sounds = List.of(
                new SoundEffect(Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1f, 10f),
                new SoundEffect(Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 1f, 10f),
                new SoundEffect(Sound.ENTITY_FIREWORK_ROCKET_SHOOT, 1f, 10f)
        );
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                ticks++;

                if (ticks > 3 * 20) {
                    cancel();
                    return;
                }

                if (ticks % 2 == 0) {
                    SoundEffect sound = sounds.get((int) (Math.random() * sounds.size()));
                    sound.broadcast();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void announceEnd(List<? extends Audience> winners, AbstractGame<?, ? extends Audience> game) {
        Bukkit.broadcast(Component.text(" ".repeat(50), NamedTextColor.GREEN, TextDecoration.STRIKETHROUGH));
        Bukkit.broadcast(Component.empty());
        Bukkit.broadcast(Component.text("Game Ended", NamedTextColor.YELLOW, TextDecoration.BOLD));
        if (winners.isEmpty()) {
            Bukkit.broadcast(Component.text("Nobody won the game..."));
        } else {
            for (int i = 0; i < winners.size(); i++) {
                Bukkit.broadcast(game.getWinnerDescription());
            }
        }
        Bukkit.broadcast(Component.empty());
        Bukkit.broadcast(Component.text(" ".repeat(50), NamedTextColor.GREEN, TextDecoration.STRIKETHROUGH));
    }
}