package me.mykindos.betterpvp.clans.progression.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.progression.ProgressionAdapter;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.ConfigAccessor;
import me.mykindos.betterpvp.progression.profession.skill.woodcutting.ForestFlourisher;
import org.bukkit.TreeType;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * This class will handle claim-checking when place a sapling, so you can't plant saplings too close to foreign
 * claims
 */
@BPvPListener
@Singleton
public class PlantSaplingListener implements Listener, ConfigAccessor {
    private final ClanManager clanManager;
    private final ForestFlourisher forestFlourisherSkill;
    private int saplingPlantDistance;

    @Inject
    public PlantSaplingListener(ProgressionAdapter adapter, ClanManager clanManager) {
        this.clanManager = clanManager;
        this.forestFlourisherSkill = adapter.getProgression().getInjector().getInstance(ForestFlourisher.class);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onSaplingPlant(BlockPlaceEvent event) {
        TreeType treeType = forestFlourisherSkill.getTreeType(event.getBlock());
        if (treeType == null) return;

        final int LOWER_BOUND = -saplingPlantDistance;
        Player player = event.getPlayer();
        Clan playerClan = clanManager.getClanByPlayer(player).orElse(null);

        for (int x = LOWER_BOUND; x < saplingPlantDistance; x++) {
            for (int z = LOWER_BOUND; z < saplingPlantDistance; z++) {
                Block targetBlock = event.getBlockPlaced().getRelative(x, 0, z);

                Optional<Clan> targetBlockLocationClanOptional = clanManager.getClanByLocation(targetBlock.getLocation());
                if (targetBlockLocationClanOptional.isPresent()) {
                    if (playerClan == null || !playerClan.equals(targetBlockLocationClanOptional.get())) {
                        UtilMessage.message(player, "Clans", "Saplings must be placed a minimum of "  + saplingPlantDistance + " blocks away from foreign claims.");
                        event.setCancelled(true);
                        return;
                    }
                }
            }
        }
    }

    @Override
    public void loadConfig(@NotNull ExtendedYamlConfiguration config) {
        this.saplingPlantDistance = config.getOrSaveObject("clans.saplingPlantDistance", 8, Integer.class);
    }
}
