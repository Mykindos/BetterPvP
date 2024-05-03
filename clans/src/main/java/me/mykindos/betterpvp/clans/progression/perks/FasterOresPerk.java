package me.mykindos.betterpvp.clans.progression.perks;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.fields.event.FieldsInteractableUseEvent;
import me.mykindos.betterpvp.clans.progression.ProgressionAdapter;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import me.mykindos.betterpvp.core.utilities.model.ConfigAccessor;
import me.mykindos.betterpvp.progression.model.ProgressionPerk;
import me.mykindos.betterpvp.progression.model.ProgressionTree;
import me.mykindos.betterpvp.progression.model.stats.ProgressionData;
import me.mykindos.betterpvp.progression.tree.mining.Mining;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@Singleton
@CustomLog
public class FasterOresPerk implements Listener, ConfigAccessor, ProgressionPerk {

    private boolean enabled;
    private int requiredLevel;
    private double speedMultiplier;
    private final ClanManager manager;
    private final Mining mining;

    @Inject
    public FasterOresPerk(final ClanManager clanManager, final ProgressionAdapter adapter) {
        this.manager = clanManager;
        this.mining = adapter.getProgression().getInjector().getInstance(Mining.class);
    }

    @Override
    public String getName() {
        return "Faster Ores";
    }

    @Override
    public Class<? extends ProgressionTree>[] acceptedTrees() {
        return new Class[] {
                Mining.class
        };
    }

    @Override
    public boolean canUse(Player player, ProgressionData<?> data) {
        return data.getLevel() > requiredLevel;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFieldsOreMine(FieldsInteractableUseEvent event) {
        if (!enabled || !mining.isEnabled()) return;

        final Location location = event.getBlock().getBlock().getLocation();
        final Player player = event.getPlayer();
        final Optional<Clan> clan = manager.getClanByLocation(location);
        mining.hasPerk(event.getPlayer(), getClass()).whenComplete((hasPerk, throwable) -> {
            if (!hasPerk) {
                return; // Cancel if they don't have the perk
            }

            // Cancel if they're not in fields or lake
            if (clan.map(c -> !c.getName().equalsIgnoreCase("Fields")).orElse(true)) {
                return;
            }

            // At this point, they can mine it, so we'll just make it faster
            // Just subtract half the respawn delay to the last used time so it
            // seems like it was mined before
            final long half = (long) (event.getType().getRespawnDelay() * (1 - 1 / this.speedMultiplier) * 1000);
            event.getBlock().setLastUsed(event.getBlock().getLastUsed() - half);
        }).exceptionally(throwable -> {
            log.error("Failed to check if player " + player.getName() + " has perk " + getName(), throwable).submit();
            return null;
        });
    }

    @Override
    public void loadConfig(@NotNull ExtendedYamlConfiguration config) {
        this.requiredLevel = config.getOrSaveObject("mining.faster-ores-perk.level", 250, Integer.class);
        this.speedMultiplier = config.getOrSaveObject("mining.faster-ores-perk.speed-multiplier", 2.0, Double.class);
        this.enabled = config.getOrSaveBoolean("mining.faster-ores-perk.enabled", true);
    }
}
