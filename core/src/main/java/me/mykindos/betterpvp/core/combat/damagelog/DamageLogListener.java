package me.mykindos.betterpvp.core.combat.damagelog;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.combat.events.KillContributionEvent;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.concurrent.ConcurrentLinkedDeque;

@Singleton
@BPvPListener
public class DamageLogListener implements Listener {

    @Inject
    @Config(path = "pvp.showKillerHealth", defaultValue = "true")
    private boolean showKillerHealth;

    private final DamageLogManager damageLogManager;

    @Inject
    public DamageLogListener(DamageLogManager damageLogManager) {
        this.damageLogManager = damageLogManager;
    }

    @UpdateEvent(delay = 100, isAsync = true)
    public void processDamageLogs() {
        damageLogManager.getObjects().forEach((uuid, logs) -> {
            logs.removeIf(log -> log.getExpiry() - System.currentTimeMillis() <= 0);
        });

        damageLogManager.getObjects().entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(KillContributionEvent event) {
        if (!showKillerHealth) return;
        final long deathTime = System.currentTimeMillis();
        final ConcurrentLinkedDeque<DamageLog> log = new ConcurrentLinkedDeque<>(damageLogManager.getObject(event.getKiller().getUniqueId())
                .orElse(new ConcurrentLinkedDeque<>()));
        final ClickEvent clickEvent = ClickEvent.callback(
                audience -> damageLogManager.showDamageSummary(deathTime, event.getKiller(), (Player) audience, log),
                ClickCallback.Options.builder().uses(1).build()
        );

        final Component component = Component.text("Click")
                .appendSpace()
                .append(Component.text("here").color(NamedTextColor.WHITE))
                .appendSpace()
                .append(Component.text("to view your killers damage summary."))
                .clickEvent(clickEvent);
        UtilMessage.simpleMessage(event.getVictim(), "Death", component);
        UtilMessage.message(event.getVictim(), "Death", "<yellow>%s</yellow> has <red>%s</red> health remaining.", event.getKiller().getName(), event.getKiller().getHealth());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        final long deathTime = System.currentTimeMillis();
        final ConcurrentLinkedDeque<DamageLog> log = new ConcurrentLinkedDeque<>(damageLogManager.getObject(event.getPlayer().getUniqueId())
                .orElse(new ConcurrentLinkedDeque<>()));
        final ClickEvent clickEvent = ClickEvent.callback(
                audience -> damageLogManager.showDamageSummary(deathTime, event.getPlayer(), (Player) audience, log),
                ClickCallback.Options.builder().uses(1).build()
        );

        final Component component = Component.text("Click")
                .appendSpace()
                .append(Component.text("here").color(NamedTextColor.WHITE))
                .appendSpace()
                .append(Component.text("to view your death summary."))
                .clickEvent(clickEvent);
        final Component hover = Component.text("What killed you?");
        UtilMessage.simpleMessage(event.getPlayer(), "Death", component, hover);

        // Clear the damage logs for this player
        damageLogManager.getObjects().remove(event.getPlayer().getUniqueId().toString());
    }
}
