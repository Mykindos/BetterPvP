package me.mykindos.betterpvp.game.framework.model.attribute.global;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.game.framework.AbstractGame;
import me.mykindos.betterpvp.game.framework.ServerController;
import me.mykindos.betterpvp.game.framework.manager.MapManager;
import me.mykindos.betterpvp.game.framework.model.attribute.GameAttribute;
import me.mykindos.betterpvp.game.framework.model.world.MappedWorld;
import me.mykindos.betterpvp.game.framework.state.GameState;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Set;

/**
 * Attribute for configuring the game map.
 */
@Singleton
@CustomLog
public class CurrentMapAttribute extends GameAttribute<MappedWorld> {

    private final MapManager mapManager;
    private final ServerController serverController;

    @Inject
    public CurrentMapAttribute(MapManager mapManager, ServerController serverController) {
        super("game.map", null);
        this.mapManager = mapManager;
        this.serverController = serverController;
    }

    @Override
    public boolean canSet(CommandSender sender) {
        // Can only set when a game is set and we're in WAITING state
        return serverController.getCurrentGame() != null && (serverController.getCurrentState() == GameState.WAITING || serverController.getCurrentState() == GameState.STARTING);
    }

    @Override
    public boolean isValidValue(MappedWorld value) {
        // Must be a valid map for the current game
        AbstractGame<?, ?> game = serverController.getCurrentGame();
        if (game == null) {
            return false;
        }

        return value.getMetadata().getGameMode().equalsIgnoreCase(game.getConfiguration().getName());
    }

    @Override
    @Nullable
    public MappedWorld parseValue(String value) {
        // Try to find a map with the given name
        AbstractGame<?, ?> game = serverController.getCurrentGame();
        if (game == null) {
            return null;
        }

        // Look through available maps
        Set<MappedWorld> availableMaps = mapManager.getAvailableMaps();
        for (MappedWorld map : availableMaps) {
            String mapName = map.getMetadata().getName();
            if (mapName.equalsIgnoreCase(value)) {
                // Check if it's valid for the current game
                if (map.getMetadata().getGameMode().equalsIgnoreCase(game.getConfiguration().getName())) {
                    return map;
                }
            }
        }

        return null;
    }

    @Override
    @NotNull
    public String formatValue(@NotNull MappedWorld value) {
        return value.getMetadata().getName();
    }

    @Override
    @NotNull
    public String getCannotSetMessage() {
        return "Map can only be set when a game is set and in the WAITING state.";
    }

    @Override
    @NotNull
    public String getInvalidValueMessage(String value) {
        return "Map '" + value + "' not found or not valid for the current game.";
    }

    @Override
    public @NotNull Collection<MappedWorld> getPossibleValues() {
        final AbstractGame<?, ?> game = serverController.getCurrentGame();
        if (game == null) {
            return Set.of();
        }

        return mapManager.getAvailableMaps().stream()
                .filter(map -> map.getMetadata().getGameMode().equalsIgnoreCase(game.getConfiguration().getName()))
                .sorted((o1, o2) -> o1.getMetadata().getName().compareToIgnoreCase(o2.getMetadata().getName()))
                .toList();
    }
}
