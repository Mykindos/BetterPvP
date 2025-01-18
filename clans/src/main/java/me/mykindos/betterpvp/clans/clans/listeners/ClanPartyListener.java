package me.mykindos.betterpvp.clans.clans.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.parties.PartyMember;
import me.mykindos.betterpvp.core.parties.PartyMemberFilter;
import me.mykindos.betterpvp.core.parties.events.PartyCreateEvent;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Optional;

@BPvPListener
@Singleton
public class ClanPartyListener implements Listener {

    private final ClanManager clanManager;

    @Inject
    public ClanPartyListener(ClanManager clanManager) {
        this.clanManager = clanManager;
    }


    @EventHandler
    public void onPartyCreate(PartyCreateEvent event) {

        Player player = Bukkit.getPlayer(event.getParty().getPartyLeader());
        if (player == null) return;

        Optional<Clan> locationClanOptional = clanManager.getClanByLocation(player.getLocation());
        if (locationClanOptional.isEmpty()) {
            UtilMessage.simpleMessage(player, "Error 1 - You must be at shops to form a party.");
            return;
        }

        Clan locationClan = locationClanOptional.get();
        if (!locationClan.isSafe()) {
            UtilMessage.simpleMessage(player, "Error 2 - You must be at shops to form a party.");
            return;
        }

        clanManager.getClanByPlayer(event.getParty().getPartyLeader()).ifPresent(clan -> {
            if (event.getFilter() == PartyMemberFilter.CLAN) {
                clan.getMembersAsPlayers().forEach(member -> {
                    clanManager.getClanByLocation(member.getLocation()).ifPresent(memberLocClan -> {
                        if (memberLocClan.equals(locationClan)) {
                            event.getParty().getMembers().add(new PartyMember(member.getUniqueId()));
                        }
                    });
                });
            } else if (event.getFilter() == PartyMemberFilter.CLAN_ALLIES) {
                clan.getMembersAsPlayers().forEach(member -> {
                    clanManager.getClanByLocation(member.getLocation()).ifPresent(memberLocClan -> {
                        if (memberLocClan.equals(locationClan)) {
                            event.getParty().getMembers().add(new PartyMember(member.getUniqueId()));
                        }
                    });
                });
                clan.getAlliances().forEach(ally -> {
                    ally.getClan().getMembersAsPlayers().forEach(member -> {
                        clanManager.getClanByLocation(member.getLocation()).ifPresent(memberLocClan -> {
                            if (memberLocClan.equals(locationClan)) {
                                event.getParty().getMembers().add(new PartyMember(member.getUniqueId()));
                            }
                        });
                    });
                });

            }
        });

    }
}
