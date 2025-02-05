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

import java.util.Objects;

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

        Clan killerClan = clanManager.getClanByPlayer(event.getKiller()).orElse(null);
        Clan victimClan = clanManager.getClanByPlayer(event.getVictim()).orElse(null);


        if (killerClan == null && victimClan == null) {
            return;
        }
        double dominance = 0;

        if (clanManager.getRelation(killerClan, victimClan).equals(ClanRelation.ENEMY)) {
            //a null clan cannot be an enemy of a valid Clan
            if ((!Objects.requireNonNull(killerClan).isNoDominanceCooldownActive() && !Objects.requireNonNull(victimClan).isNoDominanceCooldownActive()) || !pillageProtection) {
                dominance = clanManager.getDominanceForKill(killerClan.getSquadCount(), Objects.requireNonNull(victimClan).getSquadCount());
            }
        }


        clanManager.getRepository().addClanKill(event.getKillId(), event.getKiller().getName(), killerClan, event.getVictim().getName(), victimClan, dominance);
    }
}
