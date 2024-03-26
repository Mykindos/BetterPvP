package me.mykindos.betterpvp.champions.weapons.impl.runes;

import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.combat.weapon.Weapon;
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
    protected static final String[] MELEE_WEAPON_FILTER = {"SWORD", "AXE"};

    protected Rune(Champions plugin, String key) {
        super(plugin, key);
    }

    public abstract String[] getItemFilter();

    public abstract String getCategory();

    public abstract Component getRuneLoreDescription(ItemMeta meta);
    public abstract Component getItemLoreDescription(PersistentDataContainer pdc);

    public abstract int getTier();

    /**
     * @return The namespaced key that is applied when the rune is added to an item
     */
    public abstract NamespacedKey getAppliedNamespacedKey();

    public abstract boolean canApplyToItem(ItemMeta runeMeta, ItemMeta itemMeta);

    public abstract void applyToItem(ItemMeta runeMeta, ItemMeta itemMeta);

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
    public List<Component> getLore(ItemStack item) {
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Can only be applied to " + getCategory(), NamedTextColor.DARK_GRAY));
        lore.add(Component.text(""));
        lore.add(getRuneLoreDescription(item.getItemMeta()));
        return lore;
    }

    @Override
    public String getConfigName() {
        return "weapons/runes";
    }

}
