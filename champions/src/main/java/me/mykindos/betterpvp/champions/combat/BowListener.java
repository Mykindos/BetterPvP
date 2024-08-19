package me.mykindos.betterpvp.champions.combat;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CrossbowMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.UUID;

@Singleton
@BPvPListener
public class BowListener implements Listener {

    private HashMap<UUID, Long> crossbowTracker = new HashMap<>();

    @Inject
    @Config(path = "combat.crossbow.cooldownEnabled", defaultValue = "true")
    private boolean crossbowCooldownEnabled;

    @Inject
    @Config(path = "combat.crossbow.cooldownDuration", defaultValue = "1.5")
    private double crossbowCooldownDuration;

    private final Champions champions;

    @Inject
    public BowListener(Champions champions) {
        this.champions = champions;
    }

    @EventHandler
    public void onShootCrossbow(EntityShootBowEvent event) {
        if (!crossbowCooldownEnabled) return;
        if (event.isCancelled()) return;
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.getBow() == null || event.getBow().getType() != Material.CROSSBOW) return;

        if (crossbowTracker.containsKey(player.getUniqueId())) {
            long lastShot = crossbowTracker.get(player.getUniqueId());
            if (!UtilTime.elapsed(lastShot, (long) (crossbowCooldownDuration * 1000))) {
                event.setCancelled(true);
                UtilMessage.simpleMessage(player, "Combat", "You can only shoot a crossbow once every <green>%.1f <gray>seconds.", crossbowCooldownDuration);

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (player.getInventory().getItemInMainHand().getType() == Material.CROSSBOW) {
                            CrossbowMeta meta = (CrossbowMeta) player.getInventory().getItemInMainHand().getItemMeta();
                            meta.addChargedProjectile(new ItemStack(Material.ARROW, 1));

                            player.getInventory().getItemInMainHand().setItemMeta(meta);
                        }
                    }
                }.runTaskLater(champions, 2);

            } else {
                crossbowTracker.put(player.getUniqueId(), System.currentTimeMillis());
            }
        } else {
            crossbowTracker.put(player.getUniqueId(), System.currentTimeMillis());
        }
    }

    @EventHandler
    public void onLogout(PlayerQuitEvent event) {
        crossbowTracker.remove(event.getPlayer().getUniqueId());
    }


}
