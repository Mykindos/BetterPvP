package me.mykindos.betterpvp.clans.clans.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.leveling.ClanXpFormula;
import me.mykindos.betterpvp.clans.clans.leveling.contribution.ClanXpContributionRepository;
import me.mykindos.betterpvp.clans.clans.leveling.events.ClanLevelUpEvent;
import me.mykindos.betterpvp.clans.clans.leveling.xpbar.ClanXpBossBarService;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.components.clans.events.ClanAddExperienceEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

import java.util.Optional;

@BPvPListener
@Singleton
public class ClanExperienceListener extends ClanListener {

    private final ClanXpFormula formula;
    private final ClanXpContributionRepository contributionRepository;
    private final ClanXpBossBarService bossBarService;

    @Inject
    public ClanExperienceListener(ClanManager clanManager, ClientManager clientManager,
                                  ClanXpFormula formula,
                                  ClanXpContributionRepository contributionRepository,
                                  ClanXpBossBarService bossBarService) {
        super(clanManager, clientManager);
        this.formula = formula;
        this.contributionRepository = contributionRepository;
        this.bossBarService = bossBarService;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onExperienceAdded(ClanAddExperienceEvent event) {
        Player player = event.getPlayer();

        Optional<Clan> clanOptional = clanManager.getClanByPlayer(player);
        if (clanOptional.isEmpty()) {
            return;
        }

        Clan clan = clanOptional.get();
        long levelBefore = formula.levelFromXp(clan.getExperience());

        // Grant XP to clan (persisted via the existing PropertyContainer batch-flush pipeline)
        clan.grantExperience(event.getExperience());

        // Track per-member contribution (in-memory + async DB write)
        if (event.getContributor() != null) {
            clan.addContribution(event.getContributor(), event.getExperience());
            contributionRepository.saveContribution(clan, event.getContributor(), event.getExperience());
        }

        // Show aggregated XP boss bar to all online clan members
        bossBarService.notifyXpGain(clan, event.getExperience(), event.getReason());

        // Check for level-up and fire dedicated event
        long levelAfter = formula.levelFromXp(clan.getExperience());
        if (levelAfter > levelBefore) {
            UtilServer.callEvent(new ClanLevelUpEvent(clan, levelBefore, levelAfter));
        }
    }

}
