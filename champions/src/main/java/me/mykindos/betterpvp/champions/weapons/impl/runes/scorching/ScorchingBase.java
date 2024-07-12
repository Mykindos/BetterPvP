package me.mykindos.betterpvp.champions.weapons.impl.runes.scorching;

import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.weapons.impl.runes.Rune;
import me.mykindos.betterpvp.champions.weapons.impl.runes.RuneNamespacedKeys;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;


public abstract class ScorchingBase extends Rune {

    private double chanceMinRoll;
    private double chanceMaxRoll;

    private double durationMinRoll;
    private double durationMaxRoll;

    protected ScorchingBase(Champions plugin, String key) {
        super(plugin, key);
    }

    @Override
    public void onInitialize(ItemMeta meta) {

        if (!meta.getPersistentDataContainer().has(RuneNamespacedKeys.SCORCHING_CHANCE)) {
            double chanceRoll = UtilMath.randDouble(chanceMinRoll, chanceMaxRoll);
            meta.getPersistentDataContainer().set(RuneNamespacedKeys.SCORCHING_CHANCE, PersistentDataType.DOUBLE, chanceRoll);
        }

        if (!meta.getPersistentDataContainer().has(RuneNamespacedKeys.SCORCHING_DURATION)) {
            double durationRoll = UtilMath.randDouble(durationMinRoll, durationMaxRoll);
            meta.getPersistentDataContainer().set(RuneNamespacedKeys.SCORCHING_DURATION, PersistentDataType.DOUBLE, durationRoll);
        }

    }

    @Override
    public List<Component> getRuneLoreDescription(ItemMeta itemMeta) {
        double chanceRoll = getRollFromMeta(itemMeta, RuneNamespacedKeys.SCORCHING_CHANCE, PersistentDataType.DOUBLE, 0d);
        double durationRoll = getRollFromMeta(itemMeta, RuneNamespacedKeys.SCORCHING_DURATION, PersistentDataType.DOUBLE, 0d);

        return List.of(UtilMessage.deserialize("When hitting an enemy, you have a <green>%.1f%% <reset>chance", chanceRoll),
                UtilMessage.deserialize("to set them on fire for <green>%.1f<reset> seconds", durationRoll));
    }

    @Override
    public List<Component> getItemLoreDescription(PersistentDataContainer pdc, ItemStack itemStack) {
        int tier = pdc.getOrDefault(RuneNamespacedKeys.TIER, PersistentDataType.INTEGER, 0);
        double chanceRoll = pdc.getOrDefault(RuneNamespacedKeys.SCORCHING_CHANCE, PersistentDataType.DOUBLE, 0.0);
        double durationRoll = pdc.getOrDefault(RuneNamespacedKeys.SCORCHING_DURATION, PersistentDataType.DOUBLE, 0.0);
        return List.of(UtilMessage.deserialize("%s <gray>- <green>%.1f%% <reset>chance to ignite enemies for <green>%.1f<reset> seconds",
                getStarPrefix(tier), chanceRoll, durationRoll));
    }

    @Override
    public boolean canApplyToItem(ItemMeta runeMeta, ItemMeta itemMeta) {

        double chanceRoll = getRollFromMeta(runeMeta, RuneNamespacedKeys.SCORCHING_CHANCE, PersistentDataType.DOUBLE, 0d);
        double durationRoll = getRollFromMeta(runeMeta, RuneNamespacedKeys.SCORCHING_DURATION, PersistentDataType.DOUBLE, 0d);

        PersistentDataContainer pdc = itemMeta.getPersistentDataContainer().get(getAppliedNamespacedKey(), PersistentDataType.TAG_CONTAINER);
        if (pdc != null) {
            int existingTier = getRollFromItem(pdc, RuneNamespacedKeys.TIER, PersistentDataType.INTEGER);
            if (getTier() < existingTier) {
                return false;
            }

            double existingChance = getRollFromItem(pdc, RuneNamespacedKeys.SCORCHING_CHANCE, PersistentDataType.DOUBLE);
            if (chanceRoll < existingChance) {
                return false;
            }

            double existingDuration = getRollFromItem(pdc, RuneNamespacedKeys.SCORCHING_DURATION, PersistentDataType.DOUBLE);
            if (durationRoll < existingDuration) {
                return false;
            }
        }

        return true;
    }

    @Override
    public void applyToItem(ItemMeta runeMeta, ItemMeta itemMeta) {
        super.applyToItem(runeMeta, itemMeta);

        double chance = getRollFromMeta(runeMeta, RuneNamespacedKeys.SCORCHING_CHANCE, PersistentDataType.DOUBLE, 0d);
        double duration = getRollFromMeta(runeMeta, RuneNamespacedKeys.SCORCHING_DURATION, PersistentDataType.DOUBLE, 0d);

        PersistentDataContainer pdc = itemMeta.getPersistentDataContainer();
        PersistentDataContainer newPdc = pdc.getAdapterContext().newPersistentDataContainer();

        newPdc.set(RuneNamespacedKeys.SCORCHING_CHANCE, PersistentDataType.DOUBLE, chance);
        newPdc.set(RuneNamespacedKeys.SCORCHING_DURATION, PersistentDataType.DOUBLE, duration);

        newPdc.set(RuneNamespacedKeys.OWNING_RUNE, PersistentDataType.STRING, getNamespacedKey().toString());
        newPdc.set(RuneNamespacedKeys.TIER, PersistentDataType.INTEGER, getTier());

        pdc.set(getAppliedNamespacedKey(), PersistentDataType.TAG_CONTAINER, newPdc);


    }

    @Override
    public String[] getItemFilter() {
        return Rune.BOW_FILTER;
    }

    @Override
    public String getCategory() {
        return "bows / crossbows";
    }

    @Override
    public NamespacedKey getAppliedNamespacedKey() {
        return RuneNamespacedKeys.SCORCHING;
    }

    @Override
    public void loadWeaponConfig() {

        chanceMinRoll = getConfig("chanceMinRoll", 0.0, Double.class);
        chanceMaxRoll = getConfig("chanceMaxRoll", 0.0, Double.class);

        durationMinRoll = getConfig("durationMinRoll", 0.0, Double.class);
        durationMaxRoll = getConfig("durationMaxRoll", 0.0, Double.class);

    }


}
