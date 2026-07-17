package me.mykindos.betterpvp.game.displayname;

import com.google.inject.Inject;
import lombok.AllArgsConstructor;
import me.mykindos.betterpvp.core.displayname.CoreDisplayNameProvider;
import me.mykindos.betterpvp.core.displayname.DisplayNameProvider;
import me.mykindos.betterpvp.game.framework.ServerController;
import me.mykindos.betterpvp.game.framework.TeamGame;
import me.mykindos.betterpvp.game.framework.model.team.Team;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

/**
 * Game implementation of {@link DisplayNameProvider}.
 * <p>
 * This implementation is registered by Game to replace Core's default
 * display name provider. Player display names are formatted according to
 * the current game.
 */
@AllArgsConstructor(onConstructor = @__(@Inject))
public class GameDisplayNameProvider implements DisplayNameProvider {

    private final CoreDisplayNameProvider coreDisplayNameProvider;
    private final ServerController serverController;

    /**
     * Gets the formatted display name for an entity from the perspective of the
     * supplied viewer.
     *
     * @param entity the entity whose display name is being requested
     * @param viewer the entity viewing the display name
     * @return the entity's display name formatted according to the current game
     */
    @Override
    public Component getDisplayNameAsComponent(final Entity entity, final Entity viewer) {
        if (!(entity instanceof Player player)) {
            return this.coreDisplayNameProvider.getDisplayNameAsComponent(entity, viewer);
        }

        if (!(this.serverController.getCurrentGame() instanceof TeamGame<?> teamGame)) {
            return this.coreDisplayNameProvider.getDisplayNameAsComponent(player, viewer);
        }

        final Team playerTeam = teamGame.getPlayerTeam(player);
        if (playerTeam == null) {
            return this.coreDisplayNameProvider.getDisplayNameAsComponent(player, viewer);
        }

        return Component.text(player.getName(), playerTeam.getProperties().color());
    }

}