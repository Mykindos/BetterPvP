package me.mykindos.betterpvp.core.items;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.framework.CoreNamespaceKeys;
import me.mykindos.betterpvp.core.framework.events.items.ItemUpdateLoreEvent;
import me.mykindos.betterpvp.core.framework.events.items.ItemUpdateNameEvent;
import me.mykindos.betterpvp.core.items.enchants.GlowEnchant;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.weapons.WeaponManager;
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

import java.lang.reflect.Field;
import java.util.*;

@Singleton
public class ItemHandler {

    private final WeaponManager weaponManager;
    private final ItemRepository itemRepository;
    private final HashMap<String, BPVPItem> itemMap = new HashMap<>();
    private final Enchantment glowEnchantment;


    @Inject
    @Config(path = "items.hideAttributes", defaultValue = "true")
    private boolean hideAttributes;

    @Inject
    @Config(path = "items.hideEnchants", defaultValue = "true")
    private boolean hideEnchants;

    @Inject
    public ItemHandler(Core core, WeaponManager weaponManager, ItemRepository itemRepository) {
        this.weaponManager = weaponManager;
        this.itemRepository = itemRepository;


        glowEnchantment = new GlowEnchant(CoreNamespaceKeys.GLOW_ENCHANTMENT_KEY);
        registerEnchantment(glowEnchantment);
    }

    public void loadItemData(String module) {
        List<BPVPItem> items = itemRepository.getItemsForModule(module);
        items.forEach(item -> itemMap.put(item.getMaterial().name() + item.getCustomModelData(), item));
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
        int modelData = itemMeta.hasCustomModelData() ? itemMeta.getCustomModelData() : 0;

        if (hideAttributes) {
            itemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            itemMeta.addItemFlags(ItemFlag.HIDE_ITEM_SPECIFICS);
        }

        if (hideEnchants) {
            itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        BPVPItem item = itemMap.get(material.name() + modelData);
        if (item != null) {
            var nameUpdateEvent = UtilServer.callEvent(new ItemUpdateNameEvent(itemStack, itemMeta, item.getName()));
            itemMeta.displayName(nameUpdateEvent.getItemName().decoration(TextDecoration.ITALIC, false));

            var loreUpdateEvent = UtilServer.callEvent(new ItemUpdateLoreEvent(itemStack, itemMeta, new ArrayList<>(item.getLore())));
            itemMeta.lore(UtilItem.removeItalic(loreUpdateEvent.getItemLore()));

            PersistentDataContainer dataContainer = itemMeta.getPersistentDataContainer();
            if(item.isGiveUUID()) {
                if (!dataContainer.has(CoreNamespaceKeys.UUID_KEY)) {
                    dataContainer.set(CoreNamespaceKeys.UUID_KEY, PersistentDataType.STRING, UUID.randomUUID().toString());
                }
            }

            if (item.isGlowing() || dataContainer.has(CoreNamespaceKeys.GLOW_KEY)) {
                itemMeta.addEnchant(glowEnchantment, 1, true);
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

    private void registerEnchantment(Enchantment enchantment) {
        try {
            Field accept = Enchantment.class.getDeclaredField("acceptingNew");
            accept.setAccessible(true);
            accept.set(null, true);
            Enchantment.registerEnchantment(enchantment);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
