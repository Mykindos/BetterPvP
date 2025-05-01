package me.mykindos.betterpvp.champions.weapons.impl.runes;

import lombok.Getter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public abstract class SingleStatRune extends Rune {

    @Getter
    protected double minRoll;
    @Getter
    protected double maxRoll;

    protected SingleStatRune(Champions plugin, String key) {
        super(plugin, key);
    }

    protected double getRollFromMeta(ItemMeta meta) {
        return getRollFromMeta(meta, getNamespacedKey(), PersistentDataType.DOUBLE, getMinRoll());
    }

    @Override
    public void onInitialize(ItemMeta meta) {

        if (!meta.getPersistentDataContainer().has(getNamespacedKey())) {
            double roll = UtilMath.randDouble(minRoll, maxRoll);
            meta.getPersistentDataContainer().set(getNamespacedKey(), PersistentDataType.DOUBLE, roll);
        }

        meta.setMaxStackSize(1);

    }

    @Override
    public boolean canApplyToItem(ItemMeta runeMeta, ItemMeta itemMeta) {
        double roll = getRollFromMeta(runeMeta);

        PersistentDataContainer existingPdc = itemMeta.getPersistentDataContainer().get(getAppliedNamespacedKey(), PersistentDataType.TAG_CONTAINER);
        if (existingPdc != null) {
            double existingRoll = existingPdc.getOrDefault(getAppliedNamespacedKey(), PersistentDataType.DOUBLE, 0D);
            return existingRoll < roll;
        }

        return true;
    }

    @Override
    public void applyToItem(ItemMeta runeMeta, ItemMeta itemMeta) {
        super.applyToItem(runeMeta, itemMeta);

        double roll = getRollFromMeta(runeMeta);

        PersistentDataContainer pdc = itemMeta.getPersistentDataContainer();
        PersistentDataContainer newPdc = pdc.getAdapterContext().newPersistentDataContainer();

        newPdc.set(getAppliedNamespacedKey(), PersistentDataType.DOUBLE, roll);
        newPdc.set(RuneNamespacedKeys.OWNING_RUNE, PersistentDataType.STRING, getNamespacedKey().toString());
        newPdc.set(RuneNamespacedKeys.TIER, PersistentDataType.INTEGER, getTier());

        pdc.set(getAppliedNamespacedKey(), PersistentDataType.TAG_CONTAINER, newPdc);


    }

    @Override
    public List<Component> getDisplayLore() {
        List<Component> lore = super.getDisplayLore();
        lore.addAll(List.of(
                Component.text(""),
                UtilMessage.deserialize("<white>Minimum - Maximum Values:</white>"),
                UtilMessage.deserialize("<white><green>%s</green> - <green>%s</green>", getMinRoll(), getMaxRoll())
        ));
        return lore;
    }

    @Override
    public void loadWeaponConfig() {
        minRoll = getConfig("minRoll", 0.0, Double.class);
        maxRoll = getConfig("maxRoll", 1.0, Double.class);
    }
}
