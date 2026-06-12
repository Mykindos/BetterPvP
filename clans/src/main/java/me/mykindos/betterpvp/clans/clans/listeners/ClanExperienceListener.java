package me.mykindos.betterpvp.clans.clans.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.components.clans.events.ClanAddExperienceEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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

        if(event.getExperience() >= 1) { // Don't show the message for low amounts of XP
            clan.messageClan("clans.experience.earned", new Object[]{
                    Component.text(event.getExperience(), NamedTextColor.GREEN)
            }, null, true);
        }

        if(clan.getLevel() > currentLevel){
            clan.messageClan("clans.experience.level-up", new Object[]{
                    Component.text(clan.getLevel(), NamedTextColor.GREEN)
            }, null, true);
        }
    }
}
