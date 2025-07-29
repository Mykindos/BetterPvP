package me.mykindos.betterpvp.game.mineplex.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mineplex.studio.sdk.modules.MineplexModuleManager;
import com.mineplex.studio.sdk.modules.level.MineplexLevelModule;
import com.mineplex.studio.sdk.modules.level.experience.ExperienceAwardResult;
import com.mineplex.studio.sdk.modules.level.experience.MineplexPlayerExperience;
import com.mineplex.studio.sdk.modules.level.session.MineplexExperienceSessionImpl;
import lombok.CustomLog;
import lombok.NonNull;
import me.mykindos.betterpvp.core.chat.events.ChatSentEvent;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.events.ClientJoinEvent;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.utilities.Resources;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.ProgressBar;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.game.GamePlugin;
import me.mykindos.betterpvp.game.framework.ServerController;
import me.mykindos.betterpvp.game.framework.model.player.PlayerController;
import me.mykindos.betterpvp.game.framework.state.GameState;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.ForwardingAudience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@CustomLog
@Singleton
public class MineplexExperienceListener implements Listener {

    private final GamePlugin plugin;
    private final ClientManager clientManager;
    private final ServerController serverController;
    private final PlayerController playerController;
    private MineplexExperienceSessionImpl session;
    private final MineplexLevelModule levelModule;

    @Inject
    public MineplexExperienceListener(GamePlugin plugin, ClientManager clientManager, ServerController serverController, PlayerController playerController) {
        this.plugin = plugin;
        this.clientManager = clientManager;
        this.serverController = serverController;
        this.playerController = playerController;
        setupStateHandlers();
        Bukkit.getPluginManager().registerEvents(this, plugin);
        levelModule = MineplexModuleManager.getRegisteredModule(MineplexLevelModule.class);
    }

    private void setupStateHandlers() {
        serverController.getStateMachine().addEnterHandler(GameState.IN_GAME, this::startSession);
        serverController.getStateMachine().addEnterHandler(GameState.ENDING, this::awardWinners);
        serverController.getStateMachine().addExitHandler(GameState.ENDING, this::endSession);
    }

    private void awardWinners(GameState gameState) {
        // Give points to the winners so they can get experience
        for (Audience winner : serverController.getCurrentGame().getWinners()) {
            if (winner instanceof Player player) {
                awardPoints(player, 50);
            } else if (winner instanceof ForwardingAudience forwardingAudience) {
                forwardingAudience.forEachAudience(audience -> {
                    if (audience instanceof Player player) {
                        awardPoints(player, 50);
                    }
                });
            }
        }

        // Give points to participants
        playerController.getParticipants()
                .keySet()
                .forEach(player -> awardPoints(player, 25));
    }

    private void startSession(GameState oldState) {
        session = MineplexExperienceSessionImpl.start();
    }

    private void endSession(GameState oldState) {
        if (session == null) {
            return;
        }

        // End the session
        session.end();

        // Reward the experience

        final CompletableFuture<@NonNull Map<@NonNull UUID, @NonNull ExperienceAwardResult>> awardFuture = levelModule.rewardGame(session);

        awardFuture.thenAcceptAsync(result -> {
            UtilServer.runTaskLater(plugin, () -> {
                result.forEach(this::queueExpMessage);
            }, (long) (1.5 * 20));
        }).exceptionally(ex -> {
            log.error("Error while rewarding experience: ", ex).submit();
            return null;
        });

        session = null;
    }

    private Component getProgressBar(MineplexPlayerExperience experience, boolean levelup) {
        final int currentLevel = experience.getLevel();
        final double progress = experience.getProgressPercentage() / 100d;
        final TextComponent progressBar = ProgressBar.withLength((float) progress, 30)
                .withCharacter(' ')
                .build()
                .decoration(TextDecoration.STRIKETHROUGH, true);

        final MineplexPlayerExperience nextLevel = MineplexPlayerExperience.builder()
                .playerId(experience.getPlayerId())
                .currentExperience(0)
                .experienceToNextLevel(0)
                .totalExperienceNeeded(0)
                .level(0)
                .build();

        Component progressBarFinal = Component.text(currentLevel, experience.getLevelColor())
                .appendSpace()
                .append(progressBar)
                .appendSpace()
                .append(Component.text(currentLevel + 1, nextLevel.getLevelColor()))
                .appendSpace()
                .append(Component.text(String.format("(%,d%%)", (int) (progress * 100)), TextColor.color(222, 222, 222)));
        if (levelup) {
            progressBarFinal = progressBarFinal.appendSpace()
                    .append(Component.text("LEVEL UP!", TextColor.color(26, 255, 0), TextDecoration.BOLD).font(Resources.Font.SMALL_CAPS));
        }
        return progressBarFinal;
    }

    private void awardPoints(Player player, int points) {
        session.addPoints(player, points);
    }

    private void queueExpMessage(@NonNull UUID uuid, @NonNull ExperienceAwardResult awarded) {
        final Player player = Bukkit.getPlayer(uuid);
        if (player == null || !player.isValid() || !player.isOnline()) {
            return;
        }

        UtilMessage.message(player, Component.empty());
        UtilMessage.message(player, Component.empty());
        UtilMessage.simpleMessage(player, "<bold><gradient:#ff8a05:#ffff14:#ff8a05:#ffff14>+" + awarded.getAwardedExperience() + " Mineplex Network Experience</gradient></bold>");
        UtilMessage.message(player, getProgressBar(awarded.getResultExperience(), awarded.hasLevelIncreased()));
        if (awarded.hasLevelIncreased()) {
            new SoundEffect(Sound.ENTITY_PLAYER_LEVELUP, 1.5f, 2f).play(player);
        } else {
            new SoundEffect(Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f).play(player);
        }
        UtilMessage.message(player, Component.empty());
        UtilMessage.message(player, Component.empty());
    }

    @EventHandler
    public void onClientLogin(ClientJoinEvent event) {

        levelModule.getPlayerExperience(event.getClient().getUniqueId()).thenAccept(experience -> {
            event.getClient().saveProperty("MINEPLEX_LEVEL", experience.getLevel());
        }).exceptionally(ex -> {
            log.error("Failed to fetch client experience for {}", event.getClient().getUniqueId());
            return null;
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChatSent(ChatSentEvent event) {
        Client client = clientManager.search().online(event.getPlayer());
        Optional<Integer> levelOptional = client.getProperty("MINEPLEX_LEVEL");
        if (levelOptional.isPresent()) {
            Component levelComponent = Component.text(levelOptional.get(), getLevelColor(levelOptional.get())).appendSpace();
            event.setPrefix(levelComponent.append(event.getPrefix()));
        }

    }

    private @NonNull NamedTextColor getLevelColor(int level) {
        if (level == 0) {
            return NamedTextColor.GRAY;
        }
        return switch (level / 20) {
            case 0 -> NamedTextColor.GRAY; // Level 0–19
            case 1 -> NamedTextColor.BLUE; // Level 20–39
            case 2 -> NamedTextColor.DARK_GREEN; // Level 40–59
            case 3 -> NamedTextColor.GOLD; // Level 60–79
            default -> NamedTextColor.RED; // Level 80+
        };
    }
}
