package me.mykindos.betterpvp.progression.profession.skill.woodcutting;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.profession.woodcutting.WoodcuttingHandler;
import me.mykindos.betterpvp.progression.profession.woodcutting.event.PlayerChopLogEvent;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

@Singleton
@BPvPListener
public class TreeFeller extends WoodcuttingProgressionSkill implements Listener {

    private final ProfessionProfileManager professionProfileManager;
    private final WoodcuttingHandler woodcuttingHandler;

    @Inject
    public TreeFeller(Progression progression, ProfessionProfileManager professionProfileManager, WoodcuttingHandler woodcuttingHandler) {
        super(progression);
        this.professionProfileManager = professionProfileManager;
        this.woodcuttingHandler = woodcuttingHandler;
    }

    @Override
    public String getName() {
        return "Tree Feller";
    }

    // add cooldown calculation in here!
    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Cut down an entire tree by chopping a single log",
                "Cooldown tbd"
        };
    }

    @Override
    public Material getIcon() {
        return Material.GOLDEN_AXE;
    }


    @EventHandler
    public void onPlayerChopsLog(PlayerChopLogEvent event) {
        if (event.isCancelled()) return;

        Player player = event.getPlayer();
        professionProfileManager.getObject(player.getUniqueId().toString()).ifPresent(profile -> {
            var profession = profile.getProfessionDataMap().get("Woodcutting");
            if (profession != null) {
                int skillLevel = profession.getBuild().getSkillLevel(this);
                if (skillLevel <= 0) return;

                event.setCancelled(true);
                fellTree(event.getChoppedLogBlock(), event, true);
            }
        });
    }

    public void fellTree(Block block, PlayerChopLogEvent event, boolean initialBlock) {
        if (!initialBlock && woodcuttingHandler.didPlayerPlaceBlock(block)) return;

        // attempt to chop log comes before breakNaturally b/c after you break the block, it becomes air
        // which you don't get xp from
        block.breakNaturally();
        event.setAmountChopped(event.getAmountChopped() + 1);

        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                Block targetBlock = block.getRelative(x, 1, z);

                if(targetBlock.getType().name().contains("_LOG")) {
                    fellTree(targetBlock, event, false);
                }
            }
        }
    }
}
