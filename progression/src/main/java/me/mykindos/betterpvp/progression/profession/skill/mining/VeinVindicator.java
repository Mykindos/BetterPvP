package me.mykindos.betterpvp.progression.profession.skill.mining;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.framework.blocktag.BlockTagManager;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilSound;
import me.mykindos.betterpvp.progression.profession.mining.event.PlayerMinesOreEvent;
import me.mykindos.betterpvp.progression.profession.skill.ProfessionSkillNode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

@Singleton
@BPvPListener
public class VeinVindicator extends ProfessionSkillNode implements Listener {

    @Inject
    private BlockTagManager blockTagManager;

    private double chanceToNotConsumeIncreasePerLevel;


    @Inject
    public VeinVindicator(String name) {
        super("Vein Vindicator");
    }

    @Override
    public String getName() {
        return "Vein Vindicator";
    }


    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "You have a <green>" + UtilMath.round(getChance(level), 2) + "% <reset>chance",
                "to not consume ores when mining,",
                "allowing you to mine the same ore again.",
                "",
                "Does not work at Fields."
        };
    }

    @Override
    public Material getIcon() {
        return Material.HEAVY_CORE;
    }

    public double getChance(int level) {
        return level * chanceToNotConsumeIncreasePerLevel;
    }


    @EventHandler (ignoreCancelled = true)
    public void onBlockBreak(PlayerMinesOreEvent event) {
        Player player = event.getPlayer();
        Block block = event.getMinedOreBlock();
        Material blockType = block.getType();

        final boolean playerPlaced = blockTagManager.isPlayerPlaced(block);

        if (playerPlaced) return;
        if (!UtilBlock.isOre(blockType)) return;

        professionProfileManager.getObject(player.getUniqueId().toString()).ifPresent(profile -> {
            int skillLevel = getPlayerNodeLevel(profile);
            if (skillLevel <= 0) return;
            if (UtilMath.randDouble(0.0, 100.0) > getChance(skillLevel)) return;

            UtilSound.playSound(player, Sound.BLOCK_VAULT_OPEN_SHUTTER, 1.0F, 1.0F, false);
            UtilServer.runTaskLater(getProgression(), () -> {
                block.setType(blockType);
            }, 2L);

        });
    }

    @Override
    public void loadConfig() {
        super.loadConfig();
        chanceToNotConsumeIncreasePerLevel = getConfig("chanceToNotConsumeIncreasePerLevel", 0.08, Double.class);
    }

}
