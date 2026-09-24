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
import me.mykindos.betterpvp.core.world.settler.SettlerRarity;
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
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

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

    /** What {@code key}'s Prosperity is made of now, with no factors if its record is not loaded. */
    public @NotNull ProsperityFactors factors(@NotNull SiteKey key) {
        return settlers.roster(key).map(this::factors).orElseGet(() -> new ProsperityFactors(List.of(), 0));
    }

    /**
     * Splits {@link #of(Roster)} into what each factor adds: the settlers of each rarity, then what the average morale
     * adds to or takes from their worth, then what the best Chronicler adds on top of both.
     */
    @NotNull ProsperityFactors factors(@NotNull Roster roster) {
        if (roster.size() == 0) {
            return new ProsperityFactors(List.of(), 0);
        }
        final List<ProsperityFactors.Factor> factors = new ArrayList<>();
        final Map<SettlerRarity, Integer> counts = new EnumMap<>(SettlerRarity.class);
        roster.getSettlers().forEach(settler -> counts.merge(settler.getRarity(), 1, Integer::sum));
        counts.forEach((rarity, count) -> factors.add(new ProsperityFactors.Factor(ProsperityFactors.Kind.SETTLERS,
                rarity, count, (double) count * config.prosperityValue(rarity))));

        final double worth = roster.getSettlers().stream().mapToInt(this::worth).sum();
        final double morale = roster.getSettlers().stream().mapToInt(Settler::getMorale).average().orElse(0);
        factors.add(new ProsperityFactors.Factor(ProsperityFactors.Kind.MORALE, null, morale, worth * morale / 200));
        final double chronicler = campWide.best(roster, CampTraits.CHRONICLER, "prosperity", 0.03);
        if (chronicler != 0) {
            factors.add(new ProsperityFactors.Factor(ProsperityFactors.Kind.CHRONICLER, null, chronicler,
                    worth * (1 + morale / 200) * chronicler));
        }
        return new ProsperityFactors(factors, of(roster));
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
