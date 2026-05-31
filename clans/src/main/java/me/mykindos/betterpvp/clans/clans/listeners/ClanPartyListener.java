package me.mykindos.betterpvp.clans.clans.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.parties.PartyMember;
import me.mykindos.betterpvp.core.parties.PartyMemberFilter;
import me.mykindos.betterpvp.core.parties.events.PartyCreateEvent;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.world.zone.ZoneManager;
import me.mykindos.betterpvp.core.world.zone.Zones;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.List;

@BPvPListener
@Singleton
public class ClanPartyListener implements Listener {

    private final ClanManager clanManager;
    private final ZoneManager zoneManager;

    @Inject
    public ClanPartyListener(ClanManager clanManager, ZoneManager zoneManager) {
        this.clanManager = clanManager;
        this.zoneManager = zoneManager;
    }


    @EventHandler
    public void onPartyCreate(PartyCreateEvent event) {

        Player player = Bukkit.getPlayer(event.getParty().getPartyLeader());
        if (player == null) return;

        if (!zoneManager.hasTagAt(player.getLocation(), Zones.SAFE)) {
            UtilMessage.simpleMessage(player, "You must be at shops to form a party.");
            return;
        }

        clanManager.getClanByPlayer(event.getParty().getPartyLeader()).ifPresent(clan -> {
            if (event.getFilter() == PartyMemberFilter.CLAN) {
                addMembersInSafeZone(event, clan.getMembersAsPlayers());
            } else if (event.getFilter() == PartyMemberFilter.CLAN_ALLIES) {
                addMembersInSafeZone(event, clan.getMembersAsPlayers());
                clan.getAlliances().forEach(ally -> addMembersInSafeZone(event, ally.getClan().getMembersAsPlayers()));
            }
        });

    }

    private void addMembersInSafeZone(PartyCreateEvent event, List<Player> members) {
        members.forEach(member -> {
            if (zoneManager.hasTagAt(member.getLocation(), Zones.SAFE)) {
                event.getParty().getMembers().add(new PartyMember(member.getUniqueId()));
            }
        });
    }
}
