package me.mykindos.betterpvp.clans.clans.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.events.ClanGainExperienceEvent;
import me.mykindos.betterpvp.clans.clans.leveling.contribution.ClanXpContributionRepository;
import me.mykindos.betterpvp.clans.clans.leveling.xpbar.ClanXpBossBarService;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

@BPvPListener
@Singleton
public class ClanExperienceListener extends ClanListener {

    private final ClanXpContributionRepository contributionRepository;
    private final ClanXpBossBarService bossBarService;

    @Inject
    public ClanExperienceListener(ClanManager clanManager, ClientManager clientManager,
                                  ClanXpContributionRepository contributionRepository,
                                  ClanXpBossBarService bossBarService) {
        super(clanManager, clientManager);
        this.contributionRepository = contributionRepository;
        this.bossBarService = bossBarService;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onExperienceAdded(ClanGainExperienceEvent event) {
        Player player = event.getPlayer();
        Clan clan = event.getClan();
        bossBarService.notifyXpGain(player, clan, event.getExperience(), event.getReason());
    }

}
