package me.mykindos.betterpvp.champions.weapons.impl.runes;

import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.combat.weapon.Weapon;
import me.mykindos.betterpvp.core.framework.CoreNamespaceKeys;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;


public abstract class Rune extends Weapon {

    protected static final String[] ARMOUR_FILTER = {"HELMET", "CHESTPLATE", "LEGGINGS", "BOOTS"};
    protected static final String[] MELEE_WEAPON_FILTER = {"SWORD", "_AXE"};
    protected static final String[] BOW_FILTER = {"BOW"};
    protected static final String[] TOOL_FILTER = {"PICKAXE", "AXE", "SHOVEL", "HOE", "SHEARS", "FISHING_ROD"};
    protected static final String[] PROFESSION_TOOL_FILTER = {"_AXE", "FISHING"};
    protected static final String[] ALL_FILTER = {"HELMET", "CHESTPLATE", "LEGGINGS", "BOOTS", "SWORD", "AXE", "BOW", "FISHING_ROD", "HOE", "SHEARS"};

    protected Rune(Champions plugin, String key) {
        super(plugin, key);
    }

    public abstract String[] getItemFilter();

    public abstract String getCategory();

    public abstract List<Component> getRuneLoreDescription(ItemMeta meta);

    public abstract List<Component> getItemLoreDescription(PersistentDataContainer pdc, ItemStack itemStack);

    public <T extends Number> T getRollFromItem(PersistentDataContainer pdc, NamespacedKey key, PersistentDataType<T, T> type) {
        return pdc.get(key, type);
    }

    protected <T extends Number> T getRollFromMeta(ItemMeta meta, NamespacedKey key, PersistentDataType<T, T> type, T defaultValue) {
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        T value = pdc.get(key, type);
        return value != null ? value : defaultValue;

    }

    public abstract int getTier();

    /**
     * @return The namespaced key that is applied when the rune is added to an item
     */
    public abstract NamespacedKey getAppliedNamespacedKey();

    public abstract boolean canApplyToItem(ItemMeta runeMeta, ItemMeta itemMeta);

    public void applyToItem(ItemMeta runeMeta, ItemMeta itemMeta) {
        PersistentDataContainer pdc = itemMeta.getPersistentDataContainer();
        pdc.set(CoreNamespaceKeys.GLOW_KEY, PersistentDataType.STRING, "true");
        pdc.set(RuneNamespacedKeys.HAS_RUNE, PersistentDataType.BOOLEAN, true);
    }

    public boolean itemMatchesFilter(Material material) {
        for (String filter : getItemFilter()) {
            if (material.name().contains(filter.toUpperCase())) {
                return true;
            }
        }
        return false;
    }

    protected boolean hasRune(ItemMeta meta) {
        return getRuneOfSameType(meta) != null;
    }

    protected NamespacedKey getRuneOfSameType(ItemMeta itemMeta) {
        PersistentDataContainer pdc = itemMeta.getPersistentDataContainer();
        PersistentDataContainer runePDC = pdc.get(getAppliedNamespacedKey(), PersistentDataType.TAG_CONTAINER);
        if (runePDC != null) {
            if (runePDC.has(RuneNamespacedKeys.OWNING_RUNE, PersistentDataType.STRING)) {
                String owningRune = runePDC.get(RuneNamespacedKeys.OWNING_RUNE, PersistentDataType.STRING);
                if (owningRune != null) {
                    return NamespacedKey.fromString(owningRune);

                }
            }
        }

        return null;
    }

    @Override
    public List<Component> getLore(ItemMeta meta) {
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Can only be applied to " + getCategory(), NamedTextColor.DARK_GRAY));
        lore.add(Component.text(""));
        lore.addAll(getRuneLoreDescription(meta));
        return lore;
    }

    protected String getStarPrefix(int tier) {
        String star = "\u2726";
        String colour = switch (tier) {
            case 1 -> "<green>";
            case 2 -> "<blue>";
            case 3 -> "<yellow>";
            case 4 -> "<orange>";
            default -> "<white>";
        };

        return colour + star;
    }

    @Override
    public String getConfigName() {
        return "weapons/runes";
    }

}
