package me.mykindos.betterpvp.clans.progression.perks;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.core.framework.CoreNamespaceKeys;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.profession.skill.ProgressionSkill;
import me.mykindos.betterpvp.progression.profession.skill.ProgressionSkillManager;
import me.mykindos.betterpvp.progression.profession.skill.woodcutting.ForestFlourisherSkill;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
import org.bukkit.Bukkit;
import org.bukkit.TreeType;
import org.bukkit.block.Block;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.Optional;

@Singleton
@BPvPListener
@PluginAdapter("Progression")
public class ForestFlourisher implements Listener {
    private final ClanManager clanManager;
    private final ProfessionProfileManager professionProfileManager;
    private final ProgressionSkillManager progressionSkillManager;
    private final ForestFlourisherSkill forestFlourisherSkill;

    @Inject
    public ForestFlourisher(ClanManager clanManager) {
        this.clanManager = clanManager;
        final Progression progression = Objects.requireNonNull((Progression) Bukkit.getPluginManager().getPlugin("Progression"));
        this.professionProfileManager = progression.getInjector().getInstance(ProfessionProfileManager.class);
        this.progressionSkillManager = progression.getInjector().getInstance(ProgressionSkillManager.class);
        this.forestFlourisherSkill = progression.getInjector().getInstance(ForestFlourisherSkill.class);
    }

    @EventHandler
     public void onPlayerPlantSapling(BlockPlaceEvent event) {
        if (event.isCancelled()) return;

        TreeType treeType = forestFlourisherSkill.getTreeType(event.getBlock());
        if (treeType == null) return;

        // remove before pr
        event.getPlayer().sendMessage("you planted a sapling");

        Optional<ProgressionSkill> progressionSkillOptional = progressionSkillManager.getSkill("Forest Flourisher");
        if(progressionSkillOptional.isEmpty()) return;

        ProgressionSkill skill = progressionSkillOptional.get();

        Player player = event.getPlayer();
        professionProfileManager.getObject(player.getUniqueId().toString()).ifPresent(profile -> {

            var profession = profile.getProfessionDataMap().get("Woodcutting");
            if (profession == null) return;

            int skillLevel = profession.getBuild().getSkillLevel(skill);
            if (skillLevel <= 0) return;

            final int BLOCKS_AWAY_FROM_CLAIM = 8;
            final int LOWER_BOUND = -BLOCKS_AWAY_FROM_CLAIM;

            Clan playerClan = clanManager.getClanByPlayer(player).orElse(null);

            for (int x = LOWER_BOUND; x < BLOCKS_AWAY_FROM_CLAIM; x++) {
                for (int z = LOWER_BOUND; z < BLOCKS_AWAY_FROM_CLAIM; z++) {
                    Block targetBlock = event.getBlockPlaced().getRelative(x, 0, z);

                    Optional<Clan> targetBlockLocationClanOptional = clanManager.getClanByLocation(targetBlock.getLocation());
                    if (targetBlockLocationClanOptional.isPresent()) {
                        if (playerClan == null || !playerClan.equals(targetBlockLocationClanOptional.get())) {
                            UtilMessage.message(player, "Clans", "Saplings must be placed a minimum of 8 blocks away from foreign claims.");
                            event.setCancelled(true);
                            return;
                        }
                    }
                }
            }

            // purpose of the 0.5's is to center the particle
            Location center = event.getBlock().getLocation().add(0.5, 1.5, 0.5);
            final int particleCount = 20;
            final double radius = 0.75;
            final double decreaseInYLvlPerParticle = 0.05;

            for (int i = 0; i < particleCount; i++) {
                double angle = 2 * Math.PI * i / particleCount;
                double x = center.getX() + radius * Math.cos(angle);
                double z = center.getZ() + radius * Math.sin(angle);

                Location particleLocation = new Location(center.getWorld(), x, center.getY() - i*decreaseInYLvlPerParticle, z);

                UtilServer.runTaskLater(JavaPlugin.getPlugin(Progression.class), () -> {
                    event.getBlock().getWorld().spawnParticle(Particle.CHERRY_LEAVES, particleLocation, 0, 0, -1, 0);
                }, i);
            }

            UtilServer.runTaskLater(JavaPlugin.getPlugin(Progression.class), () -> {
                Block block = event.getBlock();

                if (forestFlourisherSkill.getTreeType(block) == null) return;

                block.setType(Material.AIR);

                // if the player is in creative then this if statement probably won't have to trigger
                final PersistentDataContainer persistentDataContainer = UtilBlock.getPersistentDataContainer(block);
                if (persistentDataContainer.has(CoreNamespaceKeys.PLAYER_PLACED_KEY)) {
                    persistentDataContainer.remove(CoreNamespaceKeys.PLAYER_PLACED_KEY);
                    UtilBlock.setPersistentDataContainer(block, persistentDataContainer);
                }

                block.getWorld().generateTree(block.getLocation(), treeType);
            }, forestFlourisherSkill.growTime(skillLevel) * 20L);
        });
    }
}
