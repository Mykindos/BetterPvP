package me.mykindos.betterpvp.clans.clans.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.events.ClanKickMemberEvent;
import me.mykindos.betterpvp.clans.clans.events.MemberJoinClanEvent;
import me.mykindos.betterpvp.clans.clans.events.MemberLeaveClanEvent;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.plugin.java.JavaPlugin;

@BPvPListener
@Singleton
public class ClansCommandListener extends ClanListener {

    @Inject
    public ClansCommandListener(ClanManager clanManager, ClientManager clientManager) {
        super(clanManager, clientManager);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMemberLeave(MemberLeaveClanEvent event) {
        UtilServer.runTaskLater(JavaPlugin.getPlugin(Clans.class), () -> {
            event.getPlayer().updateCommands();
        }, 1L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMemberJoin(MemberJoinClanEvent event) {
        UtilServer.runTaskLater(JavaPlugin.getPlugin(Clans.class), () -> {
            event.getPlayer().updateCommands();
        }, 1L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMemberKick(ClanKickMemberEvent event) {
        final Player target = Bukkit.getPlayer(event.getTarget().getUniqueId());
        if (target != null) {
            UtilServer.runTaskLater(JavaPlugin.getPlugin(Clans.class), target::updateCommands, 1L);
        }
    }
}
