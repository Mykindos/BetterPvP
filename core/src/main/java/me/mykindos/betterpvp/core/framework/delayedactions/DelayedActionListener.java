package me.mykindos.betterpvp.core.framework.delayedactions;

import com.google.inject.Inject;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.framework.delayedactions.events.PlayerDelayedActionEvent;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.model.display.TitleComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

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

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDuplicateDelayedAction(PlayerDelayedActionEvent event) {
        if (delayedActionMap.containsKey(event.getPlayer())) {
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
                        UtilTime.getTimeUnit2((double) delayedAction.getTime() - System.currentTimeMillis()).toLowerCase()
                );

                final TitleComponent titleComponent = TitleComponent.title(0, 0.5, 0, false, gamer -> remainingTime);
                clientManager.search().online(player).getGamer().getTitleQueue().add(10, titleComponent);
            }

        });
    }

    @EventHandler
    public void onDamage(CustomDamageEvent event) {
        if (event.getDamagee() instanceof Player player) {
            if (delayedActionMap.containsKey(player)) {
                DelayedAction delayedAction = delayedActionMap.remove(player);

                TitleComponent titleComponent = new TitleComponent(0, 1, 1, true,
                        gamer -> Component.text(delayedAction.getTitleText() + " cancelled", NamedTextColor.RED),
                        gamer -> Component.text("You took damage while " + delayedAction.getSubtitleText(), NamedTextColor.GRAY));
                clientManager.search().online(player).getGamer().getTitleQueue().add(9, titleComponent);
            }
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (delayedActionMap.containsKey(event.getPlayer())) {
            if (event.hasChangedBlock()) {
                DelayedAction delayedAction = delayedActionMap.remove(event.getPlayer());

                TitleComponent titleComponent = new TitleComponent(0, 1, 1, true,
                        gamer -> Component.text(delayedAction.getTitleText() + " cancelled", NamedTextColor.RED),
                        gamer -> Component.text("You moved while " + delayedAction.getSubtitleText(), NamedTextColor.GRAY));
                clientManager.search().online(event.getPlayer()).getGamer().getTitleQueue().add(9, titleComponent);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCombatCancel(PlayerDelayedActionEvent event) {
        if (event.isCancelled()) return;
        if (UtilPlayer.isCreativeOrSpectator(event.getPlayer())) return;

        final Client client = clientManager.search().online(event.getPlayer());
        if (client.isAdministrating()) return;

        Gamer gamer = client.getGamer();

        if (gamer.isInCombat()) {
            event.setCancelled(true);
            UtilMessage.message(event.getPlayer(), "Combat", "You cannot do this while in combat!");
        }
    }
}
