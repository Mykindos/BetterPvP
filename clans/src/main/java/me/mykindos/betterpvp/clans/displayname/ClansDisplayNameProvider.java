package me.mykindos.betterpvp.clans.displayname;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.ClanRelation;
import me.mykindos.betterpvp.core.displayname.DefaultDisplayNameProvider;
import me.mykindos.betterpvp.core.displayname.DisplayNameProvider;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

@RequiredArgsConstructor(onConstructor = @__(@Inject))
@Singleton
public class ClansDisplayNameProvider implements DisplayNameProvider {

    private final ClanManager clanManager;
    private final DefaultDisplayNameProvider defaultDisplayNameProvider;

    @Override
    public String getDisplayName(Entity entity, Entity viewer) {
        if (!(entity instanceof Player entityPlayer) || !(viewer instanceof Player viewerPlayer)) {
            return defaultDisplayNameProvider.getDisplayName(entity, viewer);
        }

        ClanRelation relation = clanManager.getRelation(clanManager.getClanByPlayer(viewerPlayer).orElse(null), clanManager.getClanByPlayer(entityPlayer).orElse(null));

        return relation.getPrimaryMiniColorOpening() + entityPlayer.getName() + relation.getPrimaryMiniColorClosing();
    }
}