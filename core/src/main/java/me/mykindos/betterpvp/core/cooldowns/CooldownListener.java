package me.mykindos.betterpvp.core.cooldowns;

import com.google.inject.Inject;
import me.mykindos.betterpvp.core.cooldowns.events.CooldownDisplayEvent;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.gamer.Gamer;
import me.mykindos.betterpvp.core.gamer.GamerManager;
import me.mykindos.betterpvp.core.gamer.properties.GamerProperty;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;


@BPvPListener
public class CooldownListener implements Listener {

    private final CooldownManager cooldownManager;
    private final GamerManager gamerManager;

    @Inject
    public CooldownListener(CooldownManager cooldownManager, GamerManager gamerManager) {
        this.cooldownManager = cooldownManager;
        this.gamerManager = gamerManager;
    }

    @UpdateEvent(delay = 100, isAsync = true)
    public void processCooldowns() {
        cooldownManager.processCooldowns();
    }

    @UpdateEvent(delay = 100)
    public void displayActionBar() {
        for (Player player : Bukkit.getOnlinePlayers()) {

            Gamer gamer = gamerManager.getObject(player.getUniqueId()).orElse(null);
            if (gamer == null) continue;

            boolean cooldownBarEnabled = (boolean) gamer.getProperty(GamerProperty.COOLDOWN_DISPLAY).orElse(true);
            if (!cooldownBarEnabled) continue;

            UtilServer.callEvent(new CooldownDisplayEvent(player));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void processCooldownDisplay(CooldownDisplayEvent event) {
        if (event.isCancelled()) return;
        if (event.getCooldownName().equals("")) return;

        Cooldown cooldown = cooldownManager.getAbilityRecharge(event.getPlayer(), event.getCooldownName());
        if (cooldown != null) {
            int red = (int) Math.ceil(((cooldown.getRemaining() / (cooldown.getSeconds() / 1000)) * 100 / 10));
            int green = 10 - red;

            if (cooldown.getRemaining() < 0.15) {
                green = 10;
            }

            String msg = "";

            for (int i = 0; i < green; i++) {
                msg += ChatColor.GREEN.toString() + ChatColor.BOLD + "\u2588";
            }

            if (green != 10) {

                for (int i = 0; i < red; i++) {
                    msg += ChatColor.RED.toString() + ChatColor.BOLD + "\u2588";
                }
            }

            UtilPlayer.sendActionBar(event.getPlayer(), ChatColor.GOLD.toString() + ChatColor.BOLD + event.getCooldownName() + " "
                    + ChatColor.YELLOW + ChatColor.BOLD + "[" + msg + ChatColor.YELLOW + ChatColor.BOLD + "]");
        }
    }
}
