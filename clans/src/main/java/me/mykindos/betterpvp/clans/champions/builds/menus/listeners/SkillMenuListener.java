package me.mykindos.betterpvp.clans.champions.builds.menus.listeners;

import com.google.inject.Inject;
import me.mykindos.betterpvp.clans.champions.skills.events.SkillUpdateEvent;
import me.mykindos.betterpvp.clans.gamer.Gamer;
import me.mykindos.betterpvp.clans.gamer.GamerManager;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Optional;

@BPvPListener
public class SkillMenuListener implements Listener {

    private final GamerManager gamerManager;

    @Inject
    public SkillMenuListener(GamerManager gamerManager) {
        this.gamerManager = gamerManager;
    }

    @EventHandler
    public void onSkillUpdate(SkillUpdateEvent event) {
        Optional<Gamer> gamerOptional = gamerManager.getObject(event.getPlayer().getUniqueId());
        if(gamerOptional.isPresent()) {
            gamerManager.getBuildRepository().update(event.getRoleBuild());
        }
    }


}
