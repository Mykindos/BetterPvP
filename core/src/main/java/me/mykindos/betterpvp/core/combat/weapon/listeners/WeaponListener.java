package me.mykindos.betterpvp.core.combat.weapon.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.combat.combatlog.events.PlayerCombatLogEvent;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.energy.EnergyService;
import me.mykindos.betterpvp.core.framework.events.items.SpecialItemLootEvent;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilSound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

@Singleton
@BPvPListener
@CustomLog
public class WeaponListener implements Listener {

    private final ItemFactory itemFactory;
    private final CooldownManager cooldownManager;
    private final EnergyService energyService;

    @Inject
    public WeaponListener(ItemFactory itemFactory, CooldownManager cooldownManager, EnergyService energyService) {
        this.itemFactory = itemFactory;
        this.cooldownManager = cooldownManager;
        this.energyService = energyService;
    }

    @EventHandler
    public void onSpecialItemDrop(SpecialItemLootEvent event) {
        final ItemInstance item = event.getItemInstance();
        if (!item.getRarity().isImportant()) {
            return;
        }

        final String clazzName = item.getBaseItem().getClass().getSimpleName();
        final Component name = item.getView().getName();
        if (event.getSource().equalsIgnoreCase("Fishing")) {
            UtilMessage.broadcast(Component.text("A ", NamedTextColor.YELLOW).append(name.hoverEvent(item.getView().get()))
                    .append(Component.text(" was caught by a fisherman!", NamedTextColor.YELLOW)));
            log.info("A legendary weapon was caught by a fisherman! ({})", clazzName)
                    .setAction("FISH_LEGENDARY")
                    .addLocationContext(event.getLootContext().getLocation())
                    .addContext("Source", event.getSource()).submit();

            for (Player player : Bukkit.getOnlinePlayers()) {
                UtilSound.playSound(player, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 1.0f, 1.0f, false);
            }
        } else {
            UtilMessage.broadcast(Component.text("Announcement> ", NamedTextColor.BLUE)
                    .append(Component.text(event.getSource(), NamedTextColor.RED)
                            .append(Component.text(" dropped a legendary ", NamedTextColor.GRAY))
                            .append(name.hoverEvent(item.getView().get()))));
            log.info("A legendary weapon was dropped by {}! ({})", event.getSource(), clazzName)
                    .addLocationContext(event.getLootContext().getLocation())
                    .addContext("Source", event.getSource()).submit();
        }
    }

    @EventHandler
    public void onCombatLog(PlayerCombatLogEvent event) {
        for (ItemStack itemStack : event.getPlayer().getInventory().getContents()) {
            if (itemStack == null || itemStack.getType() == Material.AIR) continue;

            itemFactory.fromItemStack(itemStack).ifPresent(item -> {
                if (!item.getRarity().isImportant()) {
                    return;
                }

                event.setSafe(false);
                event.setDuration(System.currentTimeMillis()); // Permanent combat log
            });
        }
    }
}
