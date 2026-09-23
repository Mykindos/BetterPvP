package me.mykindos.betterpvp.core.world.settler.wage;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilServer;
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
 * Pays a site's settlers from its wage fund, in real time.
 * <p>
 * Wages are worked out from when they were last settled, so a site whose world was closed for a day is charged for the
 * whole day the next time its world opens. When the fund runs out, every paid settler goes on strike from the moment
 * the money ran out, and a striker still unpaid after the site's strike limit leaves for good. Strikers are not paid,
 * and they go back to work once the fund can pay everyone for a minute.
 * <p>
 * Only the server holding a site's world settles it, so two servers never charge the same fund.
 */
@BPvPListener
@Singleton
public class Payroll implements Listener {

    private static final double HOUR_MILLIS = 3_600_000.0;

    private final SettlerService settlers;
    private final SiteInstances instances;
    private final LongSupplier clock;

    @Inject
    public Payroll(@NotNull SettlerService settlers, @NotNull SiteInstances instances) {
        this(settlers, instances, System::currentTimeMillis);
    }

    Payroll(@NotNull SettlerService settlers, @NotNull SiteInstances instances, @NotNull LongSupplier clock) {
        this.settlers = settlers;
        this.instances = instances;
        this.clock = clock;
    }

    @UpdateEvent(delay = 60_000)
    public void tick() {
        for (SiteInstance instance : new ArrayList<>(instances.all())) {
            if (Bukkit.getWorld(instance.getWorldName()) != null) {
                settle(instance.getKey());
            }
        }
    }

    /** Coins an hour {@code settler} costs as it is now, or 0 if it is not paid. */
    public double hourly(@NotNull SiteKey key, @NotNull Settler settler) {
        final SettlerSite site = settlers.site(key).orElse(null);
        if (site == null || settler.getState() == SettlerState.LEAVING) {
            return 0;
        }
        final boolean working = settler.getState() == SettlerState.WORKING
                || settler.getState() == SettlerState.STRIKING && settler.getAssignment() != null;
        return site.wageModel(key)
                .map(model -> model.perHour(settler, working) * site.wageMultiplier(key, settler))
                .orElse(0.0);
    }

    /** Coins an hour the whole site costs with everyone at work, strikers included. */
    public double hourly(@NotNull SiteKey key) {
        return settlers.roster(key).map(Roster::getSettlers).orElse(List.of()).stream()
                .mapToDouble(settler -> hourly(key, settler))
                .sum();
    }

    /** Charges what is owed since wages were last settled, and starts, ends and times out strikes. */
    public void settle(@NotNull SiteKey key) {
        final SettlerSite site = settlers.site(key).orElse(null);
        final Roster roster = settlers.roster(key).orElse(null);
        if (site == null || roster == null) {
            return;
        }
        final long now = clock.getAsLong();
        final CoinAccount fund = site.wageFund(key).orElse(null);
        if (fund == null || site.wageModel(key).isEmpty() || roster.getPayrollAt() == 0) {
            roster.setPayrollAt(now);
            site.changed(key);
            return;
        }
        final long from = roster.getPayrollAt();
        if (now <= from) {
            return;
        }

        final List<Settler> paid = roster.getSettlers().stream()
                .filter(settler -> settler.getState() != SettlerState.STRIKING)
                .filter(settler -> hourly(key, settler) > 0)
                .toList();
        final double perMilli = paid.stream().mapToDouble(settler -> hourly(key, settler)).sum() / HOUR_MILLIS;
        final double owed = perMilli * (now - from) + roster.getPayrollCarry();
        final long charge = (long) owed;
        final long balance = fund.balance(key);

        final List<Settler> struck = new ArrayList<>();
        if (charge <= balance) {
            fund.withdraw(key, charge);
            roster.setPayrollCarry(owed - charge);
        } else {
            final long ranOut = from + (long) (balance / perMilli);
            fund.withdraw(key, balance);
            roster.setPayrollCarry(0);
            for (Settler settler : paid) {
                settler.changeState(SettlerState.STRIKING, ranOut);
                struck.add(settler);
            }
        }
        roster.setPayrollAt(now);

        final List<Settler> resumed = new ArrayList<>();
        final List<Settler> striking = roster.inState(SettlerState.STRIKING);
        if (struck.isEmpty() && !striking.isEmpty() && fund.balance(key) >= Math.max(1, hourly(key) / 60)) {
            for (Settler settler : striking) {
                settler.changeState(settler.getAssignment() == null ? SettlerState.IDLE : SettlerState.WORKING, now);
                resumed.add(settler);
            }
        }

        site.changed(key);
        if (!struck.isEmpty()) {
            UtilServer.callEvent(new SettlerStrikeEvent(key, struck, true));
        }
        if (!resumed.isEmpty()) {
            UtilServer.callEvent(new SettlerStrikeEvent(key, resumed, false));
        }

        final long limit = site.strikeLimit(key).toMillis();
        for (Settler settler : roster.inState(SettlerState.STRIKING)) {
            if (now - settler.getStateSince() >= limit) {
                settlers.remove(key, settler.getId(), SettlerLeaveReason.UNPAID);
            }
        }
    }
}
