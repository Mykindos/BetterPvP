package me.mykindos.betterpvp.core.combat.death;

import com.google.inject.Inject;
import me.mykindos.betterpvp.core.combat.death.events.CustomDeathEvent;
import me.mykindos.betterpvp.core.combat.damagelog.DamageLog;
import me.mykindos.betterpvp.core.combat.damagelog.DamageLogManager;
import me.mykindos.betterpvp.core.gamer.GamerManager;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import net.kyori.adventure.text.Component;
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

@BPvPListener
public class DeathListener implements Listener {

    private final DamageLogManager damageLogManager;
    private final GamerManager gamerManager;

    @Inject
    public DeathListener(DamageLogManager damageLogManager, GamerManager gamerManager) {
        this.damageLogManager = damageLogManager;
        this.gamerManager = gamerManager;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        event.deathMessage(null);
        DamageLog lastDamage = damageLogManager.getLastDamager(event.getPlayer());

        gamerManager.getObject(event.getEntity().getUniqueId()).ifPresent(gamer -> {
            gamer.setLastDamaged(0);
        });

        Bukkit.getOnlinePlayers().forEach(onlinePlayer -> {
            CustomDeathEvent customDeathEvent = new CustomDeathEvent(onlinePlayer, event.getPlayer());
            if (lastDamage != null) {
                customDeathEvent.setKiller(lastDamage.getDamager());
                customDeathEvent.setReason(lastDamage.getReason());
            }
            UtilServer.callEvent(customDeathEvent);
        });

    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCustomDeath(CustomDeathEvent event) {
        if (event.getKiller() == null) {
            if (event.getReason() == null || event.getReason().equals("")) {
                event.setCustomDeathMessage(String.format("<yellow>%s<gray> was killed.", event.getKilled().getName()));
            } else {
                event.setCustomDeathMessage(String.format("<yellow>%s<gray> was killed by <yellow>%s<gray>.",
                        event.getKilled().getName(), event.getReason()));
            }
        } else {

            LivingEntity killer = event.getKiller();
            if (killer.getEquipment() != null) {
                ItemStack item = killer.getEquipment().getItemInMainHand();
                if (event.getReason().equals("") && item.getType() != Material.AIR) {
                    event.setReason("<gray>a <green>" + PlainTextComponentSerializer.plainText()
                            .serialize(item.displayName()).replaceAll("[\\[\\]]", ""));
                }
            }

            if (event.getReason().equals("")) {
                if (!(event.getKiller() instanceof Player)) {
                    if (event.getKiller().customName() == null) {
                        // Better english if the killer doesnt have a custom name
                        event.setCustomDeathMessage(String.format("<yellow>%s<gray> was killed by a <yellow>%s<gray>.",
                                event.getKilled().getName(), event.getKiller().getName()));
                    } else {
                        event.setCustomDeathMessage(String.format("<yellow>%s<gray> was killed by <yellow>%s<gray>.",
                                event.getKilled().getName(), event.getKiller().getName()));
                    }
                } else {
                    event.setCustomDeathMessage(String.format("<yellow>%s<gray> was killed by <yellow>%s<gray>.",
                            event.getKilled().getName(), event.getKiller().getName()));
                }
            } else {
                event.setCustomDeathMessage(String.format("<yellow>%s<gray> was killed by <yellow>%s<gray> with <green>%s<gray>.",
                        event.getKilled().getName(), event.getKiller().getName(), event.getReason()));
            }

        }

    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void finishCustomDeath(CustomDeathEvent event) {
        if (event.isCancelled()) return;

        Component hoverComponent = Component.text("Damage Breakdown", NamedTextColor.GOLD).appendNewline();
        for (var breakdown : damageLogManager.getDamageBreakdown(event.getKilled())) {
            hoverComponent = hoverComponent.append(Component.text(breakdown.getKey() + ": ", NamedTextColor.YELLOW)
                    .append(Component.text(UtilMath.round(breakdown.getValue(), 1), NamedTextColor.GREEN))).appendNewline();
        }

        UtilMessage.simpleMessage(event.getReceiver(), "Death", UtilFormat.stripColor(event.getCustomDeathMessage()), hoverComponent);
    }
}
