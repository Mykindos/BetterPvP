package me.mykindos.betterpvp.core.items;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Consumable;
import io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.combat.weapon.types.LegendaryWeapon;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.framework.CoreNamespaceKeys;
import me.mykindos.betterpvp.core.framework.events.items.ItemUpdateNameEvent;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.items.menu.ItemButton;
import me.mykindos.betterpvp.core.items.type.IBPvPItem;
import me.mykindos.betterpvp.core.items.uuiditem.UUIDItem;
import me.mykindos.betterpvp.core.items.uuiditem.UUIDManager;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@CustomLog
@Singleton
public class ItemHandler {

    private final ItemRepository itemRepository;

    private final UUIDManager uuidManager;

    private final HashMap<String, BPvPItem> itemMap = new HashMap<>();

    @Inject
    @Config(path = "items.hideAttributes", defaultValue = "true")
    private boolean hideAttributes;

    @Inject
    @Config(path = "items.hideEnchants", defaultValue = "true")
    private boolean hideEnchants;

    @Inject
    public ItemHandler(Core core, ItemRepository itemRepository, UUIDManager uuidManager) {
        this.itemRepository = itemRepository;
        this.uuidManager = uuidManager;
    }

    public void loadItemData(String module) {
        List<BPvPItem> items = itemRepository.getItemsForModule(module);
        items.forEach(item -> itemMap.put(item.getIdentifier(), item));
    }

    public ItemStack updateNames(ItemStack itemStack) {
        return updateNames(itemStack, true);
    }

    /**
     * General method that updates the name of almost every item that is picked up by players
     * E.g. Names leather armour after assassins
     * E.g. Turns the colour of the items name to yellow from white
     *
     * @param itemStack ItemStack to update
     * @return An ItemStack with an updated name
     */
    public ItemStack updateNames(ItemStack itemStack, boolean giveUUID) {
        Material material = itemStack.getType();
        if (material == Material.AIR) {
            return itemStack;
        }

        if(UtilItem.isSword(itemStack)){
            itemStack.setData(DataComponentTypes.CONSUMABLE, Consumable.consumable().consumeSeconds(9999999f).animation(ItemUseAnimation.BLOCK).build());
        }

        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta == null) {
            itemMeta = Bukkit.getItemFactory().getItemMeta(material);
        }

        if (hideAttributes) {
            itemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            itemMeta.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        }

        if (hideEnchants) {
            itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }


        BPvPItem item = getItem(itemStack);
        if (item != null) {
            item.itemify(itemStack, itemMeta);

            PersistentDataContainer dataContainer = itemMeta.getPersistentDataContainer();

            if (item.isGiveUUID() && giveUUID) {
                if (!dataContainer.has(CoreNamespaceKeys.UUID_KEY)) {
                    UUID newUuid = UUID.randomUUID();
                    dataContainer.set(CoreNamespaceKeys.UUID_KEY, PersistentDataType.STRING, newUuid.toString());
                    uuidManager.addUuid(new UUIDItem(newUuid, item.getNamespace(), item.getKey()));
                }
            }

            var nameUpdateEvent = UtilServer.callEvent(new ItemUpdateNameEvent(itemStack, itemMeta, item.getName()));
            itemMeta.displayName(nameUpdateEvent.getItemName().decoration(TextDecoration.ITALIC, false));

            item.applyLore(itemStack, itemMeta);

            if (item.isGlowing() || dataContainer.has(CoreNamespaceKeys.GLOW_KEY)) {
                UtilItem.addGlow(itemMeta);
            } else {
                for (Map.Entry<Enchantment, Integer> entry : itemStack.getEnchantments().entrySet()) {
                    itemStack.removeEnchantment(entry.getKey());
                }
            }

        } else if (!itemMeta.hasDisplayName()) {
            final PersistentDataContainer pdc = itemMeta.getPersistentDataContainer();
            if (!pdc.getOrDefault(CoreNamespaceKeys.IMMUTABLE_KEY, PersistentDataType.BOOLEAN, false)) {
                itemMeta.displayName(Component.text(UtilFormat.cleanString(material.name())).color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
            }
        }

        itemMeta.setAttributeModifiers(itemStack.getType().getDefaultAttributeModifiers());
        if (itemMeta.hasCustomModelData() && itemMeta.getCustomModelData() == 0) {
            itemMeta.setCustomModelData(null);
        }
        itemStack.setItemMeta(itemMeta);

        return itemStack;
    }

    public Set<String> getItemIdentifiers() {
        return itemMap.keySet();
    }

    public BPvPItem getItem(String identifier) {
        return itemMap.get(identifier);
    }

    public BPvPItem getItem(ItemStack itemStack) {
        if(itemStack.getItemMeta() == null) return null;

        //try quick way
        PersistentDataContainer dataContainer = itemStack.getItemMeta().getPersistentDataContainer();
        if (dataContainer.has(CoreNamespaceKeys.CUSTOM_ITEM_KEY)) {
            return getItem(dataContainer.get(CoreNamespaceKeys.CUSTOM_ITEM_KEY, PersistentDataType.STRING));
        }

        //do expensive lookup
        for (BPvPItem item : itemMap.values()) {
            if (item.matches(itemStack)) return item;
        }

        return null;
    }

    public void replaceItem(String identifier, BPvPItem newItem) {
        itemMap.replace(identifier, newItem);
    }

    public List<UUIDItem> getUUIDItems(Player player) {
        List<UUIDItem> uuidItemList = new ArrayList<>();
        player.getInventory().forEach(itemStack -> {
            if (itemStack != null) {
                Optional<UUIDItem> uuidItemOptional = getUUIDItem(itemStack);
                uuidItemOptional.ifPresent(uuidItemList::add);
            }
        });
        return uuidItemList;
    }

    public Optional<UUIDItem> getUUIDItem(ItemStack itemStack) {
        if (itemStack != null) {
            ItemMeta itemMeta = itemStack.getItemMeta();
            if (itemMeta != null) {
                PersistentDataContainer pdc = itemMeta.getPersistentDataContainer();
                if (pdc.has(CoreNamespaceKeys.UUID_KEY)) {
                    return uuidManager.getObject(UUID.fromString(Objects.requireNonNull(pdc.get(CoreNamespaceKeys.UUID_KEY, PersistentDataType.STRING))));
                }
            }
        }
        return Optional.empty();
    }

    public List<BPvPItem> getLegends() {
        return getItems().stream().filter(LegendaryWeapon.class::isInstance).toList();
    }

    public Collection<BPvPItem> getItems() {
        return itemMap.values();
    }

    public List<ItemButton> getItemButtons() {
        return getItems().stream()
                .filter(IBPvPItem::isEnabled)
                .filter(bPvPItem -> !(bPvPItem.getCustomModelData() == 0 &&
                            UtilItem.isTool(bPvPItem.getItemStack())
                            && bPvPItem.getMaxDurability() <= 0))
                .sorted(Comparator.comparing(BPvPItem::getIdentifier))
                .sorted(Comparator.comparing(
                        bPvPItem -> !bPvPItem.getSimpleName().startsWith("Rune")
                ))
                .sorted(Comparator.comparing(LegendaryWeapon.class::isInstance).reversed())
                .map(bPvPItem -> {
                            ItemStack itemStack = updateNames(bPvPItem.getItemStack(), false);
                            List<Component> lore = itemStack.lore();
                            if (lore == null) {
                                lore = new ArrayList<>();
                            }
                            lore.addAll(bPvPItem.getDisplayLore());
                            itemStack.lore(lore);
                            ItemProvider itemProvider = ItemView.builder()
                                    .with(itemStack)
                                    .build();
                            return new ItemButton(bPvPItem, itemProvider);
        }       ).toList();
    }
}
