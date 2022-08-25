package me.mykindos.betterpvp.core.framework.delayedactions;

import com.google.inject.Inject;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.gamer.GamerManager;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.framework.delayedactions.events.PlayerDelayedActionEvent;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.time.Duration;
import java.util.WeakHashMap;

@BPvPListener
public class DelayedActionListener implements Listener {

    private final Core core;
    private final GamerManager gamerManager;

    @Inject
    public DelayedActionListener(Core core, GamerManager gamerManager) {
        this.core = core;
        this.gamerManager = gamerManager;
    }

    private final WeakHashMap<Player, DelayedAction> delayedActionMap = new WeakHashMap<>();

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDelayedAction(PlayerDelayedActionEvent event) {
        if (event.isCancelled()) return;

        Player player = event.getPlayer();
        long delay = System.currentTimeMillis() + (long) (event.getDelayInSeconds() * 1000L);

        DelayedAction action = new DelayedAction(event.getRunnable(), delay);
        action.setTitleText(event.getTitleText());
        action.setSubtitleText(event.getSubtitleText());
        action.setCountdown(event.isCountdown());
        action.setCountdownText(event.getCountdownText());

        delayedActionMap.put(player, action);
    }

    @UpdateEvent(delay = 100)
    public void onDelayedActionUpdate() {
        delayedActionMap.entrySet().removeIf(entry -> {

            if (entry.getValue().getTime() <= System.currentTimeMillis()) {
                UtilServer.runTask(core, entry.getValue().getRunnable());
                return true;
            }

            return false;
        });

        delayedActionMap.forEach((player, delayedAction) -> {
            if (delayedAction.isCountdown() && delayedAction.getCountdownText() != null) {

                String remainingTime = String.format(ChatColor.YELLOW + delayedAction.getCountdownText() + ChatColor.GREEN + " %.1f %s",
                        UtilTime.convert((delayedAction.getTime() - System.currentTimeMillis()), UtilTime.TimeUnit.BEST, 1),
                        ChatColor.YELLOW + UtilTime.getTimeUnit2(delayedAction.getTime() - System.currentTimeMillis()).toLowerCase());
                var times = Title.Times.times(Duration.ofMillis(0), Duration.ofMillis(1000), Duration.ofMillis(1000));
                player.showTitle(Title.title(Component.text(remainingTime), Component.empty(), times));
            }

        });
    }

    @EventHandler
    public void onDamage(CustomDamageEvent event) {
        if (event.getDamagee() instanceof Player player) {
            if (delayedActionMap.containsKey(player)) {
                DelayedAction delayedAction = delayedActionMap.remove(player);

                var times = Title.Times.times(Duration.ofMillis(0), Duration.ofMillis(1000), Duration.ofMillis(1000));
                player.showTitle(Title.title(
                        Component.text(ChatColor.RED + delayedAction.getTitleText() + " cancelled"),
                        Component.text(ChatColor.GRAY + "You took damage while " + delayedAction.getSubtitleText()),
                        times));
            }
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (delayedActionMap.containsKey(event.getPlayer())) {
            if (event.getFrom().getBlockX() != event.getTo().getBlockX() || event.getFrom().getBlockY() != event.getTo().getBlockY()
                    || event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {
                DelayedAction delayedAction = delayedActionMap.remove(event.getPlayer());

                var times = Title.Times.times(Duration.ofMillis(0), Duration.ofMillis(1000), Duration.ofMillis(1000));
                event.getPlayer().showTitle(Title.title(
                        Component.text(ChatColor.RED + delayedAction.getTitleText() + " cancelled"),
                        Component.text(ChatColor.GRAY + "You moved while " + delayedAction.getSubtitleText()),
                        times));
            }
        }
    }

    @EventHandler (priority = EventPriority.LOWEST)
    public void onCombatCancel(PlayerDelayedActionEvent event) {
        if (event.isCancelled()) return;

        gamerManager.getObject(event.getPlayer().getUniqueId()).ifPresent(gamer -> {
            if(!UtilTime.elapsed(gamer.getLastDamaged(), 15000)) {
                event.setCancelled(true);
                UtilMessage.message(event.getPlayer(), "Combat", "You cannot do this while in combat!");
            }
        });

    }
}
