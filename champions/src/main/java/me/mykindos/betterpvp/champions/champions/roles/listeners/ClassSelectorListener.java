package me.mykindos.betterpvp.champions.champions.roles.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.commands.menu.KitMenu;
import me.mykindos.betterpvp.champions.champions.roles.RoleManager;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockReceiveGameEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

@BPvPListener
@Singleton
public class ClassSelectorListener implements Listener {

    private final Champions plugin;
    private final RoleManager roleManager;
    private final ItemFactory itemFactory;

    @Inject
    private ClassSelectorListener(Champions plugin, RoleManager roleManager, ItemFactory itemFactory) {
        this.plugin = plugin;
        this.roleManager = roleManager;
        this.itemFactory = itemFactory;
    }

    @EventHandler
    void onGame(BlockReceiveGameEvent event) {
        if (event.getBlock().getType().equals(Material.SCULK_SHRIEKER)) {
            // Prevent shrieking sound and activation
            event.setCancelled(true);
        }
    }

    @EventHandler
    void onInteract(PlayerInteractEvent event) {
        if (event.getAction().equals(Action.PHYSICAL)
                && event.hasBlock()
                && Objects.requireNonNull(event.getClickedBlock()).getType().equals(Material.SCULK_SHRIEKER)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    void onEntityInteract(EntityInteractEvent event) {
        if (event.getBlock().getType().equals(Material.SCULK_SHRIEKER)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    void onMove(PlayerMoveEvent event) {
        if (!event.hasChangedBlock()) {
            return; // They haven't moved yet
        }

        final Block blockUnder = event.getTo().getBlock();
        if (blockUnder.getType().equals(Material.SCULK_SHRIEKER)) {
            // At this point, they're on top of the class selector, so trigger
            triggerClassSelector(event.getPlayer(), blockUnder);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    void onBreak(BlockBreakEvent event) {
        if (event.getBlock().getType().equals(Material.SCULK_SHRIEKER)) {
            event.setDropItems(false);
            final Location location = event.getBlock().getLocation().toCenterLocation();
            final ItemStack item = Objects.requireNonNull(event.getBlock().getType().asItemType()).createItemStack();
            location.getWorld().dropItemNaturally(location, item);
        }
    }

    private void triggerClassSelector(@NotNull Player player, Block blockUnder) {
        new SoundEffect("emaginationfallenheroes", "custom.spell.soulfirecast", 2f, 1).play(blockUnder.getLocation());
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks > 10) {
                    this.cancel();
                    return;
                }

                final Location topCenter = blockUnder.getLocation().toCenterLocation().add(0, 0.5, 0);
                final Location abovePlayer = topCenter.clone().add(0, 2.0, 0);
                final Color color = Color.fromRGB((int) (49 + Math.random() * 30), (int) (168 - Math.random() * 100), 157);
                Particle.TRAIL.builder()
                        .data(new Particle.Trail(abovePlayer, color, 15))
                        .location(topCenter)
                        .offset(0.3, 0, 0.3)
                        .receivers(60)
                        .extra(0.5)
                        .count(5)
                        .spawn();

                if (ticks % 2 == 0) {
                    Particle.SCULK_CHARGE.builder()
                            .data(0f)
                            .location(topCenter)
                            .offset(0.25, 0.1, 0.25)
                            .receivers(60)
                            .count(1)
                            .extra(0)
                            .spawn();
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
        new KitMenu(roleManager, itemFactory, false).show(player);
    }

}
