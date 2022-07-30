package me.mykindos.betterpvp.clans.clans.listeners;

import com.google.inject.Inject;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.components.ClanMember;
import me.mykindos.betterpvp.clans.clans.events.ClanCreateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.sql.Timestamp;

@BPvPListener
public class ClanCreationListener implements Listener {

    private final ClanManager clanManager;

    @Inject
    public ClanCreationListener(ClanManager clanManager) {
        this.clanManager = clanManager;
    }


    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClanCreate(ClanCreateEvent event) {
        var timestamp = new Timestamp(System.currentTimeMillis());
        Clan clan = Clan.builder().name(event.getClanName()).level(1)
                .timeCreated(timestamp).lastLogin(timestamp)
                .build();

        clan.getMembers().add(new ClanMember(event.getPlayer().getUniqueId().toString(), ClanMember.MemberRank.OWNER));

        clanManager.addObject(event.getClanName(), clan);
        clanManager.getRepository().save(clan);

        System.out.println("CLAN ID: " + clan.getId());
        UtilMessage.message(event.getPlayer(), "Command", "Successfully created clan " + ChatColor.AQUA + event.getClanName());
    }
}
