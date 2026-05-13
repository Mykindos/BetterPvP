package me.mykindos.betterpvp.clans.progression.perks;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.listeners.ClansWorldListener;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.profession.skill.ProfessionNode;
import me.mykindos.betterpvp.progression.profession.skill.ProfessionNodeManager;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.Objects;
import java.util.Optional;

@Singleton
@BPvPListener
@PluginAdapter("Progression")
public class AggressiveRodderListener implements Listener {

    private final Clans clans;
    private final ClanManager clanManager;
    private final ProfessionProfileManager professionProfileManager;
    private final ProfessionNodeManager progressionSkillManager;

    @Inject
    public AggressiveRodderListener(final Clans clans, final ClanManager clanManager) {
        this.clans = clans;
        this.clanManager = clanManager;

        final Progression progression = Objects.requireNonNull((Progression) Bukkit.getPluginManager().getPlugin("Progression"));
        this.professionProfileManager = progression.getInjector().getInstance(ProfessionProfileManager.class);
        this.progressionSkillManager = progression.getInjector().getInstance(ProfessionNodeManager.class);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onFish(final PlayerFishEvent event) {
        if (!(event.getCaught() instanceof Player target)) {
            return;
        }

        final Player caster = event.getPlayer();

        if(hasAggressiveRodder(caster)) {
            caster.setMetadata(ClansWorldListener.AGGRESSIVE_RODDER_UNLOCKED, new FixedMetadataValue(clans, true));
            return;
        }
    }

    private boolean hasAggressiveRodder(final Player player) {
        Optional<ProfessionNode> skill = progressionSkillManager.getSkill("aggressive_rodder");
        if (skill.isEmpty()) {
            return false;
        }

        return professionProfileManager.getObject(player.getUniqueId().toString())
                .map(profile -> profile.getProfessionDataMap().get("Fishing"))
                .map(data -> data.getBuild().getSkillLevel(skill.get()) >= 1)
                .orElse(false);
    }
}
