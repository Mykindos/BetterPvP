package me.mykindos.betterpvp.champions.weapons.impl.runes;

import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.utilities.ChampionsNamespacedKeys;
import me.mykindos.betterpvp.core.framework.CoreNamespaceKeys;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public abstract class SingleStatRune extends Rune {

    protected double minRoll;
    protected double maxRoll;

    protected SingleStatRune(Champions plugin, String key) {
        super(plugin, key);
    }

    protected double getRollFromMeta(ItemMeta meta) {
        return getRollFromMeta(meta, getNamespacedKey(), PersistentDataType.DOUBLE);
    }

    @Override
    public void onInitialize(ItemMeta meta) {

        if (!meta.getPersistentDataContainer().has(getNamespacedKey())) {
            double roll = UtilMath.randDouble(minRoll, maxRoll);
            meta.getPersistentDataContainer().set(getNamespacedKey(), PersistentDataType.DOUBLE, roll);
        }

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
    public void loadWeaponConfig() {
        minRoll = getConfig("minRoll", 0.0, Double.class);
        maxRoll = getConfig("maxRoll", 1.0, Double.class);
    }
}
