package me.mykindos.betterpvp.core.items;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.framework.CoreNamespaceKeys;
import me.mykindos.betterpvp.core.framework.events.items.ItemUpdateLoreEvent;
import me.mykindos.betterpvp.core.framework.events.items.ItemUpdateNameEvent;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Singleton
public class ItemHandler {

    private final ItemRepository itemRepository;

    private final HashMap<String, BPVPItem> itemMap = new HashMap<>();

    @Inject
    @Config(path = "items.hideAttributes", defaultValue = "true")
    private boolean hideAttributes;

    @Inject
    @Config(path = "items.hideEnchants", defaultValue = "true")
    private boolean hideEnchants;

    @Inject
    public ItemHandler(Core core, ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    public void loadItemData(String module) {
        List<BPVPItem> items = itemRepository.getItemsForModule(module);
        items.forEach(item -> itemMap.put(item.getIdentifier(), item));
    }

    /**
     * General method that updates the name of almost every item that is picked up by players
     * E.g. Names leather armour after assassins
     * E.g. Turns the colour of the items name to yellow from white
     *
     * @param itemStack ItemStack to update
     * @return An ItemStack with an updated name
     */
    public ItemStack updateNames(ItemStack itemStack) {

        Material material = itemStack.getType();
        ItemMeta itemMeta = itemStack.getItemMeta();

        if (hideAttributes) {
            itemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            itemMeta.addItemFlags(ItemFlag.HIDE_ITEM_SPECIFICS);
        }

        if (hideEnchants) {
            itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        BPVPItem item = getItem(itemStack);
        if (item != null) {
            item.itemify(itemStack);

            var nameUpdateEvent = UtilServer.callEvent(new ItemUpdateNameEvent(itemStack, itemMeta, item.getName()));
            itemMeta.displayName(nameUpdateEvent.getItemName().decoration(TextDecoration.ITALIC, false));

            var loreUpdateEvent = UtilServer.callEvent(new ItemUpdateLoreEvent(itemStack, itemMeta, new ArrayList<>(item.getLore())));

            PersistentDataContainer dataContainer = itemMeta.getPersistentDataContainer();

            if (dataContainer.has(CoreNamespaceKeys.DURABILITY_KEY)) {
                item.applyLore(itemMeta, loreUpdateEvent.getItemLore(), dataContainer.getOrDefault(CoreNamespaceKeys.DURABILITY_KEY, PersistentDataType.INTEGER, item.getMaxDurability()));
            } else {
                item.applyLore(itemMeta, loreUpdateEvent.getItemLore());
            }

            if (item.isGlowing() || dataContainer.has(CoreNamespaceKeys.GLOW_KEY)) {
                UtilItem.addGlow(itemMeta);
            } else {
                for (Map.Entry<Enchantment, Integer> entry : itemStack.getEnchantments().entrySet()) {
                    itemStack.removeEnchantment(entry.getKey());
                }
            }
        } else {
            final PersistentDataContainer pdc = itemMeta.getPersistentDataContainer();
            if (!pdc.getOrDefault(CoreNamespaceKeys.IMMUTABLE_KEY, PersistentDataType.BOOLEAN, false)) {
                itemMeta.displayName(Component.text(UtilFormat.cleanString(material.name())).color(NamedTextColor.YELLOW));
            }
        }

        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    public Set<String> getItemIdentifiers() {
        return itemMap.keySet();
    }

    public BPVPItem getItem(String identifier) {
        return itemMap.get(identifier);
    }

    public BPVPItem getItem(ItemStack itemStack) {
        if(itemStack.getItemMeta() == null) return null;

        //try quick way
        PersistentDataContainer dataContainer = itemStack.getItemMeta().getPersistentDataContainer();
        if (dataContainer.has(CoreNamespaceKeys.CUSTOM_ITEM_KEY)) {
            return getItem(dataContainer.get(CoreNamespaceKeys.CUSTOM_ITEM_KEY, PersistentDataType.STRING));
        }
        //do expensive lookup
        for (BPVPItem item : itemMap.values()) {
            if (item.matches(itemStack)) return item;
        }

        return null;
    }

    public void replaceItem(String identifier, BPVPItem newItem) {
        itemMap.replace(identifier, newItem);
    }
}
