package me.mykindos.betterpvp.clans.clans.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.parties.PartyMember;
import me.mykindos.betterpvp.core.parties.PartyMemberFilter;
import me.mykindos.betterpvp.core.parties.events.PartyCreateEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

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
        clanManager.getClanByPlayer(event.getParty().getPartyLeader()).ifPresent(clan -> {
            if (event.getFilter() == PartyMemberFilter.CLAN) {
                clan.getMembersAsPlayers().forEach(member -> event.getParty().getMembers().add(new PartyMember(member.getUniqueId())));
            } else if (event.getFilter() == PartyMemberFilter.CLAN_ALLIES) {
                clan.getMembersAsPlayers().forEach(member -> event.getParty().getMembers().add(new PartyMember(member.getUniqueId())));
                clan.getAlliances().forEach(ally -> {
                    ally.getClan().getMembersAsPlayers().forEach(member -> event.getParty().getMembers().add(new PartyMember(member.getUniqueId())));
                });

            }
        });

    }
}
