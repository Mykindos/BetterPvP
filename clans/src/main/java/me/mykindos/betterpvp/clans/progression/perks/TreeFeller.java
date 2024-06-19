package me.mykindos.betterpvp.clans.progression.perks;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.profession.skill.ProgressionSkill;
import me.mykindos.betterpvp.progression.profession.skill.ProgressionSkillManager;
import me.mykindos.betterpvp.progression.profession.woodcutting.WoodcuttingHandler;
import me.mykindos.betterpvp.progression.profession.woodcutting.event.PlayerChopLogEvent;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Objects;
import java.util.Optional;

@Singleton
@BPvPListener
@PluginAdapter("Progression")
public class TreeFeller implements Listener {

    private final ClanManager clanManager;
    private final ProfessionProfileManager professionProfileManager;
    private final ProgressionSkillManager progressionSkillManager;
    private final WoodcuttingHandler woodcuttingHandler;

    @Inject
    public TreeFeller(ClanManager clanManager) {
        this.clanManager = clanManager;
        final Progression progression = Objects.requireNonNull((Progression) Bukkit.getPluginManager().getPlugin("Progression"));
        this.professionProfileManager = progression.getInjector().getInstance(ProfessionProfileManager.class);
        this.progressionSkillManager = progression.getInjector().getInstance(ProgressionSkillManager.class);
        this.woodcuttingHandler = progression.getInjector().getInstance(WoodcuttingHandler.class);
    }

    @EventHandler
    public void onPlayerChopsLog(PlayerChopLogEvent event) {
        if (event.isCancelled()) return;

        Optional<ProgressionSkill> progressionSkillOptional = progressionSkillManager.getSkill("Tree Feller");
        if(progressionSkillOptional.isEmpty()) return;

        ProgressionSkill skill = progressionSkillOptional.get();

        Player player = event.getPlayer();
        professionProfileManager.getObject(player.getUniqueId().toString()).ifPresent(profile -> {

            var profession = profile.getProfessionDataMap().get("Woodcutting");
            if (profession == null) return;

            int skillLevel = profession.getBuild().getSkillLevel(skill);
            if (skillLevel <= 0) return;

            Clan playerClan = clanManager.getClanByPlayer(player).orElse(null);

            event.setCancelled(true);
            fellTree(playerClan, event.getChoppedLogBlock(), event, true);
        });
    }

    public void fellTree(Clan playerClan, Block block, PlayerChopLogEvent event, boolean initialBlock) {
        if (!initialBlock && woodcuttingHandler.didPlayerPlaceBlock(block)) return;

        block.breakNaturally();
        event.setAmountChopped(event.getAmountChopped() + 1);

        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                Block targetBlock = block.getRelative(x, 1, z);

                if(targetBlock.getType().name().contains("_LOG")) {
                    Optional<Clan> targetBlockLocationClanOptional = clanManager.getClanByLocation(targetBlock.getLocation());
                    if (targetBlockLocationClanOptional.isPresent()) {
                        if (!targetBlockLocationClanOptional.get().equals(playerClan)) continue;
                    }

                    fellTree(playerClan, targetBlock, event, false);
                }
            }
        }
    }
}
