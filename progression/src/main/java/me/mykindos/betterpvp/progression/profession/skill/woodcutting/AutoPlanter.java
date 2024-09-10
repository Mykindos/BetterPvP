package me.mykindos.betterpvp.progression.profession.skill.woodcutting;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.profession.skill.ProgressionSkillDependency;
import me.mykindos.betterpvp.progression.profession.woodcutting.event.PlayerUsesTreeFellerEvent;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.List;

@Singleton
@BPvPListener
public class AutoPlanter extends WoodcuttingProgressionSkill implements Listener {

    private final List<Material> compatibleBlockTypes = List.of(
            Material.DIRT, Material.GRASS_BLOCK, Material.COARSE_DIRT
    );

    private final ProfessionProfileManager professionProfileManager;
    private final ForestFlourisher forestFlourisher;

    @Inject
    public AutoPlanter(Progression progression, ProfessionProfileManager professionProfileManager,
                       ForestFlourisher forestFlourisher) {
        super(progression);
        this.professionProfileManager = professionProfileManager;
        this.forestFlourisher = forestFlourisher;
    }

    @Override
    public String getName() {
        return "Auto Planter";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[] {
                "Whenever you fell a tree, a sapling will be automatically planted.",
                "",
                "This perk is compatible with <green>Forest Flourisher.",
                "This perk does not work on Mangrove trees.",
        };
    }

    @Override
    public Material getIcon() {
        return Material.OAK_SAPLING;
    }

    @Override
    public ProgressionSkillDependency getDependencies() {
        final String[] dependencies = new String[]{"Tree Feller"};
        return new ProgressionSkillDependency(dependencies, 20);
    }

    @EventHandler
    public void whenPlayerFellsTree(PlayerUsesTreeFellerEvent event) {
        Player player = event.getPlayer();

        professionProfileManager.getObject(player.getUniqueId().toString()).ifPresent(profile -> {
            int skillLevel = getPlayerSkillLevel(profile);
            if (skillLevel <= 0) return;

            Location initialLogLocation = event.getInitialChoppedLogLocation();
            Block blockWhereTreeWasChoppedFrom = initialLogLocation.getBlock();
            Material typeOfBlockBelowInitialLog = blockWhereTreeWasChoppedFrom
                    .getRelative(0, -1, 0)
                    .getType();

            if (!compatibleBlockTypes.contains(typeOfBlockBelowInitialLog)) return;

            // Verifying that the tree was felled and there is no log there anymore
            if (!blockWhereTreeWasChoppedFrom.isEmpty()) return;

            UtilServer.runTaskLater(getProgression(), () -> {
                Material saplingToPlant = switch (event.getInitialChoppedLogType()) {
                    case OAK_LOG -> Material.OAK_SAPLING;
                    case BIRCH_LOG -> Material.BIRCH_SAPLING;
                    case JUNGLE_LOG -> Material.JUNGLE_SAPLING;
                    case ACACIA_LOG -> Material.ACACIA_SAPLING;
                    case DARK_OAK_LOG -> Material.DARK_OAK_SAPLING;
                    case SPRUCE_LOG -> Material.SPRUCE_SAPLING;
                    default -> null;
                };

                if (saplingToPlant != null) {
                    player.getWorld().playSound(initialLogLocation, Sound.BLOCK_GRASS_PLACE, 1.0F, 1.0F);
                    blockWhereTreeWasChoppedFrom.setType(saplingToPlant);

                    if (forestFlourisher.doesPlayerHaveSkill(player)) {
                        forestFlourisher.addSaplingForPlayer(player, blockWhereTreeWasChoppedFrom);
                    }
                }
            }, 40L);
        });
    }
}
