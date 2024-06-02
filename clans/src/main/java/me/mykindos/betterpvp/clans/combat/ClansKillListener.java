package me.mykindos.betterpvp.clans.combat;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.ClanRelation;
import me.mykindos.betterpvp.core.combat.events.KillContributionEvent;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.Optional;

@BPvPListener
@Singleton
public class ClansKillListener implements Listener {

    private final ClanManager clanManager;

    @Inject
    @Config(path = "clans.pillage.protection", defaultValue = "true")
    private boolean pillageProtection;

    @Inject
    public ClansKillListener(ClanManager clanManager) {
        this.clanManager = clanManager;
    }


    @EventHandler(priority = EventPriority.HIGH)
    public void onClanKill(KillContributionEvent event) {

        Optional<Clan> killerClanOptional = clanManager.getClanByPlayer(event.getKiller());
        if (killerClanOptional.isEmpty()) {
            return;
        }

        Optional<Clan> victimClanOptional = clanManager.getClanByPlayer(event.getVictim());
        if (victimClanOptional.isEmpty()) {
            return;
        }

        Clan killerClan = killerClanOptional.get();
        Clan victimClan = victimClanOptional.get();
        double dominance = 0;

        if (clanManager.getRelation(killerClan, victimClan).equals(ClanRelation.ENEMY)) {


            if ((!killerClan.isNoDominanceCooldownActive() && !victimClan.isNoDominanceCooldownActive()) || !pillageProtection) {
                dominance = clanManager.getDominanceForKill(killerClan.getSquadCount(), victimClan.getSquadCount());
            }
        }


        clanManager.getRepository().addClanKill(event.getKillId(), killerClan, victimClan, dominance);
    }
}
