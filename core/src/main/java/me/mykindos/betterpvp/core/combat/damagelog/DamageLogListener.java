package me.mykindos.betterpvp.core.combat.damagelog;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.combat.events.KillContributionEvent;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.ref.WeakReference;
import java.time.Duration;
import java.util.concurrent.ConcurrentLinkedDeque;

@Singleton
@BPvPListener
public class DamageLogListener implements Listener {

    @Inject
    @Config(path = "pvp.showKillerHealth", defaultValue = "false")
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
        if (!showKillerHealth) {
            return;
        }

        final long deathTime = System.currentTimeMillis();
        final ConcurrentLinkedDeque<DamageLog> log = new ConcurrentLinkedDeque<>(damageLogManager.getObject(event.getKiller().getUniqueId().toString())
                .orElse(new ConcurrentLinkedDeque<>()));
        final WeakReference<Player> killerRef = new WeakReference<>(event.getKiller());
        final ClickEvent clickEvent = ClickEvent.callback(
                audience -> {
                    Player killer = killerRef.get();
                    if (killer != null) {
                        damageLogManager.showDamageSummary(deathTime, killer, (Player) audience, log);
                    }
                },
                ClickCallback.Options.builder().lifetime(Duration.ofMinutes(2)).uses(1).build()
        );

        final Component component = Component.empty()
                .append(Component.text("Click to"))
                .appendSpace()
                .append(Component.text("view").color(NamedTextColor.WHITE))
                .appendSpace()
                .append(Component.text("why."))
                .clickEvent(clickEvent);
        final Component message = UtilMessage.getMiniMessage("<alt2>%s</alt2> has <red>%.1f\u2764</red> remaining.", event.getKiller().getName(), event.getKiller().getHealth());
        UtilMessage.simpleMessage(event.getVictim(), "Death", message.appendSpace().append(component));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        final long deathTime = System.currentTimeMillis();
        final ConcurrentLinkedDeque<DamageLog> log = new ConcurrentLinkedDeque<>(damageLogManager.getObject(event.getPlayer().getUniqueId().toString())
                .orElse(new ConcurrentLinkedDeque<>()));
        final WeakReference<Player> playerRef = new WeakReference<>(event.getPlayer());
        final ClickEvent clickEvent = ClickEvent.callback(
                audience -> {
                    Player player = playerRef.get();
                    if (player != null) {
                        damageLogManager.showDamageSummary(deathTime, player, (Player) audience, log);
                    }
                },
                ClickCallback.Options.builder().lifetime(Duration.ofMinutes(2)).uses(1).build()
        );


        final Component component = Component.text("Click")
                .appendSpace()
                .append(Component.text("here").color(NamedTextColor.WHITE))
                .appendSpace()
                .append(Component.text("to view your death summary."))
                .clickEvent(clickEvent);
        final Component hover = Component.text("What killed you?");
        UtilMessage.simpleMessage(event.getPlayer(), "Death", component, hover);

        // Clear the damage logs for this player after this death
        UtilServer.runTaskLater(JavaPlugin.getPlugin(Core.class), () -> {
            damageLogManager.getObjects().remove(event.getPlayer().getUniqueId().toString());
        }, 1L);
    }
}
