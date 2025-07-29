package me.mykindos.betterpvp.game.framework.listener.state;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.discord.DiscordMessage;
import me.mykindos.betterpvp.core.discord.DiscordWebhook;
import me.mykindos.betterpvp.core.discord.embeds.EmbedField;
import me.mykindos.betterpvp.core.discord.embeds.EmbedFooter;
import me.mykindos.betterpvp.core.discord.embeds.EmbedObject;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilServer;
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

import java.awt.Color;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static me.mykindos.betterpvp.core.utilities.Resources.Font.SMALL_CAPS;

/**
 * Handles slow player movement when the game is ending, cool effect
 */
@BPvPListener
@CustomLog
@Singleton
public class EndingStateHandler implements Listener {

    @Inject
    @Config(path = "webhook.game-results", defaultValue = "")
    private String gameResultsWebhook;

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

        if (gameResultsWebhook != null && !gameResultsWebhook.isEmpty()) {
            UtilServer.runTaskAsync(plugin, () -> {
                sendGameResultsWebhook(game);
            });
        }
    }

    private void sendGameResultsWebhook(AbstractGame<?, ?> game) {
        try {
            DiscordWebhook webhook = new DiscordWebhook(gameResultsWebhook);

            boolean hasWinners = !game.getWinners().isEmpty();

            // Create embed
            EmbedObject.EmbedObjectBuilder embedBuilder = EmbedObject.builder();

            if (hasWinners) {
                embedBuilder.title("🏆 Game Victory!")
                        .description("The game has ended with a winner!")
                        .color(new Color(255, 213, 0)); // Gold color for victory
            } else {
                embedBuilder.title("⚔️ Game Draw!")
                        .description("The game has ended in a draw!")
                        .color(new Color(150, 150, 150)); // Gray color for draw
            }

            // Add game type field
            embedBuilder.field(EmbedField.builder()
                    .name("📋 Game Type")
                    .value(game.getClass().getSimpleName().replace("Game", ""))
                    .inline(true)
                    .build());

            // Separate winners and losers for counting
            List<String> winnerNames = new ArrayList<>();
            List<String> loserNames = new ArrayList<>();

            for (Audience participant : game.getParticipants()) {
                boolean isWinner = game.getWinners().contains(participant);
                participant.forEachAudience(audience -> {
                    if (audience instanceof Player player) {
                        if(isWinner) {
                            winnerNames.add("🏆 " + player.getName());
                        } else {
                            loserNames.add("⚔️ " + player.getName());
                        }
                    }
                });
            }

            // Add team counts instead of total participants
            if (hasWinners) {
                embedBuilder.field(EmbedField.builder()
                        .name("🏆 Winners")
                        .value(String.valueOf(winnerNames.size()))
                        .inline(true)
                        .build());

                embedBuilder.field(EmbedField.builder()
                        .name("⚔️ Losers")
                        .value(String.valueOf(loserNames.size()))
                        .inline(true)
                        .build());
            } else {
                embedBuilder.field(EmbedField.builder()
                        .name("👥 Total Players")
                        .value(String.valueOf(game.getParticipants().size()))
                        .inline(true)
                        .build());
            }

            // Add timestamp field
            embedBuilder.field(EmbedField.builder()
                    .name("⏰ Game Ended")
                    .value(LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy - HH:mm:ss")))
                    .inline(true)
                    .build());

            // Add winners section (if any)
            if (hasWinners && !winnerNames.isEmpty()) {
                String winnersText = String.join("\n", winnerNames);
                addFieldWithChunking(embedBuilder, "🏆 Winning Team", winnersText, 1000);
            }

            // Add losers/other participants section
            if (!loserNames.isEmpty()) {
                String losersText = String.join("\n", loserNames);
                String fieldName = hasWinners ? "⚔️ Losing Team" : "👥 All Players";
                addFieldWithChunking(embedBuilder, fieldName, losersText, 1000);
            }

            if (!playerController.getSpectators().isEmpty()) {
                String spectatorsText = String.join("\n", playerController.getSpectators().keySet().stream().map(Player::getName).toList());
                String fieldName = "Spectators";
                addFieldWithChunking(embedBuilder, fieldName, spectatorsText, 1000);
            }

            // Add footer
            embedBuilder.footer(new EmbedFooter("Champions Game Results", ""));
            embedBuilder.color(Color.GREEN);

            // Send the webhook
            webhook.send(DiscordMessage.builder()
                    .username("Game Results")
                    .embed(embedBuilder.build())
                    .build());

        } catch (Exception e) {
            log.warn("Failed to send game results webhook", e).submit();
        }
    }

    private void addFieldWithChunking(EmbedObject.EmbedObjectBuilder embedBuilder, String fieldName, String content,
                                      int maxLength) {
        if (content.length() <= maxLength) {
            embedBuilder.field(EmbedField.builder()
                    .name(fieldName)
                    .value("```\n" + content + "\n```")
                    .inline(false)
                    .build());
        } else {
            List<String> chunks = splitIntoChunks(content, maxLength - 10); // Leave room for code block formatting
            for (int i = 0; i < chunks.size(); i++) {
                String chunkFieldName = i == 0 ? fieldName : fieldName + " (cont.)";
                embedBuilder.field(EmbedField.builder()
                        .name(chunkFieldName)
                        .value("```\n" + chunks.get(i) + "\n```")
                        .inline(false)
                        .build());
            }
        }
    }


    private List<String> splitIntoChunks(String text, int chunkSize) {
        List<String> chunks = new ArrayList<>();
        String[] lines = text.split("\n");
        StringBuilder currentChunk = new StringBuilder();

        for (String line : lines) {
            if (currentChunk.length() + line.length() + 1 > chunkSize) {
                if (!currentChunk.isEmpty()) {
                    chunks.add(currentChunk.toString());
                    currentChunk = new StringBuilder();
                }
            }
            if (!currentChunk.isEmpty()) {
                currentChunk.append("\n");
            }
            currentChunk.append(line);
        }

        if (!currentChunk.isEmpty()) {
            chunks.add(currentChunk.toString());
        }

        return chunks;
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