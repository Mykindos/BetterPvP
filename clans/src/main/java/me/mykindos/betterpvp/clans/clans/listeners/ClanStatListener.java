package me.mykindos.betterpvp.clans.clans.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.client.stats.events.WrapStatEvent;
import me.mykindos.betterpvp.core.client.stats.impl.clans.ClanWrapperStat;
import me.mykindos.betterpvp.core.client.stats.impl.clans.ClansStat;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

@Singleton
@BPvPListener
public class ClanStatListener extends ClanListener {
    @Inject
    public ClanStatListener(ClanManager clanManager, ClientManager clientManager) {
        super(clanManager, clientManager);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onStatUpdate(WrapStatEvent event) {
        //if already a Clans Stat, either it has already been handled or a native type
        if (event.getStat() instanceof ClansStat) return;
        if (!event.getStat().wrappingAllowed()) return;
        final ClanWrapperStat.ClanWrapperStatBuilder<?, ?> builder = ClanWrapperStat.builder()
                .wrappedStat(event.getStat());
        final Player player = Bukkit.getPlayer(event.getId());
        final ClanWrapperStat wrappedStat = (ClanWrapperStat) clanManager.addClanInfo(player, builder).build();
        event.setStat(wrappedStat);
    }


}
