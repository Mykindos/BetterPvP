package me.mykindos.betterpvp.progression.profession.skill.mining;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

@Singleton
@BPvPListener
public class CaveCaller extends MiningProgressionSkill implements Listener {
    ProfessionProfileManager professionProfileManager;

    @Inject
    public CaveCaller(Progression progression, ProfessionProfileManager professionProfileManager) {
        super(progression);
        this.professionProfileManager = professionProfileManager;
    }

    @Override
    public String getName() {
        return "Cave Caller";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Mine any stone block in your territory for a <green>20%</green> chance",
                "to summon a cave monster. These monsters drop",
                "various armaments.",
                "",
                "Toggle this perk on/off with <green>/cavecaller</green>"
        };
    }

    @Override
    public Material getIcon() {
        return Material.IRON_HELMET;
    }

    @Override
    public void loadConfig() {
        super.loadConfig();
    }

    @EventHandler
    public void onBreakStoneBlock(BlockBreakEvent event) {
        if (event.isCancelled()) return;

        event.getPlayer().sendMessage("Broken block " + event.getBlock().getType());
    }
}
