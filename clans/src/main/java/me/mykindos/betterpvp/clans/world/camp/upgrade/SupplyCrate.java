package me.mykindos.betterpvp.clans.world.camp.upgrade;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.world.camp.CampConfig;
import me.mykindos.betterpvp.clans.world.camp.Camps;
import me.mykindos.betterpvp.clans.world.camp.structure.CampStructures;
import me.mykindos.betterpvp.clans.world.camp.structure.CampUpgrades;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Barracks upgrade: a member respawns with the items it lists, at most once per cooldown. */
@BPvPListener
@Singleton
public class SupplyCrate implements Listener {

    public static final String ID = "supply_crate";

    private final Clans clans;
    private final ClanManager clanManager;
    private final CampConfig config;
    private final CampUpgrades upgrades;
    private final ItemFactory itemFactory;
    private final Map<UUID, Long> lastSupplied = new HashMap<>();

    @Inject
    public SupplyCrate(@NotNull Clans clans, @NotNull ClanManager clanManager, @NotNull CampConfig config,
                       @NotNull CampUpgrades upgrades, @NotNull ItemFactory itemFactory) {
        this.clans = clans;
        this.clanManager = clanManager;
        this.config = config;
        this.upgrades = upgrades;
        this.itemFactory = itemFactory;
        upgrades.declare(CampStructures.BARRACKS, ID, 1);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(@NotNull PlayerRespawnEvent event) {
        final Player player = event.getPlayer();
        final boolean upgraded = clanManager.getClanByPlayer(player)
                .map(clan -> upgrades.has(Camps.keyFor(clan), CampStructures.BARRACKS, ID))
                .orElse(false);
        if (!upgraded) {
            return;
        }
        config.upgrade(CampStructures.BARRACKS, ID).ifPresent(numbers -> {
            final long now = System.currentTimeMillis();
            final long cooldown = numbers.setting("cooldown-seconds", 300) * 1000L;
            final Long last = lastSupplied.get(player.getUniqueId());
            if (last != null && now - last < cooldown) {
                return;
            }
            lastSupplied.put(player.getUniqueId(), now);
            Bukkit.getScheduler().runTask(clans, () -> supply(player, numbers.amounts("items")));
        });
    }

    private void supply(@NotNull Player player, @NotNull Map<String, Integer> items) {
        if (!player.isOnline()) {
            return;
        }
        items.forEach((name, amount) -> {
            final Material material = Material.matchMaterial(name.toUpperCase(Locale.ROOT));
            if (material == null || amount <= 0) {
                return;
            }
            final ItemStack stack = itemFactory.create(itemFactory.getFallbackItem(material)).createItemStack();
            stack.setAmount(amount);
            player.getInventory().addItem(stack).values()
                    .forEach(left -> player.getWorld().dropItemNaturally(player.getLocation(), left));
        });
    }
}
