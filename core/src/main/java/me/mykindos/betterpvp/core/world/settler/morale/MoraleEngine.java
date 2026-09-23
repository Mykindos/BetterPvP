package me.mykindos.betterpvp.core.world.settler.morale;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.world.settler.Roster;
import me.mykindos.betterpvp.core.world.settler.Settler;
import me.mykindos.betterpvp.core.world.settler.SettlerLeaveReason;
import me.mykindos.betterpvp.core.world.settler.SettlerService;
import me.mykindos.betterpvp.core.world.settler.SettlerSite;
import me.mykindos.betterpvp.core.world.settler.SettlerState;
import me.mykindos.betterpvp.core.world.site.SiteInstance;
import me.mykindos.betterpvp.core.world.site.SiteInstances;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.LongSupplier;

/**
 * Keeps every settler's morale current, and sends away a settler that has been unhappy for too long. Like wages, only
 * the server holding a site's world settles it.
 */
@BPvPListener
@Singleton
public class MoraleEngine implements Listener {

    private final SettlerService settlers;
    private final SiteInstances instances;
    private final LongSupplier clock;

    @Inject
    public MoraleEngine(@NotNull SettlerService settlers, @NotNull SiteInstances instances) {
        this(settlers, instances, System::currentTimeMillis);
    }

    MoraleEngine(@NotNull SettlerService settlers, @NotNull SiteInstances instances, @NotNull LongSupplier clock) {
        this.settlers = settlers;
        this.instances = instances;
        this.clock = clock;
    }

    /** What a settler's resident bonuses are multiplied by: 0.5 at -100 morale, 1 at neutral, 1.5 at 100. */
    public static double multiplier(@NotNull Settler settler) {
        return 1 + settler.getMorale() / 200.0;
    }

    @UpdateEvent(delay = 60_000)
    public void tick() {
        for (SiteInstance instance : new ArrayList<>(instances.all())) {
            if (Bukkit.getWorld(instance.getWorldName()) != null) {
                settle(instance.getKey());
            }
        }
    }

    /** Works out every settler's morale at {@code key} again, and lets go of those unhappy for long enough. */
    public void settle(@NotNull SiteKey key) {
        final SettlerSite site = settlers.site(key).orElse(null);
        final Roster roster = settlers.roster(key).orElse(null);
        final MoraleModel model = site == null ? null : site.moraleModel(key).orElse(null);
        if (roster == null || model == null) {
            return;
        }

        final long now = clock.getAsLong();
        final List<Settler> leaving = new ArrayList<>();
        for (Settler settler : roster.getSettlers()) {
            if (settler.getState() == SettlerState.LEAVING) {
                continue;
            }
            settler.setMorale(Math.clamp(model.morale(key, settler, roster, now), -100, 100));
            if (settler.getMorale() >= model.leaveBelow() || !model.mayLeave(settler)) {
                settler.setUnhappySince(0);
                continue;
            }
            if (settler.getUnhappySince() == 0) {
                settler.setUnhappySince(now);
            } else if (now - settler.getUnhappySince() >= model.leaveAfter().toMillis()) {
                leaving.add(settler);
            }
        }
        site.changed(key);
        leaving.forEach(settler -> settlers.remove(key, settler.getId(), SettlerLeaveReason.UNHAPPY));
    }
}
