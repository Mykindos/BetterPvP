package me.mykindos.betterpvp.progression.profession.skill.woodcutting;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.progression.profession.skill.ProfessionSkillNode;
import org.bukkit.Material;

import java.util.List;

@Singleton
public abstract class AutoPlanter extends ProfessionSkillNode {

    private final List<Material> compatibleBlockTypes = List.of(
            Material.DIRT, Material.GRASS_BLOCK, Material.COARSE_DIRT
    );

    @Inject
    public AutoPlanter(String name) {
        super(name);
    }

    @Override
    public String getName() {
        return name;
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

    /*@EventHandler
    public void whenPlayerFellsTree(PlayerUsesTreeFellerEvent event) {
        Player player = event.getPlayer();

        professionProfileManager.getObject(player.getUniqueId().toString()).ifPresent(profile -> {
            int skillLevel = getPlayerNodeLevel(profile);
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
                    case CHERRY_LOG -> Material.CHERRY_SAPLING;
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
    }*/

}
