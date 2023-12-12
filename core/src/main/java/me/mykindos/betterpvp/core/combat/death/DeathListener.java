package me.mykindos.betterpvp.core.combat.death;

import com.google.inject.Inject;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.combat.damagelog.DamageLog;
import me.mykindos.betterpvp.core.combat.damagelog.DamageLogManager;
import me.mykindos.betterpvp.core.combat.death.events.CustomDeathEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.Objects;

@BPvPListener
public class DeathListener implements Listener {

    private final DamageLogManager damageLogManager;
    private final ClientManager clientManager;

    @Inject
    public DeathListener(DamageLogManager damageLogManager, ClientManager clientManager) {
        this.damageLogManager = damageLogManager;
        this.clientManager = clientManager;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        event.deathMessage(null);
        DamageLog lastDamage = damageLogManager.getLastDamager(event.getPlayer());

        clientManager.search().online(event.getEntity()).getGamer().setLastDamaged(0);

        Bukkit.getOnlinePlayers().forEach(onlinePlayer -> {
            CustomDeathEvent customDeathEvent = new CustomDeathEvent(onlinePlayer, event.getPlayer());
            if (lastDamage != null) {
                customDeathEvent.setKiller(lastDamage.getDamager());
                customDeathEvent.setReason(lastDamage.getReason());
            }
            UtilServer.callEvent(customDeathEvent);
        });

    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCustomDeath(CustomDeathEvent event) {
        final String[] reasonRaw = Objects.requireNonNullElse(event.getReason(), new String[]{});
        final Component[] reasons = Arrays.stream(reasonRaw).map(text -> Component.text(text, NamedTextColor.GREEN)).toArray(Component[]::new);
        Component reason = Component.join(JoinConfiguration.separator(Component.text(", ", NamedTextColor.GRAY)), reasons).applyFallbackStyle(NamedTextColor.GRAY);
        Component message;
        final Component killedName = event.getKilledName().applyFallbackStyle(NamedTextColor.YELLOW);
        if (event.getKiller() == null) {
            if (reasons.length == 0) {
                message = killedName.append(Component.text(" was killed", NamedTextColor.GRAY));
            } else {
                message = killedName.append(Component.text(" was killed by ", NamedTextColor.GRAY)).append(reason);
            }
        } else {
            final Component killerName = event.getKillerName().applyFallbackStyle(NamedTextColor.YELLOW);
            LivingEntity killer = event.getKiller();
            boolean with = false;
            if (killer.getEquipment() != null) {
                ItemStack item = killer.getEquipment().getItemInMainHand();
                if (reasons.length == 0 && item.getType() != Material.AIR) {
                    Component itemCmpt = Component.text(PlainTextComponentSerializer.plainText().serialize(item.displayName()).replaceAll("[\\[\\]]", ""), NamedTextColor.GREEN);
                    reason = Component.text("a ", NamedTextColor.GRAY).append(itemCmpt.hoverEvent(item));
                    with = true;
                }
            }

            if (reasons.length == 0 && !with) {
                if (!(event.getKiller() instanceof Player)) {
                    final Component customName = event.getKiller().customName();
                    if (customName == null) {
                        // Better english if the killer doesn't have a custom name
                        message = killedName
                                .append(Component.text(" was killed by a ", NamedTextColor.GRAY)
                                .append(killerName));
                    } else {
                        message = killedName
                                .append(Component.text(" was killed by ", NamedTextColor.GRAY))
                                .append(customName.applyFallbackStyle(NamedTextColor.YELLOW));
                    }
                } else {
                    message = killedName
                            .append(Component.text(" was killed by ", NamedTextColor.GRAY))
                            .append(killerName);
                }
            } else {
                message = killedName.append(Component.text(" was killed by ", NamedTextColor.GRAY))
                        .append(killerName)
                        .append(Component.text(" with ", NamedTextColor.GRAY))
                        .append(reason);
            }
        }

        message = Component.text("").applyFallbackStyle(NamedTextColor.GRAY).append(message).append(Component.text("."));
        Component hoverComponent = Component.text("Damage Breakdown", NamedTextColor.GOLD).appendNewline();
        for (var breakdown : damageLogManager.getDamageBreakdown(event.getKilled())) {
            hoverComponent = hoverComponent.append(Component.text(breakdown.getKey() + ": ", NamedTextColor.YELLOW)
                    .append(Component.text(UtilMath.round(breakdown.getValue(), 1), NamedTextColor.GREEN))).appendNewline();
        }

        UtilMessage.simpleMessage(event.getReceiver(), "Death", message, hoverComponent);
    }
}
