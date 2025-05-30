package me.mykindos.betterpvp.core.combat.death;

import com.google.inject.Inject;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.combat.damagelog.DamageLog;
import me.mykindos.betterpvp.core.combat.damagelog.DamageLogManager;
import me.mykindos.betterpvp.core.combat.death.events.CustomDeathEvent;
import me.mykindos.betterpvp.core.combat.death.events.CustomDeathMessageEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
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

        final CustomDeathEvent deathEvent = new CustomDeathEvent(event.getPlayer());
        if (lastDamage != null) {
            deathEvent.setKiller(lastDamage.getDamager());
            deathEvent.setReason(lastDamage.getReason());
        }
        deathEvent.callEvent();

        Bukkit.getOnlinePlayers().forEach(onlinePlayer -> {
            CustomDeathMessageEvent customDeathMessageEvent = new CustomDeathMessageEvent(onlinePlayer, deathEvent);
            UtilServer.callEvent(customDeathMessageEvent);
        });
    }



    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        final Client client = clientManager.search().online(event.getPlayer());
        final Gamer gamer = client.getGamer();
        gamer.setLastDamaged(0);
        gamer.setLastDeath(System.currentTimeMillis());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCustomDeath(CustomDeathMessageEvent event) {
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
            if (killer instanceof Player) {
                ItemStack item = killer.getEquipment().getItemInMainHand();
                if (reasons.length == 0 && item.getType() != Material.AIR) {
                    TextColor color = NamedTextColor.GREEN;

                    if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                        Component displayName = item.getItemMeta().displayName();
                        if (displayName != null && displayName.color() != null) {
                            color = displayName.color();
                        }
                    }

                    if (color == NamedTextColor.YELLOW) {
                        color = NamedTextColor.GREEN;
                    }

                    String namePlainText = PlainTextComponentSerializer.plainText().serialize(item.displayName()).replaceAll("[\\[\\]]", "");
                    Component itemCmpt = Component.text(namePlainText, color);
                    reason = Component.text(UtilFormat.getIndefiniteArticle(namePlainText) + " ", NamedTextColor.GRAY).append(itemCmpt.hoverEvent(item));
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
                                        .append(killerName.applyFallbackStyle(NamedTextColor.YELLOW)));
                    } else {
                        message = killedName
                                .append(Component.text(" was killed by ", NamedTextColor.GRAY))
                                .append(customName.color(NamedTextColor.YELLOW));
                    }
                } else {
                    message = killedName
                            .append(Component.text(" was killed by ", NamedTextColor.GRAY))
                            .append(killerName);
                }
            } else {
                message = killedName.append(Component.text(" was killed by ", NamedTextColor.GRAY))
                        .append(killerName.applyFallbackStyle(NamedTextColor.YELLOW));

                if (killer instanceof Player) {
                    message = message.append(Component.text(" with ", NamedTextColor.GRAY))
                            .append(reason);
                }
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
