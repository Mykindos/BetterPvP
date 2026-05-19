package me.mykindos.betterpvp.clans.clans.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.ClanRelation;
import me.mykindos.betterpvp.core.displayname.DisplayNameEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

@Singleton
@BPvPListener
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class ClansPlayerDisplayNameListener implements Listener {

    private final ClanManager clanManager;

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerDisplayName(final DisplayNameEvent event) {
        Player player = event.getEntity();
        Player targetPlayer = event.getTargetEntity();

        ClanRelation relation = this.clanManager.getRelation(this.clanManager.getClanByPlayer(targetPlayer).orElse(null), this.clanManager.getClanByPlayer(player).orElse(null));

        event.setDisplayName(relation.getPrimaryMiniColorOpening() + player.getName() + relation.getPrimaryMiniColorClosing());
    }
}