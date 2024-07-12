package me.mykindos.betterpvp.champions.weapons.impl.runes.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.weapons.impl.runes.Rune;
import me.mykindos.betterpvp.champions.weapons.impl.runes.RuneNamespacedKeys;
import me.mykindos.betterpvp.core.framework.customtypes.KeyValue;
import me.mykindos.betterpvp.core.framework.events.items.ItemUpdateLoreEvent;
import me.mykindos.betterpvp.core.framework.events.items.ItemUpdateNameEvent;
import me.mykindos.betterpvp.core.items.BPvPItem;
import me.mykindos.betterpvp.core.items.ItemHandler;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Singleton
@BPvPListener
@CustomLog
public class RuneCraftingListener implements Listener {

    private final Champions champions;
    private final ItemHandler itemHandler;

    @Inject
    public RuneCraftingListener(Champions champions, ItemHandler itemHandler) {
        this.champions = champions;
        this.itemHandler = itemHandler;
    }


    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {

        var firstItem = event.getInventory().getFirstItem();
        if (firstItem == null) return;

        var secondItem = event.getInventory().getSecondItem();
        if (secondItem == null) return;

        BPvPItem item = itemHandler.getItem(secondItem);
        if(!(item instanceof Rune rune)) return;

        if(!rune.itemMatchesFilter(firstItem.getType())) return;
        if(!rune.canApplyToItem(secondItem.getItemMeta(), firstItem.getItemMeta())) return;

        ItemStack result = firstItem.clone();
        ItemMeta meta = result.getItemMeta();

        rune.applyToItem(secondItem.getItemMeta(), meta);

        result.setItemMeta(meta);

        event.setResult(itemHandler.updateNames(result));
        // Don't ask why, it's just required for some stupid reason
        UtilServer.runTaskLater(champions, () -> event.getInventory().setRepairCost(0), 1);

    }

    @EventHandler (priority = EventPriority.HIGHEST)
    public void onLoreUpdate(ItemUpdateLoreEvent event) {

        ItemMeta meta = event.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        List<KeyValue<Rune, PersistentDataContainer>> runes = new ArrayList<>();
        boolean hasAddedImbuementSection = false;
        for(NamespacedKey key : pdc.getKeys()) {
            if(!pdc.has(key, PersistentDataType.TAG_CONTAINER)) continue;
            PersistentDataContainer runePdc = pdc.get(key, PersistentDataType.TAG_CONTAINER);
            if(runePdc == null) continue;

            String owningRune = runePdc.get(RuneNamespacedKeys.OWNING_RUNE, PersistentDataType.STRING);
            if(owningRune == null) continue;

            BPvPItem item = itemHandler.getItem(owningRune);
            if(!(item instanceof Rune rune)) continue;

            if(!hasAddedImbuementSection) {
                event.getItemLore().add(Component.empty());
                event.getItemLore().add(UtilMessage.getMiniMessage("<blue>Imbuements"));
                hasAddedImbuementSection = true;
            }

            runes.add(new KeyValue<>(rune, runePdc));

        }

        runes.sort(Comparator.comparingInt(o -> o.getKey().getTier()));

        for(KeyValue<Rune, PersistentDataContainer> pair : runes) {
            event.getItemLore().addAll(pair.getKey().getItemLoreDescription(pair.getValue(), event.getItemStack()));
        }
    }

    @EventHandler
    public void onNameUpdate(ItemUpdateNameEvent event) {

        PersistentDataContainer persistentDataContainer = event.getItemMeta().getPersistentDataContainer();
        if(persistentDataContainer.has(RuneNamespacedKeys.HAS_RUNE, PersistentDataType.BOOLEAN)){
            event.setItemName(
                    Component.text("Imbued ").color(NamedTextColor.AQUA)
                            .append(event.getItemName().color(NamedTextColor.AQUA))
                            .decoration(TextDecoration.ITALIC, false)
            );
        }

    }
}
