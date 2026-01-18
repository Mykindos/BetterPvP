package me.mykindos.betterpvp.game.impl.ctf.controller;

import com.google.inject.Inject;
import dev.brauw.mapper.region.PerspectiveRegion;
import lombok.CustomLog;
import lombok.Getter;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.framework.hat.PacketHatController;
import me.mykindos.betterpvp.game.framework.ServerController;
import me.mykindos.betterpvp.game.framework.manager.MapManager;
import me.mykindos.betterpvp.game.framework.model.Lifecycled;
import me.mykindos.betterpvp.game.framework.model.stats.StatManager;
import me.mykindos.betterpvp.game.framework.model.team.Team;
import me.mykindos.betterpvp.game.framework.state.GameState;
import me.mykindos.betterpvp.game.guice.GameScoped;
import me.mykindos.betterpvp.game.impl.ctf.CaptureTheFlag;
import me.mykindos.betterpvp.game.impl.ctf.model.Flag;
import org.bukkit.Location;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@GameScoped
@CustomLog
public class GameController implements Lifecycled {
    @Getter private final Map<Team, Flag> flags = new HashMap<>();
    @Getter private final Map<Team, Integer> captures = new HashMap<>();

    private final ServerController serverController;
    private final CaptureTheFlag game;
    private final MapManager mapManager;
    private final FlagInventoryCache inventoryCache;
    private final ClientManager clientManager;
    private final PacketHatController packetHatController;
    private final EffectManager effectManager;
    private final StatManager statManager;
    @Getter
    private boolean suddenDeath;
    
    @Inject
    public GameController(ServerController serverController, CaptureTheFlag game, MapManager mapManager,
                          FlagInventoryCache inventoryCache, ClientManager clientManager, PacketHatController packetHatController,
                          EffectManager effectManager, StatManager statManager) {
        this.serverController = serverController;
        this.game = game;
        this.mapManager = mapManager;
        this.inventoryCache = inventoryCache;
        this.clientManager = clientManager;
        this.packetHatController = packetHatController;
        this.effectManager = effectManager;
        this.statManager = statManager;
    }

    public boolean triggerSuddenDeath() {
        // get teams with most captures
        final int maxCaptures = captures.values().stream().max(Integer::compareTo).orElse(0);
        final List<Team> teamsWithMaxCaptures = captures.entrySet().stream()
                .filter(entry -> entry.getValue() == maxCaptures)
                .map(Map.Entry::getKey)
                .toList();

        // if there is a tie, sudden death
        if (teamsWithMaxCaptures.size() > 1) {
            suddenDeath = true;
            //disable respawn and balancing
            game.getConfiguration().getRespawnsAttribute().setValue(false);
            game.getConfiguration().getAllowLateJoinsAttribute().setValue(false);
            game.getConfiguration().getForceBalanceAttribute().setValue(false);
            game.getConfiguration().getAutoBalanceOnDeathAttribute().setValue(false);
            log.info("Sudden death triggered! Teams with max captures: {}", teamsWithMaxCaptures).submit();
            return true;
        }

        // if there is a winner, end the game
        else {
            game.setWinners(teamsWithMaxCaptures);
            serverController.transitionTo(GameState.ENDING);
            return false;
        }
    }

    public Flag getFlag(Team team) {
        return flags.get(team);
    }

    public void scoreCapture(Team capturer, Flag flag) {
        int currentCaptures = captures.getOrDefault(capturer, 0) + 1;
        captures.put(capturer, currentCaptures);
        
        log.info("Team {} captured {} flag! Current score: {}", 
                capturer.getProperties().name(), 
                flag.getTeam().getProperties().name(),
                currentCaptures).submit();
                
        // Check for sudden death
        if (suddenDeath) {
            game.setWinners(List.of(capturer));
            serverController.transitionTo(GameState.ENDING);
        }

        // Score to win reached
        else if (currentCaptures >= game.getConfiguration().getScoreToWinAttribute().getValue()) {
            game.setWinners(List.of(capturer));
            serverController.transitionTo(GameState.ENDING);
        }

        // Set the current winner to the team with max scores, so if game is forced to end, the winner is known
        else {
            game.setWinners(captures.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .map(List::of)
                    .orElseThrow());
        }
    }

    @Override
    public void setup() {
        // Create flags for each team based on regions in the map
        for (Team team : game.getTeams().values()) {
            // Initialize flag
            final Optional<PerspectiveRegion> flagLocOpt = mapManager.getCurrentMap().findRegion("flag_" + team.getProperties().name().toLowerCase(), PerspectiveRegion.class).findAny();
            if (flagLocOpt.isEmpty()) {
                log.error("Flag location not found for team {}", team.getProperties().name()).submit();
                return;
            }

            final Location location = flagLocOpt.get().getLocation();
            final Flag flag = new Flag(2f, team, location, inventoryCache, clientManager, packetHatController, effectManager, game, statManager);
            flags.put(team, flag);
            flag.setup();
            flag.spawn();

            // Initialize captures counter
            captures.put(team, 0);
        }
    }

    @Override
    public void tearDown() {
        for (Flag flag : flags.values()) {
            flag.tearDown();
        }
    }
}