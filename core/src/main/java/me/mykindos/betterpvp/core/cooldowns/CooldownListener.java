package me.mykindos.betterpvp.core.cooldowns;

import com.google.inject.Inject;
import me.mykindos.betterpvp.core.combat.death.events.CustomDeathEvent;
import me.mykindos.betterpvp.core.cooldowns.events.CooldownDisplayEvent;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.gamer.Gamer;
import me.mykindos.betterpvp.core.gamer.GamerManager;
import me.mykindos.betterpvp.core.gamer.properties.GamerProperty;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
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

    @EventHandler
    public void onDeath(CustomDeathEvent event) {
        if (!(event.getKilled() instanceof Player player)) return;
        cooldownManager.getObject(player.getUniqueId()).ifPresent(cooldowns -> {
            cooldowns.entrySet().removeIf(cooldown -> cooldown.getValue().isRemoveOnDeath());
        });
    }

    @UpdateEvent(delay = 50)
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
        if (event.isCancelled() || event.getCooldownName().equals("")) {
            return;
        }

        Cooldown cooldown = cooldownManager.getAbilityRecharge(event.getPlayer(), event.getCooldownName());
        if (cooldown != null) {
            int red = (int) Math.ceil(((cooldown.getRemaining() / (cooldown.getSeconds() / 1000)) * 100 / 10));
            int green = 10 - red;

            if (cooldown.getRemaining() < 0.1) {
                green = 10;
            }

            String msg = "<green><bold>" + "\u2588".repeat(Math.max(0, green));
            if (green != 10) {
                msg += "<red><bold>" + "\u2588".repeat(Math.max(0, red));
            }

            final Component bar = MiniMessage.miniMessage().deserialize(msg);
            final Component actionBar = MiniMessage.miniMessage().deserialize("<gold><bold>" + event.getCooldownName() + " <yellow><bold>[<bar>]", Placeholder.component("bar", bar));
            UtilPlayer.sendActionBar(event.getPlayer(), actionBar);
        }
    }
}
