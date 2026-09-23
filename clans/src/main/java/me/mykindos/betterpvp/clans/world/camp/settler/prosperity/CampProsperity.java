package me.mykindos.betterpvp.clans.world.camp.settler.prosperity;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.events.ClanDisbandEvent;
import me.mykindos.betterpvp.clans.world.camp.Camps;
import me.mykindos.betterpvp.clans.world.camp.settler.CampTraits;
import me.mykindos.betterpvp.clans.world.camp.settler.CampWideTraits;
import me.mykindos.betterpvp.clans.world.camp.settler.SettlerConfig;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.world.settler.Roster;
import me.mykindos.betterpvp.core.world.settler.Settler;
import me.mykindos.betterpvp.core.world.settler.SettlerService;
import me.mykindos.betterpvp.core.world.site.SiteInstance;
import me.mykindos.betterpvp.core.world.site.SiteInstances;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

/**
 * A camp's Prosperity: every settler is worth its rarity's value, and the total rises or falls with the camp's
 * average morale and any Chronicler. It is worked out whenever it is asked for, and written to the
 * {@link ProsperityStore} every ten minutes by the server holding the camp's world, so the leaderboard can rank camps
 * that are not loaded.
 */
@BPvPListener
@Singleton
public class CampProsperity implements Listener {

    private final SettlerConfig config;
    private final CampWideTraits campWide;
    private final SettlerService settlers;
    private final SiteInstances instances;
    private final ProsperityStore store;

    @Inject
    public CampProsperity(@NotNull SettlerConfig config, @NotNull CampWideTraits campWide,
                          @NotNull SettlerService settlers, @NotNull SiteInstances instances,
                          @NotNull ProsperityStore store) {
        this.config = config;
        this.campWide = campWide;
        this.settlers = settlers;
        this.instances = instances;
        this.store = store;
    }

    /** {@code key}'s Prosperity now, or 0 if its record is not loaded. */
    public int of(@NotNull SiteKey key) {
        return settlers.roster(key).map(this::of).orElse(0);
    }

    int of(@NotNull Roster roster) {
        if (roster.size() == 0) {
            return 0;
        }
        final double worth = roster.getSettlers().stream().mapToInt(this::worth).sum();
        final double morale = roster.getSettlers().stream().mapToInt(Settler::getMorale).average().orElse(0);
        final double chronicler = campWide.best(roster, CampTraits.CHRONICLER, "prosperity", 0.03);
        return (int) Math.round(worth * (1 + morale / 200) * (1 + chronicler));
    }

    private int worth(@NotNull Settler settler) {
        return config.prosperityValue(settler.getRarity());
    }

    @UpdateEvent(delay = 600_000)
    public void save() {
        for (SiteInstance instance : new ArrayList<>(instances.all())) {
            final SiteKey key = instance.getKey();
            if (key.getSiteId().equals(Camps.SITE_ID) && Bukkit.getWorld(instance.getWorldName()) != null) {
                settlers.roster(key).ifPresent(roster -> store.save(key.getOwnerId(), of(roster)));
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDisband(@NotNull ClanDisbandEvent event) {
        store.delete(event.getClan().getId());
    }
}
