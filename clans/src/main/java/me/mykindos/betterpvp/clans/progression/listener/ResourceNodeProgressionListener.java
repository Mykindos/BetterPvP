package me.mykindos.betterpvp.clans.progression.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.clans.progression.ProgressionAdapter;
import me.mykindos.betterpvp.clans.world.resource.event.ResourceHarvestEvent;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.model.ConfigAccessor;
import me.mykindos.betterpvp.progression.profession.ProfessionHandler;
import me.mykindos.betterpvp.progression.profession.fishing.FishingHandler;
import me.mykindos.betterpvp.progression.profession.mining.MiningHandler;
import me.mykindos.betterpvp.progression.profession.woodcutting.WoodcuttingHandler;
import me.mykindos.betterpvp.progression.profile.ProfessionData;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * Awards profession XP when a resource node is harvested — the decoupled progression bridge that replaces the old
 * Fields → {@code MiningListener} flow. It listens to {@link ResourceHarvestEvent} (fired by
 * {@code ResourceNodeManager}) and routes by the node's profession:
 * <ul>
 *   <li><b>Mining</b>: looks up the harvested ore's configured mining XP ({@code mining.xpPerBlock}) and grants it,
 *   scaled by {@code resourcenodes.xpMultiplier} — exactly how Fields ore mining was rewarded.</li>
 *   <li><b>Woodcutting / Fishing</b>: grants the node's flat {@code xp} value.</li>
 * </ul>
 * Loaded only when Progression is present (discovered by {@link ProgressionAdapter}), so the resource system stays
 * decoupled from professions.
 */
@Singleton
@CustomLog
@BPvPListener
@PluginAdapter("Progression")
public class ResourceNodeProgressionListener implements Listener, ConfigAccessor {

    private double xpMultiplier;
    private final MiningHandler miningHandler;
    private final WoodcuttingHandler woodcuttingHandler;
    private final FishingHandler fishingHandler;

    @Inject
    public ResourceNodeProgressionListener(ProgressionAdapter adapter) {
        this.miningHandler = adapter.getProgression().getInjector().getInstance(MiningHandler.class);
        this.woodcuttingHandler = adapter.getProgression().getInjector().getInstance(WoodcuttingHandler.class);
        this.fishingHandler = adapter.getProgression().getInjector().getInstance(FishingHandler.class);
    }

    @EventHandler
    public void onHarvest(ResourceHarvestEvent event) {
        final String profession = event.getProfession();
        if (profession == null) {
            return;
        }
        final Player player = event.getPlayer();
        switch (profession.toLowerCase(Locale.ROOT)) {
            case "mining" -> awardMining(player, event);
            case "woodcutting" -> awardFlat(woodcuttingHandler, player, event);
            case "fishing" -> awardFlat(fishingHandler, player, event);
            default -> { /* unknown profession — no XP */ }
        }
    }

    private void awardMining(@NotNull Player player, @NotNull ResourceHarvestEvent event) {
        final Material material = event.getHarvestedMaterial();
        if (material == null) {
            return;
        }
        final long base = miningHandler.getExperienceFor(material);
        if (base <= 0) {
            return;
        }
        final ProfessionData data = miningHandler.getProfessionData(player.getUniqueId());
        if (data != null) {
            data.grantExperience(base * xpMultiplier, player);
        }
    }

    private void awardFlat(@NotNull ProfessionHandler handler, @NotNull Player player, @NotNull ResourceHarvestEvent event) {
        final double xp = event.getNode().getDefinition().getRoot().getDouble("xp", 0.0);
        if (xp <= 0) {
            return;
        }
        final ProfessionData data = handler.getProfessionData(player.getUniqueId());
        if (data != null) {
            data.grantExperience(xp * xpMultiplier, player);
        }
    }

    @Override
    public void loadConfig(@NotNull ExtendedYamlConfiguration config) {
        this.xpMultiplier = config.getOrSaveObject("resourcenodes.xpMultiplier", 5.0d, Double.class);
    }
}
