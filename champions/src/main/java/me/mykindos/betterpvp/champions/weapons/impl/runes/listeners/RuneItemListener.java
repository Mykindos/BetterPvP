package me.mykindos.betterpvp.champions.weapons.impl.runes.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.weapons.impl.runes.Rune;
import me.mykindos.betterpvp.champions.weapons.impl.runes.RuneNamespacedKeys;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.energy.events.DegenerateEnergyEvent;
import me.mykindos.betterpvp.core.items.ItemHandler;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

@BPvPListener
@Singleton
public class RuneItemListener implements Listener {

    private final ItemHandler itemHandler;

    @Inject
    public RuneItemListener(ItemHandler itemHandler) {
        this.itemHandler = itemHandler;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onConqueringDamage(CustomDamageEvent event) {
        if (!(event.getDamager() instanceof Player damager)) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;

        ItemStack mainHand = damager.getInventory().getItemInMainHand();
        ItemMeta itemMeta = mainHand.getItemMeta();

        Rune rune = getRuneFromNamespacedKey(RuneNamespacedKeys.CONQUERING, itemMeta);
        if (rune == null) return;

        PersistentDataContainer conqueringPdc = itemMeta.getPersistentDataContainer().get(rune.getAppliedNamespacedKey(), PersistentDataType.TAG_CONTAINER);
        event.setDamage(event.getDamage() + rune.getRollFromItem(conqueringPdc).doubleValue());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onHasteSpeed(CustomDamageEvent event) {
        if (!(event.getDamager() instanceof Player damager)) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;

        ItemStack mainHand = damager.getInventory().getItemInMainHand();
        ItemMeta itemMeta = mainHand.getItemMeta();

        Rune rune = getRuneFromNamespacedKey(RuneNamespacedKeys.HASTE, itemMeta);
        if (rune == null) return;

        PersistentDataContainer hastePdc = itemMeta.getPersistentDataContainer().get(rune.getAppliedNamespacedKey(), PersistentDataType.TAG_CONTAINER);
        event.setDamageDelay((long) (event.getDamageDelay() * (1 - rune.getRollFromItem(hastePdc).doubleValue() / 100)));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInsightEnergy(DegenerateEnergyEvent event) {
        Player player = event.getPlayer();
        ItemStack[] armour = player.getInventory().getArmorContents();

        double totalReduction = 0;
        for (ItemStack item : armour) {
            ItemMeta itemMeta = item.getItemMeta();
            Rune rune = getRuneFromNamespacedKey(RuneNamespacedKeys.INSIGHT, itemMeta);
            if (rune == null) continue;

            PersistentDataContainer insightPdc = itemMeta.getPersistentDataContainer().get(rune.getAppliedNamespacedKey(), PersistentDataType.TAG_CONTAINER);
            totalReduction += rune.getRollFromItem(insightPdc).doubleValue();

        }

        event.setEnergy(event.getEnergy() * (1 - totalReduction / 100));

    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDurability(PlayerItemDamageEvent event) {
        ItemMeta itemMeta = event.getItem().getItemMeta();

        Rune rune = getRuneFromNamespacedKey(RuneNamespacedKeys.UNBREAKING, itemMeta);
        if (rune == null) return;

        PersistentDataContainer hastePdc = itemMeta.getPersistentDataContainer().get(rune.getAppliedNamespacedKey(), PersistentDataType.TAG_CONTAINER);
        double ignoreChance = rune.getRollFromItem(hastePdc).doubleValue();

        if (UtilMath.randDouble(0, 100) < ignoreChance) {
            event.setCancelled(true);
        }
    }


    private Rune getRuneFromNamespacedKey(NamespacedKey key, ItemMeta itemMeta) {
        if (itemMeta == null) return null;

        PersistentDataContainer pdc = itemMeta.getPersistentDataContainer();
        PersistentDataContainer runePdc = pdc.get(key, PersistentDataType.TAG_CONTAINER);
        if (runePdc == null) return null;

        String runeName = runePdc.get(RuneNamespacedKeys.OWNING_RUNE, PersistentDataType.STRING);
        if (runeName == null) return null;

        return (Rune) itemHandler.getItem(runeName);
    }
}
