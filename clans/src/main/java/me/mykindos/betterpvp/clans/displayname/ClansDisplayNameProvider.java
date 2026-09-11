package me.mykindos.betterpvp.clans.displayname;

import com.google.inject.Inject;
import lombok.AllArgsConstructor;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.ClanRelation;
import me.mykindos.betterpvp.core.displayname.CoreDisplayNameProvider;
import me.mykindos.betterpvp.core.displayname.DisplayNameProvider;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

/**
 * Clan implementation of {@link DisplayNameProvider}.
 * <p>
 * This implementation is registered by Clans to replace Core's default
 * display name provider. Player display names are formatted according to
 * the relationship between the viewer's clan and the target player's clan.
 */
@AllArgsConstructor(onConstructor = @__(@Inject))
public class ClansDisplayNameProvider implements DisplayNameProvider {

    private final CoreDisplayNameProvider coreDisplayNameProvider;
    private final ClanManager clanManager;

    /**
     * Gets the formatted display name for an entity from the perspective of the
     * supplied viewer.
     *
     * @param entity the entity whose display name is being requested
     * @param viewer the entity viewing the display name
     * @return the entity's display name formatted according to the clan
     * relationship between the viewer and the player
     */
    @Override
    public Component getDisplayNameAsComponent(final Entity entity, final Entity viewer) {
        if (!(entity instanceof Player player) || !(viewer instanceof Player viewingPlayer)) {
            return this.coreDisplayNameProvider.getDisplayNameAsComponent(entity, viewer);
        }

        final ClanRelation clanRelation = this.clanManager.getRelation(
                this.clanManager.getClanByPlayer(viewingPlayer).orElse(null),
                this.clanManager.getClanByPlayer(player).orElse(null)
        );

        return this.clanManager.getPlayerName(clanRelation, player);
    }

}