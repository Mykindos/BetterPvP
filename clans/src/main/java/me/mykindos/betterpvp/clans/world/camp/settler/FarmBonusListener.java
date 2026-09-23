package me.mykindos.betterpvp.clans.world.camp.settler;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.world.camp.Camps;
import me.mykindos.betterpvp.clans.world.camp.protection.CampGrounds;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import me.mykindos.betterpvp.core.world.zone.ZoneManager;
import org.bukkit.Location;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.OptionalLong;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Puts the Farmers' bonus on a camp's farm. A crop that grows there may grow extra stages at once, which is what
 * growing faster looks like, and a fully grown crop harvested there may drop one more of what it gave.
 */
@BPvPListener
@Singleton
public class FarmBonusListener implements Listener {

    private final Camps camps;
    private final ZoneManager zones;
    private final FarmWorkplace farm;

    @Inject
    public FarmBonusListener(@NotNull Camps camps, @NotNull ZoneManager zones, @NotNull FarmWorkplace farm) {
        this.camps = camps;
        this.zones = zones;
        this.farm = farm;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onGrow(@NotNull BlockGrowEvent event) {
        final BlockState grown = event.getNewState();
        if (!(grown.getBlockData() instanceof Ageable crop)) {
            return;
        }
        final SiteKey site = farmAt(grown.getLocation());
        if (site == null) {
            return;
        }
        final int extra = stages(farm.growth(site), ThreadLocalRandom.current().nextDouble());
        if (extra > 0) {
            crop.setAge(Math.min(crop.getMaximumAge(), crop.getAge() + extra));
            grown.setBlockData(crop);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHarvest(@NotNull BlockDropItemEvent event) {
        if (!(event.getBlockState().getBlockData() instanceof Ageable crop) || crop.getAge() < crop.getMaximumAge()
                || event.getItems().isEmpty()) {
            return;
        }
        final Location at = event.getBlockState().getLocation();
        final SiteKey site = farmAt(at);
        if (site == null || ThreadLocalRandom.current().nextDouble() >= farm.extraDrop(site)) {
            return;
        }
        final Item first = event.getItems().getFirst();
        final ItemStack extra = first.getItemStack().clone();
        extra.setAmount(1);
        at.getWorld().dropItemNaturally(at.clone().add(0.5, 0.5, 0.5), extra);
    }

    /** The camp whose farm {@code location} is on, if it is on one. */
    private @Nullable SiteKey farmAt(@NotNull Location location) {
        final OptionalLong clan = camps.clanOf(location.getWorld());
        if (clan.isEmpty() || !zones.hasTagAt(location, CampGrounds.FARM)) {
            return null;
        }
        return Camps.keyFor(clan.getAsLong());
    }

    /** Extra stages for one growth: every whole share of bonus is a stage, and what is left is the chance of one more. */
    static int stages(double bonus, double roll) {
        final int whole = (int) Math.floor(bonus);
        return whole + (roll < bonus - whole ? 1 : 0);
    }
}
