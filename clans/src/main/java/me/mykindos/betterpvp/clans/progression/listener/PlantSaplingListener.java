package me.mykindos.betterpvp.clans.progression.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.progression.ProgressionAdapter;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.ConfigAccessor;
import me.mykindos.betterpvp.progression.profession.skill.woodcutting.ForestFlourisherSkill;
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
 * This class stops you from planting saplings too fast
 */
@BPvPListener
@Singleton
public class PlantSaplingListener implements Listener, ConfigAccessor {
    private final CooldownManager cooldownManager;
    private final ClanManager clanManager;
    private final ForestFlourisherSkill forestFlourisherSkill;
    private double saplingCooldown;
    final String COOLDOWN_NAME = "PlantSapling";

    @Inject
    public PlantSaplingListener(ProgressionAdapter adapter, CooldownManager cooldownManager, ClanManager clanManager) {
        this.cooldownManager = cooldownManager;
        this.clanManager = clanManager;
        this.forestFlourisherSkill = adapter.getProgression().getInjector().getInstance(ForestFlourisherSkill.class);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onSaplingPlant(BlockPlaceEvent event) {
        TreeType treeType = forestFlourisherSkill.getTreeType(event.getBlock());
        if (treeType == null) return;

        Player player = event.getPlayer();

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

        if (cooldownManager.hasCooldown(player, COOLDOWN_NAME)) {
            double remainingSeconds = cooldownManager.getAbilityRecharge(player, COOLDOWN_NAME).getRemaining();
            UtilMessage.simpleMessage(player, "Clans", "You cant use " + COOLDOWN_NAME + " for <alt>" + remainingSeconds + "</alt> seconds");
            event.setCancelled(true);
            return;
        }

        cooldownManager.use(player, COOLDOWN_NAME, saplingCooldown, true);
    }

    @Override
    public void loadConfig(@NotNull ExtendedYamlConfiguration config) {
        this.saplingCooldown = config.getOrSaveObject("clans.plantSaplingCooldown", 5.0, Double.class);
    }
}
