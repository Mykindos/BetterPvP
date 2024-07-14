package me.mykindos.betterpvp.champions.weapons.impl.runes.frost;

import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.weapons.impl.runes.Rune;
import me.mykindos.betterpvp.champions.weapons.impl.runes.RuneNamespacedKeys;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;


public abstract class FrostBase extends Rune {

    private double chanceMinRoll;
    private double chanceMaxRoll;

    private double durationMinRoll;
    private double durationMaxRoll;

    private int slownessAmplifier;

    protected FrostBase(Champions plugin, String key) {
        super(plugin, key);
    }

    @Override
    public void onInitialize(ItemMeta meta) {

        if (!meta.getPersistentDataContainer().has(RuneNamespacedKeys.FROST_CHANCE)) {
            double chanceRoll = UtilMath.randDouble(chanceMinRoll, chanceMaxRoll);
            meta.getPersistentDataContainer().set(RuneNamespacedKeys.FROST_CHANCE, PersistentDataType.DOUBLE, chanceRoll);
        }

        if (!meta.getPersistentDataContainer().has(RuneNamespacedKeys.FROST_DURATION)) {
            double durationRoll = UtilMath.randDouble(durationMinRoll, durationMaxRoll);
            meta.getPersistentDataContainer().set(RuneNamespacedKeys.FROST_DURATION, PersistentDataType.DOUBLE, durationRoll);
        }

        if(!meta.getPersistentDataContainer().has(RuneNamespacedKeys.FROST_AMPLIFIER)) {
            meta.getPersistentDataContainer().set(RuneNamespacedKeys.FROST_AMPLIFIER, PersistentDataType.INTEGER, slownessAmplifier);
        }

    }

    @Override
    public List<Component> getRuneLoreDescription(ItemMeta itemMeta) {
        double chanceRoll = getRollFromMeta(itemMeta, RuneNamespacedKeys.FROST_CHANCE, PersistentDataType.DOUBLE, 0d);
        double durationRoll = getRollFromMeta(itemMeta, RuneNamespacedKeys.FROST_DURATION, PersistentDataType.DOUBLE, 0d);

        return List.of(UtilMessage.deserialize("When hitting an enemy, you have a <green>%.1f%% <reset>chance", chanceRoll),
                UtilMessage.deserialize("to apply <green>Slowness %s<reset> for <green>%.1f<reset> seconds",
                        slownessAmplifier, durationRoll));
    }

    @Override
    public List<Component> getItemLoreDescription(PersistentDataContainer pdc, ItemStack itemStack) {
        int tier = pdc.getOrDefault(RuneNamespacedKeys.TIER, PersistentDataType.INTEGER, 0);
        double chanceRoll = pdc.getOrDefault(RuneNamespacedKeys.FROST_CHANCE, PersistentDataType.DOUBLE, 0.0);
        double durationRoll = pdc.getOrDefault(RuneNamespacedKeys.FROST_DURATION, PersistentDataType.DOUBLE, 0.0);
        return List.of(UtilMessage.deserialize("%s <gray>- <green>%.1f%% <reset>chance to apply <green>Slowness %s <reset>for <green>%.1f<reset> seconds",
                getStarPrefix(tier), chanceRoll, UtilFormat.getRomanNumeral(slownessAmplifier), durationRoll));
    }

    @Override
    public boolean canApplyToItem(ItemMeta runeMeta, ItemMeta itemMeta) {

        double chanceRoll = getRollFromMeta(runeMeta, RuneNamespacedKeys.FROST_CHANCE, PersistentDataType.DOUBLE, 0d);
        double durationRoll = getRollFromMeta(runeMeta, RuneNamespacedKeys.FROST_DURATION, PersistentDataType.DOUBLE, 0d);

        PersistentDataContainer pdc = itemMeta.getPersistentDataContainer().get(getAppliedNamespacedKey(), PersistentDataType.TAG_CONTAINER);
        if (pdc != null) {
            int existingTier = getRollFromItem(pdc, RuneNamespacedKeys.TIER, PersistentDataType.INTEGER);
            if (getTier() < existingTier) {
                return false;
            }

            double existingChance = getRollFromItem(pdc, RuneNamespacedKeys.FROST_CHANCE, PersistentDataType.DOUBLE);
            if (chanceRoll < existingChance) {
                return false;
            }

            double existingDuration = getRollFromItem(pdc, RuneNamespacedKeys.FROST_DURATION, PersistentDataType.DOUBLE);
            if (durationRoll < existingDuration) {
                return false;
            }
        }

        return true;
    }

    @Override
    public void applyToItem(ItemMeta runeMeta, ItemMeta itemMeta) {
        super.applyToItem(runeMeta, itemMeta);

        double frostChance = getRollFromMeta(runeMeta, RuneNamespacedKeys.FROST_CHANCE, PersistentDataType.DOUBLE, 0d);
        double frostDuration = getRollFromMeta(runeMeta, RuneNamespacedKeys.FROST_DURATION, PersistentDataType.DOUBLE, 0d);
        int slowness = getRollFromMeta(runeMeta, RuneNamespacedKeys.FROST_AMPLIFIER, PersistentDataType.INTEGER, 1);

        PersistentDataContainer pdc = itemMeta.getPersistentDataContainer();
        PersistentDataContainer newPdc = pdc.getAdapterContext().newPersistentDataContainer();

        newPdc.set(RuneNamespacedKeys.FROST_CHANCE, PersistentDataType.DOUBLE, frostChance);
        newPdc.set(RuneNamespacedKeys.FROST_DURATION, PersistentDataType.DOUBLE, frostDuration);
        newPdc.set(RuneNamespacedKeys.FROST_AMPLIFIER, PersistentDataType.INTEGER, slowness);

        newPdc.set(RuneNamespacedKeys.OWNING_RUNE, PersistentDataType.STRING, getNamespacedKey().toString());
        newPdc.set(RuneNamespacedKeys.TIER, PersistentDataType.INTEGER, getTier());

        pdc.set(getAppliedNamespacedKey(), PersistentDataType.TAG_CONTAINER, newPdc);


    }

    @Override
    public String[] getItemFilter() {
        return Rune.MELEE_WEAPON_FILTER;
    }

    @Override
    public String getCategory() {
        return "melee weapons";
    }

    @Override
    public NamespacedKey getAppliedNamespacedKey() {
        return RuneNamespacedKeys.FROST;
    }

    @Override
    public void loadWeaponConfig() {

        chanceMinRoll = getConfig("chanceMinRoll", 0.0, Double.class);
        chanceMaxRoll = getConfig("chanceMaxRoll", 0.0, Double.class);

        durationMinRoll = getConfig("durationMinRoll", 0.0, Double.class);
        durationMaxRoll = getConfig("durationMaxRoll", 0.0, Double.class);

        slownessAmplifier = getConfig("slownessAmplifier", 0, Integer.class);

    }


}
