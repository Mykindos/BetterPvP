package me.mykindos.betterpvp.clans.progression.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.progression.ProgressionAdapter;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.ConfigAccessor;
import me.mykindos.betterpvp.progression.profession.skill.woodcutting.ForestFlourisherSkill;
import org.bukkit.TreeType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.jetbrains.annotations.NotNull;

/**
 * This class stops you from planting saplings too fast
 */
@BPvPListener
@Singleton
public class PlantSaplingListener implements Listener, ConfigAccessor {
    final CooldownManager cooldownManager;
    final ForestFlourisherSkill forestFlourisherSkill;
    double saplingCooldown;
    final String COOLDOWN_NAME = "PlantSapling";

    @Inject
    public PlantSaplingListener(ProgressionAdapter adapter, CooldownManager cooldownManager) {
        this.cooldownManager = cooldownManager;
        this.forestFlourisherSkill = adapter.getProgression().getInjector().getInstance(ForestFlourisherSkill.class);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onSaplingPlant(BlockPlaceEvent event) {
        TreeType treeType = forestFlourisherSkill.getTreeType(event.getBlock());
        if (treeType == null) return;

        Player player = event.getPlayer();

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
