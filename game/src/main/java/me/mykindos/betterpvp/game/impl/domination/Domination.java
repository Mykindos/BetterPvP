package me.mykindos.betterpvp.game.impl.domination;

import java.time.Duration;
import java.util.List;
import me.mykindos.betterpvp.game.GamePlugin;
import me.mykindos.betterpvp.game.framework.TeamGame;
import me.mykindos.betterpvp.game.framework.model.player.Participant;
import me.mykindos.betterpvp.game.framework.model.team.Team;
import me.mykindos.betterpvp.game.framework.model.team.TeamColorProvider;
import me.mykindos.betterpvp.game.framework.model.team.TeamProperties;
import me.mykindos.betterpvp.game.framework.model.team.TeamSpawnPointProvider;
import me.mykindos.betterpvp.game.framework.module.powerup.PowerupManager;
import me.mykindos.betterpvp.game.impl.domination.controller.GameController;
import me.mykindos.betterpvp.game.impl.domination.model.DominationConfiguration;
import me.mykindos.betterpvp.game.impl.domination.model.Gem;
import net.kyori.adventure.text.Component;

public class Domination extends TeamGame<DominationConfiguration> {

    public Domination() {
        super(DominationConfiguration.builder()
                .spawnPointProvider(new TeamSpawnPointProvider())
                .playerColorProvider(new TeamColorProvider())
                .teamProperty(TeamProperties.defaultBlue(6))
                .teamProperty(TeamProperties.defaultRed(6))
                .allowOversizedTeams(false)
                .allowLateJoins(true)
                .name("Domination")
                .abbreviation("DOM")
                .requiredPlayers(8)
                .maxPlayers(12)
                .scoreToWin(10_000)
                .capturePointScore(8)
                .killScore(50)
                .gemScore(300)
                .secondsToCapture(10f)
                .respawnTimer(10.0)
                .duration(Duration.ofMinutes(10L))
                .maxImbalance(1)
                .durationBeforeAutoBalance(Duration.ofSeconds(30))
                .autoBalanceOnDeath(true)
                .forceBalance(true)
                .build());
    }

    @Override
    public void setup() {
        final GamePlugin plugin = injector.getInstance(GamePlugin.class);
        final GameController gameController = injector.getInstance(GameController.class);
        gameController.setup();
        final PowerupManager powerupManager = injector.getInstance(PowerupManager.class);
        powerupManager.registerPowerupType("gem", region -> new Gem(region.getLocation(), plugin, gameController, this));
        powerupManager.setup();
    }

    @Override
    public void tearDown() {
        final PowerupManager powerupManager = injector.getInstance(PowerupManager.class);
        powerupManager.tearDown();
        final GameController gameController = injector.getInstance(GameController.class);
        gameController.tearDown();
    }

    @Override
    public void forceEnd() {
        super.forceEnd();

        // Get the highest scoring team
        final GameController gameController = injector.getInstance(GameController.class);
        final List<Team> winners = getParticipants().stream()
                .filter(team -> team.getParticipants().stream().anyMatch(Participant::isAlive))
                .sorted((team1, team2) -> {
                    int score1 = gameController.getScores().getOrDefault(team1, 0);
                    int score2 = gameController.getScores().getOrDefault(team2, 0);
                    return Integer.compare(score2, score1);
                })
                .toList();

        if (!winners.isEmpty()) {
            setWinners(List.of(winners.getFirst()));
        } else {
            setWinners(List.of());
        }
    }

    @Override
    public Component getDescription() {
        DominationConfiguration configuration = getConfiguration();
        int gemScore = configuration.getGemScoreAttribute().getValue();
        int killScore = configuration.getKillScoreAttribute().getValue();
        int winScore = configuration.getScoreToWinAttribute().getValue();
        return Component.text("Capture Beacons for Points").appendNewline()
                .append(Component.text("+" + gemScore + " Points for Gem Powerups")).appendNewline()
                .append(Component.text("+" + killScore + " Points for Kills")).appendNewline()
                .append(Component.text("First team to " + winScore + " Points wins"));
    }
}
