package me.mykindos.betterpvp.clans.clans.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.components.clans.events.ClanAddExperienceEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

import java.util.Optional;

@BPvPListener
@Singleton
public class ClanExperienceListener extends ClanListener {

    @Inject
    public ClanExperienceListener(ClanManager clanManager, ClientManager clientManager) {
        super(clanManager, clientManager);
    }

    @EventHandler (priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onExperienceAdded(ClanAddExperienceEvent event){
        Player player = event.getPlayer();

        Optional<Clan> clanOptional = clanManager.getClanByPlayer(player);
        if(clanOptional.isEmpty()) {
            return;
        }

        Clan clan = clanOptional.get();

        long currentLevel = clan.getLevel();
        clan.grantExperience(event.getExperience());

        clan.messageClan("Your clan earned <green>" + event.getExperience() + "<reset> experience.", null, true);

        if(clan.getLevel() > currentLevel){
            clan.messageClan("Your clan has levelled up to level <green>" + clan.getLevel() + "<reset>!", null, true);
        }
    }
}
