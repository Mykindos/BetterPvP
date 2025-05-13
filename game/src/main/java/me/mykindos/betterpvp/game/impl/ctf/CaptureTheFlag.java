package me.mykindos.betterpvp.game.impl.ctf;

import java.time.Duration;
import java.util.List;
import me.mykindos.betterpvp.game.framework.TeamGame;
import me.mykindos.betterpvp.game.framework.model.player.Participant;
import me.mykindos.betterpvp.game.framework.model.team.Team;
import me.mykindos.betterpvp.game.framework.model.team.TeamColorProvider;
import me.mykindos.betterpvp.game.framework.model.team.TeamProperties;
import me.mykindos.betterpvp.game.framework.model.team.TeamSpawnPointProvider;
import me.mykindos.betterpvp.game.framework.module.powerup.PowerupManager;
import me.mykindos.betterpvp.game.impl.ctf.controller.GameController;
import me.mykindos.betterpvp.game.impl.ctf.controller.SuddenDeathTimer;
import me.mykindos.betterpvp.game.impl.ctf.model.CTFConfiguration;
import net.kyori.adventure.text.Component;

public class CaptureTheFlag extends TeamGame<CTFConfiguration> {

    public CaptureTheFlag() {
        super(CTFConfiguration.builder()
                .spawnPointProvider(new TeamSpawnPointProvider())
                .playerColorProvider(new TeamColorProvider())
                .teamProperty(TeamProperties.defaultBlue(6))
                .teamProperty(TeamProperties.defaultRed(6))
                .allowOversizedTeams(false)
                .allowLateJoins(true)
                .name("Capture The Flag")
                .abbreviation("CTF")
                .requiredPlayers(8)
                .maxPlayers(12)
                .respawns(true)
                .respawnTimer(10.0)
                .duration(Duration.ofMinutes(13L))
                .suddenDeathDuration(Duration.ofMinutes(3))
                .scoreToWin(5)
                .maxImbalance(1)
                .durationBeforeAutoBalance(Duration.ofSeconds(30))
                .autoBalanceOnDeath(true)
                .forceBalance(true)
                .build());
    }

    @Override
    public void setup() {
        final GameController gameController = injector.getInstance(GameController.class);
        gameController.setup();
        final SuddenDeathTimer suddenDeathTimer = injector.getInstance(SuddenDeathTimer.class);
        suddenDeathTimer.run();
        final PowerupManager powerupManager = injector.getInstance(PowerupManager.class);
        powerupManager.setup();
    }

    @Override
    public void tearDown() {
        final GameController gameController = injector.getInstance(GameController.class);
        gameController.tearDown();
        final SuddenDeathTimer suddenDeathTimer = injector.getInstance(SuddenDeathTimer.class);
        suddenDeathTimer.cancel();
        final PowerupManager powerupManager = injector.getInstance(PowerupManager.class);
        powerupManager.tearDown();
    }

    @Override
    public boolean attemptGracefulEnding() {
        if (super.attemptGracefulEnding()) {
            return true;
        }

        final GameController gameController = injector.getInstance(GameController.class);
        if (gameController.isSuddenDeath()) {
            final List<Team> winners = getParticipants().stream()
                    .filter(team -> team.getParticipants().stream().anyMatch(Participant::isAlive))
                    .toList();

            if (winners.size() == 1) {
                setWinners(winners);
                return true;
            }
        }

        return false;
    }

    @Override
    public void forceEnd() {
        super.forceEnd();

        // Get the highest scoring team
        final GameController gameController = injector.getInstance(GameController.class);
        final List<Team> winners = getParticipants().stream()
                .filter(team -> team.getParticipants().stream().anyMatch(Participant::isAlive))
                .sorted((team1, team2) -> {
                    int score1 = gameController.getCaptures().getOrDefault(team1, 0);
                    int score2 = gameController.getCaptures().getOrDefault(team2, 0);
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
        CTFConfiguration configuration = getConfiguration();
        int captures = configuration.getScoreToWinAttribute().getValue();
        String time = configuration.getGameDurationAttribute().formatValue(configuration.getGameDurationAttribute().getValue());
        return Component.text("Capture The Opponent's Flag").appendNewline()
                .append(Component.text("First team to " + captures + " Captures")).appendNewline()
                .append(Component.text("Or with the most Captures after " + time+ " wins"));
    }
}
