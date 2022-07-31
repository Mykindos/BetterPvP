package me.mykindos.betterpvp.clans.clans.listeners;

import com.google.inject.Inject;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.components.ClanAlliance;
import me.mykindos.betterpvp.clans.clans.components.ClanEnemy;
import me.mykindos.betterpvp.clans.clans.components.ClanMember;
import me.mykindos.betterpvp.clans.clans.events.ClanCreateEvent;
import me.mykindos.betterpvp.clans.clans.events.ClanDisbandEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.sql.Timestamp;
import java.util.Optional;

@BPvPListener
public class ClanListener implements Listener {

    private final ClanManager clanManager;

    @Inject
    public ClanListener(ClanManager clanManager) {
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

        UtilMessage.message(event.getPlayer(), "Command", "Successfully created clan " + ChatColor.AQUA + event.getClanName());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClanDisband(ClanDisbandEvent event) {

        if (event.isCancelled()) {
            return;
        }

        Clan clan = event.getClan();

        clan.getMembers().clear();
        clan.getTerritory().clear();

        for (ClanAlliance alliance : clan.getAlliances()) {
            Optional<Clan> otherClanOptional = clanManager.getObject(alliance.getOtherClan());
            otherClanOptional.ifPresent(otherClan -> {
                otherClan.getAlliances().removeIf(ally -> ally.getOtherClan().equalsIgnoreCase(clan.getName()));
            });
        }
        clan.getAlliances().clear();

        for (ClanEnemy enemy : clan.getEnemies()) {
            Optional<Clan> otherClanOptional = clanManager.getObject(enemy.getOtherClan());
            otherClanOptional.ifPresent(otherClan -> {
                otherClan.getEnemies().removeIf(en -> en.getOtherClan().equalsIgnoreCase(clan.getName()));
            });
        }
        clan.getAlliances().clear();

        clanManager.getRepository().delete(clan);
        clanManager.getObjects().remove(clan.getName());

        UtilMessage.broadcast("Clans", clan.getName() + " was disbanded");
    }
}
