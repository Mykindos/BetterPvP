package me.mykindos.betterpvp.game.impl.domination.controller;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import dev.brauw.mapper.region.CuboidRegion;
import lombok.CustomLog;
import lombok.Getter;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.game.framework.ServerController;
import me.mykindos.betterpvp.game.framework.manager.MapManager;
import me.mykindos.betterpvp.game.framework.model.Lifecycled;
import me.mykindos.betterpvp.game.framework.model.team.Team;
import me.mykindos.betterpvp.game.framework.model.world.MappedWorld;
import me.mykindos.betterpvp.game.framework.state.GameState;
import me.mykindos.betterpvp.game.guice.GameScoped;
import me.mykindos.betterpvp.game.impl.domination.Domination;
import me.mykindos.betterpvp.game.impl.domination.model.CapturePoint;
import me.mykindos.betterpvp.game.impl.domination.model.DominationConfiguration;

import java.util.*;

@GameScoped
@CustomLog
public class GameController implements Lifecycled {
    
    @Getter private final Map<Team, Integer> scores = new HashMap<>();
    @Getter private final List<CapturePoint> capturePoints = new ArrayList<>();
    
    private final ServerController serverController;
    private final Domination game;
    private final MapManager mapManager;
    private final DominationConfiguration configuration;
    private final ClientManager clientManager;

    @Inject
    public GameController(ServerController serverController, Domination game, MapManager mapManager,
                          DominationConfiguration configuration, ClientManager clientManager) {
        this.serverController = serverController;
        this.game = game;
        this.mapManager = mapManager;
        this.configuration = configuration;
        this.clientManager = clientManager;
    }
    @Override
    public void setup() {
        // Find all capture point regions
        List<CuboidRegion> points = mapManager.getCurrentMap().regexRegion("capture_point_.+", CuboidRegion.class).toList();

        for (CuboidRegion region : points) {
            final String name = region.getName().substring(region.getName().lastIndexOf("_") + 1);
            CapturePoint point = new CapturePoint(name, region, this, configuration, clientManager, game);
            capturePoints.add(point);
            point.setup();
            log.info("Created capture point {} at {}", name, region.getMin()).submit();
        }

        // Teams
        for (Team team : game.getTeams().values()) {
            scores.put(team, 0);
        }
    }

    @Override
    public void tearDown() {
        capturePoints.forEach(CapturePoint::tearDown);
    }

    public void tick() {
        capturePoints.forEach(CapturePoint::tick);
        
        // Check for game end
        for (Map.Entry<Team, Integer> entry : scores.entrySet()) {
            if (entry.getValue() >= configuration.getScoreToWinAttribute().getValue()) {
                game.setWinners(List.of(entry.getKey()));
                serverController.transitionTo(GameState.ENDING);
                break;
            }
        }
    }
    
    public void addPoints(Team team, int points) {
        if (serverController.getCurrentState() == GameState.ENDING) {
            return;
        }

        final int newScore = scores.compute(team, (key, value) -> {
            return Math.min(configuration.getScoreToWinAttribute().getValue(), points + Objects.requireNonNullElse(value, 0));
        });

        // Update current winner
        scores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .ifPresent(currentWinner -> {
                    game.setWinners(List.of(currentWinner));
                });

        // End game if somebody reached max points
        if (newScore >= configuration.getScoreToWinAttribute().getValue()) {
            serverController.transitionTo(GameState.ENDING);
        }
    }
}