package me.mykindos.betterpvp.core.framework.delayedactions;

import com.google.inject.Inject;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.framework.delayedactions.events.PlayerDelayedActionEvent;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
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
    private final ClientManager clientManager;

    @Inject
    public DelayedActionListener(Core core, ClientManager clientManager) {
        this.core = core;
        this.clientManager = clientManager;
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

    @EventHandler (priority = EventPriority.LOWEST)
    public void onDuplicateDelayedAction(PlayerDelayedActionEvent event) {
        if(delayedActionMap.containsKey(event.getPlayer())) {
            event.cancel("Player already has an active delayedaction");
        }
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

                Component remainingTime = UtilMessage.deserialize("<alt2>%s</alt2> <alt>%.1f</alt> <alt2>%s</alt2>",
                        delayedAction.getCountdownText(),
                        UtilTime.convert((delayedAction.getTime() - System.currentTimeMillis()), UtilTime.TimeUnit.BEST, 1),
                        UtilTime.getTimeUnit2(delayedAction.getTime() - System.currentTimeMillis()).toLowerCase()
                );
                
                var times = Title.Times.times(Duration.ofMillis(0), Duration.ofMillis(1000), Duration.ofMillis(1000));
                player.showTitle(Title.title(remainingTime, Component.empty(), times));
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
                        Component.text(delayedAction.getTitleText() + " cancelled", NamedTextColor.RED),
                        Component.text("You took damage while " + delayedAction.getSubtitleText(), NamedTextColor.GRAY),
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
                        Component.text(delayedAction.getTitleText() + " cancelled", NamedTextColor.RED),
                        Component.text("You moved while " + delayedAction.getSubtitleText(), NamedTextColor.GRAY),
                        times));
            }
        }
    }

    @EventHandler (priority = EventPriority.LOWEST)
    public void onCombatCancel(PlayerDelayedActionEvent event) {
        if (event.isCancelled()) return;

        final Gamer gamer = clientManager.search().online(event.getPlayer()).getGamer();
        if(!UtilTime.elapsed(gamer.getLastDamaged(), 15000)) {
            event.setCancelled(true);
            UtilMessage.message(event.getPlayer(), "Combat", "You cannot do this while in combat!");
        }
    }
}
