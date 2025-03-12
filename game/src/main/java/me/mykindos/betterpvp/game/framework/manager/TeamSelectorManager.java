package me.mykindos.betterpvp.game.framework.manager;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.brauw.mapper.region.PerspectiveRegion;
import lombok.CustomLog;
import lombok.Getter;
import me.mykindos.betterpvp.game.framework.ServerController;
import me.mykindos.betterpvp.game.framework.TeamGame;
import me.mykindos.betterpvp.game.framework.model.team.TeamProperties;
import me.mykindos.betterpvp.game.framework.model.team.TeamSelector;
import me.mykindos.betterpvp.game.framework.model.world.MappedWorld;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.*;

/**
 * Manages team selectors in the waiting lobby
 */
@CustomLog
@Singleton
public class TeamSelectorManager {

    private final ServerController serverController;
    @Getter
    private final Set<TeamSelector> teamSelectors = new HashSet<>();

    @Inject
    public TeamSelectorManager(ServerController serverController) {
        this.serverController = serverController;
    }

    /**
     * Creates and spawns team selectors in the waiting lobby
     * @param waitingLobby the waiting lobby
     * @return a list of spawned team selectors
     */
    public List<TeamSelector> createTeamSelectors(MappedWorld waitingLobby) {
        // Clear previous selectors
        clearSelectors();

        // Check if current game is a team game
        if (!(serverController.getCurrentGame() instanceof TeamGame teamGame)) {
            throw new IllegalStateException("Current game is not a team game");
        }

        // Get teams from game configuration
        Set<TeamProperties> teams = teamGame.getConfiguration().getTeamProperties();
        List<TeamSelector> selectors = new ArrayList<>();

        // Spawn selector for each team
        for (TeamProperties team : teams) {
            // Try different naming patterns for finding the region
            Optional<PerspectiveRegion> regionOpt = findTeamSelectorRegion(waitingLobby, team.name());

            if (regionOpt.isEmpty()) {
                log.warn("No region found for team selector {}", team.name()).submit();
                continue;
            }

            Location location = regionOpt.get().getLocation();
            TeamSelector selector = new TeamSelector(team);
            selector.spawn(location);
            selectors.add(selector);
            log.info("Spawned selector for team {} at {}", team.name(), location).submit();
        }

        return selectors;
    }

    private Optional<PerspectiveRegion> findTeamSelectorRegion(MappedWorld world, String teamName) {
        String regionName = "team_selector_" + teamName.toLowerCase();
        return world.findRegion(regionName, PerspectiveRegion.class).findFirst();
    }

    /**
     * Clears all spawned selectors
     */
    public void clearSelectors() {
        for (TeamSelector selector : teamSelectors) {
            final Entity entity = selector.getEntity();
            if (entity.isValid()) {
                entity.remove();
            }
        }

        teamSelectors.clear();
    }
}